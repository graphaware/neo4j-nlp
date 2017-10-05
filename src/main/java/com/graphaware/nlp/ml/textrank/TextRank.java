/*
 * Copyright (c) 2013-2017 GraphAware
 *
 * This file is part of the GraphAware Framework.
 *
 * GraphAware Framework is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package com.graphaware.nlp.ml.textrank;

import com.graphaware.common.util.Pair;
import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.domain.Keyword;
import com.graphaware.nlp.persistence.constants.Labels;
import com.graphaware.nlp.persistence.persisters.KeywordPersister;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.graphaware.nlp.persistence.constants.Relationships.DESCRIBES;
import java.util.concurrent.atomic.AtomicReference;

public class TextRank {

    private static final Logger LOG = LoggerFactory.getLogger(TextRank.class);
    // a query for creating co-occurrences per sentence
    // query based on orignal TextRank: it doesn't care about sentence boundaries (it connects last word of a sentence with 1st word of the next sentence)
    private static final String COOCCURRENCE_QUERY
            = "MATCH (a:AnnotatedText)-[:CONTAINS_SENTENCE]->(s:Sentence)-[:SENTENCE_TAG_OCCURRENCE]->(to:TagOccurrence)\n"
            + "WHERE id(a) = {id} \n"
            + "WITH to\n"
            + "ORDER BY to.startPosition\n"
            + "MATCH (to)-[:TAG_OCCURRENCE_TAG]->(t:Tag)\n"
            + "WHERE size(t.value) > 2\n"
            + "WITH collect(t) as tags, collect(to) as tagsPosition\n"
            + "UNWIND range(0, size(tags) - 2, 1) as i\n"
            + "RETURN id(tags[i]) as tag1, id(tags[i+1]) as tag2, tags[i].value as tag1_val, tags[i+1].value as tag2_val, "
            + "tagsPosition[i].startPosition as sourceStartPosition, "
            + "tagsPosition[i+1].startPosition as destinationStartPosition, tags[i].pos as pos1, tags[i+1].pos as pos2";

    private static final String COOCCURRENCE_QUERY_BY_SENTENCE
            = "MATCH (a:AnnotatedText)-[:CONTAINS_SENTENCE]->(s:Sentence)-[:SENTENCE_TAG_OCCURRENCE]->(to:TagOccurrence)\n"
            + "WHERE id(a) = {id} \n"
            + "WITH s, to\n"
            + "ORDER BY s.sentenceNumber, to.startPosition\n"
            + "MATCH (to)-[:TAG_OCCURRENCE_TAG]->(t:Tag)\n"
            + "WHERE size(t.value) > 2\n"
            + "WITH s, collect(t) as tags, collect(to) as tagsPosition\n"
            + "ORDER BY s.sentenceNumber\n"
            + "UNWIND range(0, size(tags) - 2, 1) as i\n"
            + "RETURN s, id(tags[i]) as tag1, id(tags[i+1]) as tag2, tags[i].value as tag1_val, tags[i+1].value as tag2_val, "
            + "tagsPosition[i].startPosition as sourceStartPosition, "
            + "tagsPosition[i+1].startPosition as destinationStartPosition, tags[i].pos as pos1, tags[i+1].pos as pos2";

    private static final String GET_TAG_QUERY = "MATCH (node:Tag)<-[:TAG_OCCURRENCE_TAG]-(to:TagOccurrence)<-[:SENTENCE_TAG_OCCURRENCE]-(:Sentence)<-[:CONTAINS_SENTENCE]-(a:AnnotatedText)\n"
            //+ "WHERE id(a) = {id} and id(node) IN {nodeList}\n"
            + "WHERE id(a) = {id}" // new
            + "OPTIONAL MATCH (to)<-[:COMPOUND|AMOD]-(to2:TagOccurrence)-[:TAG_OCCURRENCE_TAG]->(t2:Tag)\n"
            + "WHERE not exists(t2.pos) or any(p in t2.pos where p in {posList})\n"
            + "RETURN node.id as tag, to.startPosition as sP, to.endPosition as eP, id(node) as tagId, "
            + "collect(t2.value) as rel_tags, collect(to2.startPosition) as rel_tos,  collect(to2.endPosition) as rel_toe\n"
            + "ORDER BY sP asc";

    private final GraphDatabaseService database;
    private final boolean removeStopWords;
    private final boolean directionsMatter;
    private final boolean respectSentences;
    private final boolean useTfIdfWeights;
    private final boolean useDependencies;
    private final boolean cleanSingleWordKeyword;
    private final int cooccurrenceWindow;
    private final int maxSingles;
    private final double phrasesTopxWords;
    private final double singlesTopxWords;
    private final Label keywordLabel;
    private final Set<String> stopWords;
    private final List<String> admittedPOSs;
    private Map<Long, List<Long>> neExpanded;
    private final Map<Long, String> idToValue = new HashMap<>();

    public TextRank(GraphDatabaseService database, 
            boolean removeStopWords, 
            boolean directionsMatter, 
            boolean respectSentences, 
            boolean useTfIdfWeights, 
            boolean useDependencies, 
            boolean cleanSingleWordKeyword, 
            int cooccurrenceWindow, 
            int maxSingles, 
            double phrases_topx_words, 
            double singles_topx_words, 
            Label keywordLabel, 
            Set<String> stopWords, 
            List<String> admittedPOSs) {
        this.database = database;
        this.removeStopWords = removeStopWords;
        this.directionsMatter = directionsMatter;
        this.respectSentences = respectSentences;
        this.useTfIdfWeights = useTfIdfWeights;
        this.useDependencies = useDependencies;
        this.cleanSingleWordKeyword = cleanSingleWordKeyword;
        this.cooccurrenceWindow = cooccurrenceWindow;
        this.maxSingles = maxSingles;
        this.phrasesTopxWords = phrases_topx_words;
        this.singlesTopxWords = singles_topx_words;
        this.keywordLabel = keywordLabel;
        this.stopWords = stopWords;
        this.admittedPOSs = admittedPOSs;
    }

    @Deprecated
    public Map<Long, Map<Long, CoOccurrenceItem>> createCooccurrencesOld(Node annotatedText) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", annotatedText.getId());
        String query;
        if (respectSentences) {
            query = COOCCURRENCE_QUERY_BY_SENTENCE;
        } else {
            query = COOCCURRENCE_QUERY;
        }

        Result res = null;
        try (Transaction tx = database.beginTx();) {
            res = database.execute(query, params);
            tx.success();
        } catch (Exception e) {
            LOG.error("Error while creating co-occurrences: ", e);
        }

        Map<Long, Map<Long, CoOccurrenceItem>> results = new HashMap<>();
        Long previous1 = -1L;
        int previous1Start = -1;
        int nSkips = 1;
        while (res != null && res.hasNext()) {
            Map<String, Object> next = res.next();
            Long tag1 = toLong(next.get("tag1"));
            Long tag2 = toLong(next.get("tag2"));
            int tag1Start = (toLong(next.get("sourceStartPosition"))).intValue();
            int tag2Start = (toLong(next.get("destinationStartPosition"))).intValue();
            List<String> pos1 = Arrays.asList((String[]) next.get("pos1"));
            List<String> pos2 = Arrays.asList((String[]) next.get("pos2"));

            // check whether POS of both tags are admitted
            boolean bPOS1 = pos1.stream().filter(pos -> admittedPOSs.contains(pos)).count() != 0  ||  pos1.size() == 0;
            boolean bPOS2 = pos2.stream().filter(pos -> admittedPOSs.contains(pos)).count() != 0  ||  pos2.size() == 0;

            // fill tag co-occurrences (adjacency matrix)
            //   * window of words N = 2 (i.e. neighbours only => both neighbours must pass cleaning requirements)
            if (bPOS1 && bPOS2) {
                addTagToCoOccurrence(results, tag1, tag1Start, tag2, tag2Start);
                if (!directionsMatter) { // when direction of co-occurrence relationships is not important
                    addTagToCoOccurrence(results, tag2, tag2Start, tag1, tag1Start);
                }
                //LOG.info("Adding co-occurrence: " + (String) next.get("tag1_val") + " -> " + (String) next.get("tag2_val"));
                nSkips = 1;
            } //   * window of words N > 2
            else if (bPOS2) { // after possibly skipping some words, we arrived to a tag2 that complies with cleaning requirements
                if (nSkips < cooccurrenceWindow) {
                    addTagToCoOccurrence(results, previous1, previous1Start, tag2, tag2Start);
                    if (!directionsMatter) {
                        addTagToCoOccurrence(results, tag2, tag2Start, previous1, previous1Start);
                    }
                    //LOG.info("  window N=" + (n_skips+1) + " co-occurrence: " + idToValue.get(previous1) + " -> " + (String) next.get("tag2_val"));
                }
                nSkips = 1;
            } else { // skip to another word
                nSkips++;
                if (bPOS1) {
                    previous1 = tag1;
                    previous1Start = tag1Start;
                }
            }

            // for logging purposses
            idToValue.put(tag1, (String) next.get("tag1_val"));
            idToValue.put(tag2, (String) next.get("tag2_val"));
        }
        return results;
    }

    public Map<Long, Map<Long, CoOccurrenceItem>> createCooccurrences(Node annotatedText) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", annotatedText.getId());
        String query;
        if (respectSentences) {
            query = COOCCURRENCE_QUERY_BY_SENTENCE;
        } else {
            query = COOCCURRENCE_QUERY;
        }

        Result res = null;
        try (Transaction tx = database.beginTx();) {
            res = database.execute(query, params);
            tx.success();
        } catch (Exception e) {
            LOG.error("Error while creating co-occurrences: ", e);
        }

        List<CoOccurrenceItem> prelim = new ArrayList<>();
        while (res != null && res.hasNext()) {
            Map<String, Object> next = res.next();
            Long tag1 = toLong(next.get("tag1"));
            Long tag2 = toLong(next.get("tag2"));
            String tagVal1 = (String) next.get("tag1_val");
            String tagVal2 = (String) next.get("tag2_val");
            int tag1Start = (toLong(next.get("sourceStartPosition"))).intValue();
            int tag2Start = (toLong(next.get("destinationStartPosition"))).intValue();
            List<String> pos1 = Arrays.asList((String[]) next.get("pos1"));
            List<String> pos2 = Arrays.asList((String[]) next.get("pos2"));

            // check whether POS of both tags are admitted
            boolean bPOS1 = pos1.stream().filter(pos -> admittedPOSs.contains(pos)).count() != 0  ||  pos1.size() == 0;
            boolean bPOS2 = pos2.stream().filter(pos -> admittedPOSs.contains(pos)).count() != 0  ||  pos2.size() == 0;

            // fill tag co-occurrences (adjacency matrix)
            if (bPOS1 && bPOS2) {
                prelim.add(new CoOccurrenceItem(tag1, tag1Start, tag2, tag2Start));
            }

            // for logging purposses and for `handleNamedEntities()`
            idToValue.put(tag1, tagVal1);
            idToValue.put(tag2, tagVal2);
        }

        Map<Long, List<Pair<Long, Long>>> neExp = expandNamedEntities();
        neExpanded = neExp.entrySet().stream()
                        .collect(Collectors.toMap( Map.Entry::getKey, e -> e.getValue().stream().map(p -> p.second()).collect(Collectors.toList()) ));
 
        Map<Long, Map<Long, CoOccurrenceItem>> results = new HashMap<>();
        long neVisited = 0L;
        for (CoOccurrenceItem it: prelim) {
            Long tag1 = it.getSource();
            Long tag2 = it.getDestination();
            int tag1Start = it.getSourceStartingPositions().get(0).first().intValue();
            int tag2Start = it.getSourceStartingPositions().get(0).second().intValue();

            if (neExp.containsKey(tag1)) {
                if (neVisited == 0L || neVisited != tag1.longValue()) {
                    connectTagsInNE(results, neExp.get(tag1), tag1Start);
                    neVisited = 0L;
                }
                tag1Start += neExp.get(tag1).get( neExp.get(tag1).size() - 1 ).first().intValue();
                tag1 = neExp.get(tag1).get( neExp.get(tag1).size() - 1 ).second();
            }

            if (neExp.containsKey(tag2)) {
                connectTagsInNE(results, neExp.get(tag2), tag2Start);
                neVisited = tag2;
                tag2 = neExp.get(tag2).get(0).second();
            } else
                neVisited = 0L;

            addTagToCoOccurrence(results, tag1, tag1Start, tag2, tag2Start);
            if (!directionsMatter) { // when direction of co-occurrence relationships is not important
                addTagToCoOccurrence(results, tag2, tag2Start, tag1, tag1Start);
            }
        }

        return results;
    }

    private static Long toLong(Object value) {
        Long returnValue;
        if (value instanceof Integer) {
            returnValue = ((Integer) value).longValue();
        } else if (value instanceof Long) {
            returnValue = ((Long) value);
        } else if (value instanceof String) {
            returnValue = Long.parseLong((String) value);
        } else {
            throw new RuntimeException("Value: " + value + " cannot be cast to Long");
        }
        return returnValue;
    }

    private void addTagToCoOccurrence(Map<Long, Map<Long, CoOccurrenceItem>> results, Long source, int sourceStartPosition, Long destination, int destinationStartPosition) {
        Map<Long, CoOccurrenceItem> mapTag1;
        if (!results.containsKey(source)) {
            mapTag1 = new HashMap<>();
            results.put(source, mapTag1);
        } else {
            mapTag1 = results.get(source);
        }

        if (mapTag1.containsKey(destination)) {
            CoOccurrenceItem ccEntry = mapTag1.get(destination);
            ccEntry.incCount();
            ccEntry.addPositions(sourceStartPosition, destinationStartPosition);
        } else {
            mapTag1.put(destination, new CoOccurrenceItem(source, sourceStartPosition, destination, destinationStartPosition));
        }
    }

    private void connectTagsInNE(Map<Long, Map<Long, CoOccurrenceItem>> results, List<Pair<Long, Long>> tags, int startOffset) {
        int n = tags.size();
        for (int i=0; i<n-2; i++) {
            for (int j=i+1; j<n; j++) {
                addTagToCoOccurrence(results, tags.get(i).second(), startOffset + tags.get(i).first().intValue(), tags.get(j).second(), startOffset + tags.get(j).first().intValue());
                if (!directionsMatter) { // when direction of co-occurrence relationships is not important
                    addTagToCoOccurrence(results, tags.get(j).second(), startOffset + tags.get(j).first().intValue(), tags.get(i).second(), startOffset + tags.get(i).first().intValue());
                }
            }
        }
    }

    private  Map<Long, List<Pair<Long, Long>>> expandNamedEntities() {
        //TextProcessor processor = getNLPManager().getTextProcessorsManager().getTextProcessor();
        //AnnotatedText annotatedText = processor.annotateText(text, pipelineSpecification.getName(), "en", null);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "tokenizer");
        parameters.put("textProcessor", "com.graphaware.nlp.processor.stanford.StanfordTextProcessor");

        Map<String, Object> p = new HashMap<>();
        p.put("params", parameters);

        Map<Long, List<Pair<Long, Long>>> result = new HashMap<>();
        Map<Long, String> newIdsToVal = new HashMap<>();

        long nextNewId = -2L;
        for (Long valueL: idToValue.keySet()) {
                if (idToValue.get(valueL).split(" ").length < 2)
                    continue;
                String str = idToValue.get(valueL).toLowerCase();
                p.put("text", str);
                List<Pair<Long, Long>> res = new ArrayList<>();
                try (Transaction tx = database.beginTx()) {
                    Result r = database.execute(
                        "WITH ga.nlp.processor.annotate({text}, {params}) AS annotated\n"
                        + "with keys(annotated.sentences[0].tagOccurrences) as keys, annotated\n"
                        + "unwind keys as k\n"
                        + "with toInteger(k) as kInt, annotated\n"
                        + "order by kInt asc\n"
                        + "return kInt as start, annotated.sentences[0].tagOccurrences[toString(kInt)][0].element.lemma as lemma"
                        , p);
                    while (r.hasNext()) {
                        Map<String, Object> next = r.next();
                        Long start = (Long) next.get("start");
                        String val = (String) next.get("lemma");
                        List<Long> lId = idToValue.entrySet().stream().filter(en -> en.getValue().equals(val) || en.getValue().toLowerCase().equals(val)).map(Map.Entry::getKey).collect(Collectors.toList());
                        List<Long> lIdNew = newIdsToVal.entrySet().stream().filter(en -> en.getValue().equals(val) || en.getValue().toLowerCase().equals(val)).map(Map.Entry::getKey).collect(Collectors.toList());
                        if (lId!=null && lId.size()>0) {
                            res.add(new Pair<>(start, lId.get(0)));
                        } else if (lIdNew!=null && lIdNew.size()>0) {
                            res.add(new Pair<>(start, lIdNew.get(0)));
                        } else {
                            res.add(new Pair<>(start, nextNewId));
                            newIdsToVal.put(nextNewId, val);
                            nextNewId -= 1L;
                        }
                    }
                    r.close();
                    tx.success();
                }
                if (res.size() > 0)
                    result.put(valueL, res);
        }

        // finish by adding newly assigned IDs to idToValue map
        for (Long key: newIdsToVal.keySet())
             idToValue.put(key, newIdsToVal.get(key));

        return result;
    }

    public boolean evaluate(Node annotatedText, int iter, double damp, double threshold) {
        Map<Long, Map<Long, CoOccurrenceItem>> coOccurrence = createCooccurrences(annotatedText);
        PageRank pageRank = new PageRank(database);
        if (useTfIdfWeights) {
            pageRank.setNodeWeights(initializeNodeWeights_TfIdf(annotatedText, coOccurrence));
        }
        Map<Long, Double> pageRanks = pageRank.run(coOccurrence, iter, damp, threshold);

        int n_oneThird = (int) (pageRanks.size() * phrasesTopxWords);
        List<Long> topThird = getTopX(pageRanks, n_oneThird);

        LOG.info("Top " + n_oneThird + " tags: " + topThird.stream().map(id -> idToValue.get(id)).collect(Collectors.joining(", ")));
        Map<String, Object> params = new HashMap<>();
        params.put("id", annotatedText.getId());
        //params.put("nodeList", topThird); // new (also changed the GET_TAG_QUERY)
        params.put("posList", admittedPOSs);
        List<KeywordExtractedItem> keywordsOccurrences = new ArrayList<>();
        Map<Long, KeywordExtractedItem> keywordMap = new HashMap<>();
        try (Transaction tx = database.beginTx()) {
            Result res = database.execute(GET_TAG_QUERY, params);
            while (res != null && res.hasNext()) {
                Map<String, Object> next = res.next();
                long tagId = (long) next.get("tagId");
                KeywordExtractedItem item = new KeywordExtractedItem(tagId);
                item.setStartPosition(((Number) next.get("sP")).intValue());
                item.setValue(((String) next.get("tag")));
                item.setEndPosition(((Number) next.get("eP")).intValue());
                item.setRelatedTags(iterableToList((Iterable<String>) next.get("rel_tags")));
                item.setRelTagStartingPoints(iterableToList((Iterable<Number>) next.get("rel_tos")));
                item.setRelTagEndingPoints(iterableToList((Iterable<Number>) next.get("rel_toe")));
                item.setRelevance(pageRanks.containsKey(tagId) ? pageRanks.get(tagId) : 0);
                keywordsOccurrences.add(item);
                if (!keywordMap.containsKey(tagId)) {
                    keywordMap.put(tagId, item);
                }
            }
            if (res != null) {
                res.close();
            }
            tx.success();
        } catch (Exception e) {
            LOG.error("Error while running TextRank evaluation: ", e);
            return false;
        }

        Map<String, Long> valToId = idToValue.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

        Map<String, Keyword> results = new HashMap<>();

        while (!keywordsOccurrences.isEmpty()) {
            final AtomicReference<KeywordExtractedItem> keywordOccurrence
                    = new AtomicReference<>(keywordsOccurrences.remove(0));
            final AtomicReference<String> currValue = new AtomicReference<>(keywordOccurrence.get().getValue());
            final AtomicReference<Double> currRelevance = new AtomicReference<>(keywordOccurrence.get().getRelevance());
            List<Long> relTagIDs = keywordOccurrence.get().getRelatedTags().stream().map(el -> valToId.get(el)).collect(Collectors.toList()); // new
            relTagIDs.retainAll(topThird); // new
            if (!topThird.contains(keywordOccurrence.get().getTagId()) && relTagIDs.size()==0) // new
                continue;
            //System.out.println("\n> " + currValue.get() + " - " + keywordOccurrence.get().getStartPosition());
            Map<String, Keyword> localResults;
            do {
                long tagId = keywordOccurrence.get().getTagId();
                //System.out.println("  cur: " + currValue.get() + ". Examining next level");
                localResults = checkNextKeyword(tagId, keywordOccurrence.get(), coOccurrence, keywordMap);
                if (localResults.size() > 0) {
                    //System.out.println("    related tags: " + localResults.entrySet().stream().map(en -> en.getKey()).collect(Collectors.joining(", ")));
                    localResults.entrySet().stream().forEach((item) -> {
                        KeywordExtractedItem nextKeyword = keywordsOccurrences.get(0);
                        if (nextKeyword != null && nextKeyword.value.equalsIgnoreCase(item.getKey())) {
                            String newCurrValue = currValue.get().split("_")[0] + " " + item.getKey();
                            //System.out.println(">> " + newCurrValue);
                            double newCurrRelevance = currRelevance.get() + item.getValue().getRelevance();
                            currValue.set(newCurrValue);
                            currRelevance.set(newCurrRelevance);
                            keywordOccurrence.set(nextKeyword);
                            keywordsOccurrences.remove(0);
                        } else {
                            LOG.warn("Next keyword not found!");
                            keywordOccurrence.set(null);
                        }
                    });
                }
            } while (!localResults.isEmpty() && keywordOccurrence.get() != null);
            addToResults(currValue.get(), currRelevance.get(), results, 1);
            //System.out.println("< " + currValue.get());
        }

        // add named entities that contain at least some of the top 1/3 of words
        for (Long key: neExpanded.keySet()) {
            if (neExpanded.get(key).stream().filter(v -> topThird.contains(v)).count() == 0)
                continue;
            String keystr = idToValue.get(key) + "_en"; // + lang;
            addToResults(keystr, pageRanks.containsKey(key) ? pageRanks.get(key) : 0, results, 1);
        }

        computeTotalOccurrence(results);
        if (cleanSingleWordKeyword) {
            results = cleanSingleWordKeyword(results);
        }
        peristKeyword(results, annotatedText);

        return true;
    }

    private Map<String, Keyword> checkNextKeyword(long tagId, KeywordExtractedItem keywordOccurrence, Map<Long, Map<Long, CoOccurrenceItem>> coOccurrences, Map<Long, KeywordExtractedItem> keywords) {
        Map<String, Keyword> results = new HashMap<>();
        if (!coOccurrences.containsKey(tagId))
            return results;

        Map<Integer, Set<Long>> mapStartId = createThisMapping(coOccurrences.get(tagId), tagId);
        Set<Long> coOccurrence = mapStartId.get(keywordOccurrence.startPosition);
        if (coOccurrence == null) {
            return results;
        }

        Iterator<Long> iterator = coOccurrence.stream()
                .filter((ccEntry) -> ccEntry != tagId)
                .filter((ccEntry) -> keywords.containsKey(ccEntry)).iterator();

        while (iterator.hasNext()) {
            Long ccEntry = iterator.next();
            String relValue = keywords.get(ccEntry).getValue();
            //System.out.println("checkNextKeyword >> " + relValue);
            //if (!useDependencies || keywordOccurrence.getRelatedTags().contains(relValue.split("_")[0])) {
            List<String> merged = new ArrayList<>(keywords.get(tagId).getRelatedTags()); // new
            merged.retainAll(keywordOccurrence.getRelatedTags()); // new
            //System.out.println("    related tag = " + idToValue.get(ccEntry) + ", related tags = " + keywords.get(tagId).getRelatedTags().stream().collect(Collectors.joining(", ")));
            //System.out.println("      merged = " + merged.stream().collect(Collectors.joining(", ")));
            if (!useDependencies || keywordOccurrence.getRelatedTags().contains(relValue.split("_")[0]) || merged.size()>0) { // new
                //System.out.println("checkNextKeyword >>> " + relValue);
                addToResults(relValue,
                        keywords.get(ccEntry).getRelevance(),
                        results, 1);
            }
        }

        return results;
    }

    private void addToResults(String res, double relevance, Map<String, Keyword> results, int occurrences) {
        //System.out.println("addToResults: " + res + " " + relevance + " " + occurrences);
        if (res != null) {
            if (results.containsKey(res)) {
                results.get(res).incCountsBy(occurrences);
                //System.out.println("+inc");
            } else {
                final Keyword keyword = new Keyword(res, occurrences);
                keyword.setRelevance(relevance);
                results.put(res, keyword);
            }
        }
    }

    private void peristKeyword(Map<String, Keyword> results, Node annotatedText) {
        LOG.info("--- Results: ");
        KeywordPersister persister = NLPManager.getInstance().getPersister(Keyword.class);
        results.entrySet().stream()
                .forEach(en -> {
                    // check keyword consistency
                    if (en.getKey().split("_").length > 2) {
                        LOG.warn("Tag " + en.getKey() + " has more than 1 underscore symbols, newly created " + keywordLabel.name() + " node might be wrong");
                    }
                    Node newNode = persister.persist(en.getValue(), en.getKey(), String.valueOf(System.currentTimeMillis()));
                    if (newNode != null) {
                        //LOG.info("New node has labels: " + iterableToList(newNode.getLabels()).stream().map(l -> l.name()).collect(Collectors.joining(", ")));
                        Relationship rel = mergeRelationship(annotatedText, newNode);
                        rel.setProperty("count_exactMatch", en.getValue().getExactMatchCount());
                        rel.setProperty("count", en.getValue().getTotalCount());
                        rel.setProperty("relevance", en.getValue().getRelevance());
                    }
                    LOG.info(en.getKey().split("_")[0]);
                });
    }

    public boolean postprocess() {
        // if a keyphrase in current document contains a keyphrase from any other document, create also DESCRIBES relationship to that other keyphrase
        String query = "match (k:" + keywordLabel.name() + ")\n"
                + "where k.numTerms > 1\n"
                + "with k, k.keywordsList as ks_orig\n"
                + "match (k2:" + keywordLabel.name() + ")\n"
                + "where k2.numTerms > k.numTerms and not exists( (k)-[:DESCRIBES]->(:AnnotatedText)<-[:DESCRIBES]-(k2) )\n"
                + "with ks_orig, k, k2, k2.keywordsList as ks_check\n"
                + "where all(el in ks_orig where el in ks_check)\n"
                + "match (k2)-[r2:DESCRIBES]->(a:AnnotatedText)\n"
                + "MERGE (k)-[rNew:DESCRIBES]->(a)\n"
                + "ON CREATE SET rNew.count = r2.count_exactMatch, rNew.count_exactMatch = 0\n"
                + "ON MATCH SET  rNew.count = rNew.count + r2.count_exactMatch";

        try (Transaction tx = database.beginTx();) {
            LOG.info("Running identification of sub-keyphrases ...");
            database.execute(query);
            tx.success();
        } catch (Exception e) {
            LOG.error("Error while running TextRank post-processing (identification of sub-keyphrases): ", e);
            return false;
        }

        // add HAS_SUBGROUP relationships between keywords, ex.: (station) -[HAS_SUBGROUP]-> (space station) -[HAS_SUBGROUP]-> (international space station)
        query = "match (k:" + keywordLabel.name() + ")\n"
                + "with k, k.keywordsList as ks_orig\n"
                + "match (k2:" + keywordLabel.name() + ")\n"
                + "where k2.numTerms > k.numTerms\n"
                + "with ks_orig, k, k2, k2.keywordsList as ks_check\n"
                + "where all(el in ks_orig where el in ks_check)\n"
                + "MERGE (k)-[r:HAS_SUBGROUP]->(k2)";

        try (Transaction tx = database.beginTx();) {
            LOG.info("Discovering HAS_SUBGROUP relationships between keywords and keyphrases ...");
            database.execute(query);
            tx.success();
        } catch (Exception e) {
            LOG.error("Error while running TextRank post-processing (discovering HAS_SUBGROUP relationships): ", e);
            return false;
        }

        return true;
    }


    private List<Long> getTopX(Map<Long, Double> pageRanks, int x) {
        List<Long> topx = pageRanks.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(x)
                .map((item) -> item.getKey()).collect(Collectors.toList());
        return topx;
    }

    private Map<Long, Double> initializeNodeWeights_TfIdf(Node annotatedText, Map<Long, Map<Long, CoOccurrenceItem>> coOccurrences) {
        Map<Long, Double> nodeWeights = new HashMap<>();
        coOccurrences.entrySet().stream().forEach((coOccurrence) -> {
            coOccurrence.getValue().entrySet().stream().forEach((entry) -> {
                nodeWeights.put(entry.getValue().getSource(), 1.0d);
                nodeWeights.put(entry.getValue().getDestination(), 1.0d);
            });
        });

        String query = "MATCH (doc:AnnotatedText)\n"
                + "WITH count(doc) as documentsCount\n"
                + "MATCH (a:AnnotatedText)-[:CONTAINS_SENTENCE]->(:Sentence)-[ht:HAS_TAG]->(t:Tag)\n"
                + "WHERE id(a) = {id} \n"
                + "WITH t, sum(ht.tf) as tf, documentsCount\n"
                + "MATCH (a:AnnotatedText)-[:CONTAINS_SENTENCE]->(:Sentence)-[:HAS_TAG]->(t)\n"
                + "RETURN id(t) as tag, t.value as tagVal, tf, count(distinct a) as docCountForTag, documentsCount\n";

        try (Transaction tx = database.beginTx();) {
            Result res = database.execute(query, Collections.singletonMap("id", annotatedText.getId()));
            while (res != null && res.hasNext()) {
                Map<String, Object> next = res.next();
                Long tag = (Long) next.get("tag");
                if (!nodeWeights.keySet().contains(tag)) // initialize only those that are needed!
                {
                    continue;
                }
                long tf = ((Long) next.get("tf"));

                long docCount = (long) next.get("documentsCount");
                long docCountTag = (long) next.get("docCountForTag");
                double idf = Math.log10(1.0d * docCount / docCountTag);

                //nodeWeights.put(tag, tf*idf);
                nodeWeights.put(tag, idf);
                //LOG.info((String) next.get("tagVal") + ": tf = " + tf + ", idf = " + idf + " (docCountTag = " + docCountTag + "), tf*idf = " + tf*idf);
            }
            tx.success();
        } catch (Exception e) {
            LOG.error("Error while initializing node weights: ", e);
            return nodeWeights;
        }

        return nodeWeights;
    }

    private <T> List<T> iterableToList(Iterable<T> it) {
        List<T> newList = new ArrayList<>();
        for (T obj : it) {
            newList.add(obj);
        }
        return newList;
    }
    

    private Relationship mergeRelationship(Node annotatedText, Node newNode) {
        Relationship rel = null;
        Iterable<Relationship> itr = newNode.getRelationships(Direction.OUTGOING, DESCRIBES);
        for (Relationship r : itr) {
            if (r.getEndNode().equals(annotatedText)) {
                rel = r;
                break;
            }
        }
        if (rel == null) {
            rel = newNode.createRelationshipTo(annotatedText, DESCRIBES);
        }
        return rel;
    }

    private Map<Integer, Set<Long>> createThisMapping(Map<Long, CoOccurrenceItem> coOccorrence, long tagId) {
        Map<Integer, Set<Long>> result = new HashMap<>();
        coOccorrence.entrySet().stream().forEach((entry) -> {
            if (entry.getValue().getSource() == tagId) {
                entry.getValue().getSourceStartingPositions()
                        .forEach((pairStartingPoint) -> {
                            if (pairStartingPoint.first() < pairStartingPoint.second()) {
                                if (!result.containsKey(pairStartingPoint.first())) {
                                    result.put(pairStartingPoint.first(), new TreeSet());
                                }
                                result.get(pairStartingPoint.first()).add(entry.getValue().getDestination());
                            }
                        });
            }
        });
        return result;
    }

    private void computeTotalOccurrence(Map<String, Keyword> results) {
        results.entrySet().stream().forEach((entry) -> {
            results.entrySet().stream().forEach((innerEntry) -> {
                if (entry.getValue().getWordsCount() < innerEntry.getValue().getWordsCount()
                        && innerEntry.getValue().getRawKeyword().contains(entry.getValue().getRawKeyword())) {
                    entry.getValue().incTotalCountBy(innerEntry.getValue().getTotalCount());
                }
            });
        });
    }
    
    private Map<String, Keyword>  cleanSingleWordKeyword(Map<String, Keyword> results) {
        Map<String, Keyword> newResults = new HashMap<>(results);
        results.entrySet().stream().forEach((entry) -> {
            results.entrySet().stream().forEach((innerEntry) -> {
                if (entry.getValue().getWordsCount() < innerEntry.getValue().getWordsCount()
                        && innerEntry.getValue().getRawKeyword().contains(entry.getValue().getRawKeyword())
                        && entry.getValue().getWordsCount() == 1 // new
                    ) {
                    newResults.remove(entry.getKey());
                }
            });
        });
        return newResults;
    }

    public static class Builder {

        private static final String[] STOP_WORDS = {"new", "old", "large", "big", "vast", "small", "many", "few", "best", "worst"};
        private static final String[] ADMITTED_POS = {"NN", "NNS", "NNP", "NNPS", "JJ", "JJR", "JJS"};

        private static final boolean DEFAULT_REMOVE_STOP_WORDS = false;

        private static final boolean DEFAULT_DIRECTION_MATTER = false;
        private static final boolean DEFAULT_RESPECT_SENTENCES = true;
        private static final boolean DEFAULT_USE_TF_IDF_WEIGHT = false;
        private static final boolean DEFAULT_USE_TYPED_DEPENDENCIES = true;
        private static final boolean DEFAULT_CLEAN_SINGLE_WORD_KEYWORDS = true;
        private static final int DEFAULT_CO_OCCURRENCE_WINDOW = 2;
        private static final int DEFAULT_MAX_SINGLES = 15;

        private final GraphDatabaseService database;
        private boolean removeStopWords = DEFAULT_REMOVE_STOP_WORDS;
        private boolean directionsMatter = DEFAULT_DIRECTION_MATTER;
        private boolean respectSentences = DEFAULT_RESPECT_SENTENCES;
        private boolean useTfIdfWeights = DEFAULT_USE_TF_IDF_WEIGHT;
        private boolean useDependencies = DEFAULT_USE_TYPED_DEPENDENCIES;
        private boolean cleanSingleWordKeyword = DEFAULT_CLEAN_SINGLE_WORD_KEYWORDS;
        private int cooccurrenceWindow = DEFAULT_CO_OCCURRENCE_WINDOW;
        private int maxSingles = DEFAULT_MAX_SINGLES;
        private double phrasesTopxWords;
        private double singlesTopxWords;
        private Label keywordLabel;
        private Set<String> stopWords = new HashSet<>(Arrays.asList(STOP_WORDS));
        private List<String> admittedPOSs = Arrays.asList(ADMITTED_POS);

        ;

        public Builder(GraphDatabaseService database, DynamicConfiguration configuration) {
            this.database = database;
            this.keywordLabel = configuration.getLabelFor(Labels.Keyword);
        }

        public TextRank build() {
            TextRank result = new TextRank(database,
                    removeStopWords,
                    directionsMatter,
                    respectSentences,
                    useTfIdfWeights,
                    useDependencies,
                    cleanSingleWordKeyword,
                    cooccurrenceWindow,
                    maxSingles,
                    phrasesTopxWords,
                    singlesTopxWords,
                    keywordLabel,
                    stopWords,
                    admittedPOSs);
            return result;
        }

        public Builder setStopwords(String stopwords) {
            if (stopwords.split(",").length > 0 && stopwords.split(",")[0].equals("+")) { // if the stopwords list starts with "+,....", append the list to the default 'stopWords' set
                this.stopWords.addAll(Arrays.asList(stopwords.split(",")).stream().filter(str -> !str.equals("+")).map(str -> str.trim().toLowerCase()).collect(Collectors.toSet()));
            } else {
                this.stopWords = Arrays.asList(stopwords.split(",")).stream().map(str -> str.trim().toLowerCase()).collect(Collectors.toSet());
            }
            this.removeStopWords = true;
            return this;
        }

        public Builder removeStopWords(boolean val) {
            this.removeStopWords = val;
            return this;
        }

        public Builder respectDirections(boolean val) {
            this.directionsMatter = val;
            return this;
        }

        public Builder respectSentences(boolean val) {
            this.respectSentences = val;
            return this;
        }

        public Builder useTfIdfWeights(boolean val) {
            this.useTfIdfWeights = val;
            return this;
        }

        public Builder useDependencies(boolean val) {
            this.useDependencies = val;
            return this;
        }

        public Builder setCooccurrenceWindow(int val) {
            this.cooccurrenceWindow = val;
            return this;
        }

        public Builder setMaxSingleKeywords(int val) {
            this.maxSingles = val;
            return this;
        }

        public Builder setAdmittedPOSs(List<String> admittedPOSs) {
            this.admittedPOSs = admittedPOSs;
            return this;
        }

        public Builder setTopXWordsForPhrases(double topXWordsForPhrases) {
            this.phrasesTopxWords = topXWordsForPhrases;
            return this;
        }

        public Builder setTopXSinglewordKeywords(double topXSinglewordKeywords) {
            this.singlesTopxWords = topXSinglewordKeywords;
            return this;
        }

        public Builder setKeywordLabel(String keywordLabel) {
            //this.keywordLabel = Labels.valueOf(keywordLabel); // doesn't work because Labels is enum, while we want customizable keyword labels
            this.keywordLabel = Label.label(keywordLabel);
            return this;
        }
        
        public Builder setCleanSingleWordKeywords(boolean cleanSingleWordKeyword) {
            this.cleanSingleWordKeyword = cleanSingleWordKeyword;
            return this;
        }
    }

    private class KeywordExtractedItem {

        private final long tagId;
        private int startPosition;
        private int endPosition;
        private String value;
        private double relevance;
        private List<String> relatedTags;
        private List<Number> relTagStartingPoints;
        private List<Number> relTagEndingPoints;

        public KeywordExtractedItem(long tagId) {
            this.tagId = tagId;
        }

        public long getTagId() {
            return tagId;
        }

        public int getStartPosition() {
            return startPosition;
        }

        public void setStartPosition(int startPosition) {
            this.startPosition = startPosition;
        }

        public int getEndPosition() {
            return endPosition;
        }

        public void setEndPosition(int endPosition) {
            this.endPosition = endPosition;
        }

        public List<String> getRelatedTags() {
            return relatedTags;
        }

        public void setRelatedTags(List<String> relatedTags) {
            this.relatedTags = relatedTags;
        }

        public List<Number> getRelTagStartingPoints() {
            return relTagStartingPoints;
        }

        public void setRelTagStartingPoints(List<Number> relTagStartingPoints) {
            this.relTagStartingPoints = relTagStartingPoints;
        }

        public List<Number> getRelTagEndingPoints() {
            return relTagEndingPoints;
        }

        public void setRelTagEndingPoints(List<Number> relTagEndingPoints) {
            this.relTagEndingPoints = relTagEndingPoints;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public double getRelevance() {
            return relevance;
        }

        public void setRelevance(double relevance) {
            this.relevance = relevance;
        }
    }
}
