/*
 * Copyright (c) 2013-2018 GraphAware
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
import com.graphaware.nlp.domain.TfIdfObject;
import com.graphaware.nlp.persistence.constants.Labels;
import com.graphaware.nlp.persistence.persisters.KeywordPersister;
import com.graphaware.nlp.dsl.request.PipelineSpecification;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import com.graphaware.common.log.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.graphaware.nlp.persistence.constants.Relationships.DESCRIBES;
import java.util.concurrent.atomic.AtomicReference;

public class TextRank {

    private static final Log LOG = LoggerFactory.getLogger(TextRank.class);

    // a query for creating co-occurrences per sentence
    // query based on orignal TextRank: it doesn't care about sentence boundaries (it connects last word of a sentence with 1st word of the next sentence)
    private static final String COOCCURRENCE_QUERY
            = "MATCH (a:AnnotatedText)-[:CONTAINS_SENTENCE]->(s:Sentence)-[:SENTENCE_TAG_OCCURRENCE]->(to:TagOccurrence)\n"
            + "WHERE id(a) = {id}\n"
            + "WITH to\n"
            + "ORDER BY to.startPosition\n"
            + "MATCH (to)-[:TAG_OCCURRENCE_TAG]->(t:Tag)\n"
            //+ "WHERE size(t.value) > 2\n"
            + "WHERE size(t.value) > 2 AND NOT(toLower(t.value) IN {stopwords}) AND NOT ANY(pos IN t.pos WHERE t.pos IN {forbiddenPOSs}) AND NOT ANY(l IN labels(t) WHERE l IN {forbiddenNEs})\n"
            + "WITH collect(t) as tags, collect(to) as tagsPosition\n"
            + "UNWIND range(0, size(tags) - 2, 1) as i\n"
            + "RETURN id(tags[i]) as tag1, id(tags[i+1]) as tag2, tags[i].id as tag1_id, tags[i+1].id as tag2_id, "
            + "tagsPosition[i].startPosition as sourceStartPosition, "
            + "tagsPosition[i+1].startPosition as destinationStartPosition, tags[i].pos as pos1, tags[i+1].pos as pos2";

    private static final String COOCCURRENCE_QUERY_BY_SENTENCE
            = "MATCH (a:AnnotatedText)-[:CONTAINS_SENTENCE]->(s:Sentence)-[:SENTENCE_TAG_OCCURRENCE]->(to:TagOccurrence)\n"
            + "WHERE id(a) = {id} \n"
            + "WITH s, to\n"
            + "ORDER BY s.sentenceNumber, to.startPosition\n"
            + "MATCH (to)-[:TAG_OCCURRENCE_TAG]->(t:Tag)\n"
            //+ "WHERE size(t.value) > 2\n"
            + "WHERE size(t.value) > 2 AND NOT(toLower(t.value) IN {stopwords}) AND NOT ANY(pos IN t.pos WHERE t.pos IN {forbiddenPOSs}) AND NOT ANY(l IN labels(t) WHERE l IN {forbiddenNEs})\n"
            + "WITH s, collect(t) as tags, collect(to) as tagsPosition\n"
            + "ORDER BY s.sentenceNumber\n"
            + "UNWIND range(0, size(tags) - 2, 1) as i\n"
            + "RETURN s, id(tags[i]) as tag1, id(tags[i+1]) as tag2, tags[i].id as tag1_id, tags[i+1].id as tag2_id, "
            + "tagsPosition[i].startPosition as sourceStartPosition, "
            + "tagsPosition[i+1].startPosition as destinationStartPosition, tags[i].pos as pos1, tags[i+1].pos as pos2";

    private static final String COOCCURRENCE_QUERY_FROM_DEPENDENCIES
            = "MATCH (a:AnnotatedText)-[:CONTAINS_SENTENCE]->(s:Sentence)-[:SENTENCE_TAG_OCCURRENCE]->(to:TagOccurrence)//-[r]->(to2:TagOccurrence)\n"
            + "WHERE id(a) = {id}\n"
            + "WITH to\n"
            //+ "MATCH (to)-[r]-(to2:TagOccurrence)\n"
            + "OPTIONAL MATCH (to)-[r]-(to2:TagOccurrence)\n"
            + "WHERE to <> to2 AND to.startPosition < to2.startPosition\n"
            + "WITH to, to2, r\n"
            + "MATCH (to)-[:TAG_OCCURRENCE_TAG]->(t:Tag)\n"
            //+ "WHERE size(t.value) > 2 //and (size(t.pos)=0 OR any(p in t.pos where p in {}))\n"
            + "WHERE size(t.value) > 2 AND NOT(toLower(t.value) IN {stopwords}) AND NOT ANY(pos IN t.pos WHERE t.pos IN {forbiddenPOSs}) AND NOT ANY(l IN labels(t) WHERE l IN {forbiddenNEs})\n"
            //+ "MATCH (to2)-[:TAG_OCCURRENCE_TAG]->(t2:Tag)\n"
            + "OPTIONAL MATCH (to2)-[:TAG_OCCURRENCE_TAG]->(t2:Tag)\n"
            //+ "WHERE size(t2.value) > 2 //and (size(t2.pos)=0 OR any(p in t2.pos where p in {}))\n"
            + "WHERE size(t2.value) > 2 AND NOT(toLower(t2.value) IN {stopwords}) AND NOT ANY(pos IN t2.pos WHERE t2.pos IN {forbiddenPOSs}) AND NOT ANY(l IN labels(t2) WHERE l IN {forbiddenNEs})\n"
            + "RETURN id(t) as tag1, id(t2) as tag2, t.id as tag1_id, t2.id as tag2_id, to.startPosition as sourceStartPosition, to2.startPosition as destinationStartPosition, t.pos as pos1, t2.pos as pos2, collect(type(r))\n"
            + "ORDER BY sourceStartPosition, destinationStartPosition";

    private static final String GET_TAG_QUERY = "MATCH (node:Tag)<-[:TAG_OCCURRENCE_TAG]-(to:TagOccurrence)<-[:SENTENCE_TAG_OCCURRENCE]-(:Sentence)<-[:CONTAINS_SENTENCE]-(a:AnnotatedText)\n"
            //+ "WHERE id(a) = {id} and id(node) IN {nodeList}\n"
            + "WHERE id(a) = {id}  and not (toLower(node.value) IN {stopwords})" // new
            + "OPTIONAL MATCH (to)<-[:COMPOUND|AMOD]-(to2:TagOccurrence)-[:TAG_OCCURRENCE_TAG]->(t2:Tag)\n"
            + "WHERE not exists(t2.pos) or size(t2.pos) = 0 or any(p in t2.pos where p in {posList}) and not (toLower(t2.value) IN {stopwords})\n"
            + "RETURN node.id as tag, to.startPosition as sP, to.endPosition as eP, id(node) as tagId, "
            + "collect(id(t2)) as rel_tags, collect(to2.startPosition) as rel_tos,  collect(to2.endPosition) as rel_toe, labels(node) as labels\n"
            + "ORDER BY sP asc";

    private static final String PIPELINE_WITHOUT_NER = "tokenizerNoNEs";

    private final GraphDatabaseService database;
    private final boolean removeStopWords;
    private final boolean directionsMatter;
    private final boolean respectSentences;
    private final boolean useDependencies;
    private final boolean cooccurrencesFromDependencies;
    private final boolean cleanKeywords;
    private final boolean expandNEs;
    private final int cooccurrenceWindow;
    private final double topxTags;
    private final Label keywordLabel;
    private final Set<String> stopWords;
    private final List<String> admittedPOSs;
    private final List<String> forbiddenNEs;
    private final List<String> forbiddenPOSs;
    private Map<Long, List<Long>> neExpanded;
    private final Map<Long, String> idToValue = new HashMap<>();
    private double relevanceAvg;
    private double relevanceSigma;
    private double tfidfAvg;
    private double tfidfSigma;

    public TextRank(GraphDatabaseService database, 
            boolean removeStopWords, 
            boolean directionsMatter, 
            boolean respectSentences, 
            boolean useDependencies, 
            boolean cooccurrencesFromDependencies,
            boolean cleanKeywords,
            int cooccurrenceWindow, 
            double topxTags, 
            Label keywordLabel, 
            Set<String> stopWords, 
            List<String> admittedPOSs,
            List<String> forbiddenNEs,
            List<String> forbiddenPOSs) {
        this.database = database;
        this.removeStopWords = removeStopWords;
        this.directionsMatter = directionsMatter;
        this.respectSentences = respectSentences;
        //this.useTfIdfWeights = useTfIdfWeights;
        this.useDependencies = useDependencies;
        this.cooccurrencesFromDependencies = cooccurrencesFromDependencies;
        this.cleanKeywords = cleanKeywords;
        this.expandNEs = true; // not useful making this user-customizable
        this.cooccurrenceWindow = cooccurrenceWindow;
        this.topxTags = topxTags;
        this.keywordLabel = keywordLabel;
        this.stopWords = stopWords;
        this.admittedPOSs = admittedPOSs;
        this.forbiddenNEs = forbiddenNEs;
        this.forbiddenPOSs = forbiddenPOSs;

        initializePipelineWithoutNEs();
    }

    private void initializePipelineWithoutNEs() {
        //System.out.println(" >>> default processor: " + NLPManager.getInstance().getTextProcessorsManager().getDefaultProcessor().getAlias());
        Map<String, Object> params = new HashMap<>();
        params.put("tokenize", true);
        params.put("ner", false);
        PipelineSpecification ps = new PipelineSpecification(PIPELINE_WITHOUT_NER, null);
        ps.setProcessingSteps(params);
        NLPManager.getInstance().getTextProcessorsManager().getDefaultProcessor()
            .createPipeline(ps);
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

    public Map<Long, Map<Long, CoOccurrenceItem>> createCooccurrences(Node annotatedText, boolean fromDependencies) {
        String query;
        if (fromDependencies)
            query = COOCCURRENCE_QUERY_FROM_DEPENDENCIES;
        else if (respectSentences) {
            query = COOCCURRENCE_QUERY_BY_SENTENCE;
        } else {
            query = COOCCURRENCE_QUERY;
        }

        // use tf*idf for creating a list of stopwords
        //Map<Long, TfIdfObject> tfidfMap = initializeNodeWeights_TfIdf(annotatedText, null);
        //List<Long> currStopwords = tfidfMap.entrySet().stream()
        //    .filter(en -> en.getValue().getTfIdf() < 0.5)
        //    .map(en -> en.getKey()).collect(Collectors.toList());//.keySet();

        Map<String, Object> params = new HashMap<>();
        params.put("id", annotatedText.getId());
        params.put("stopwords", stopWords);
        params.put("forbiddenPOSs", forbiddenPOSs);
        params.put("forbiddenNEs", forbiddenNEs);
        //params.put("forbiddenNEs", new ArrayList<String>());
        //params.put("forbiddenNEs", Arrays.asList("NER_Number", "NER_Ordinal", "NER_Percent", "NER_Duration"));

        if (fromDependencies) {
            params.put("stopwords", new ArrayList<>());
            params.put("forbiddenPOSs", new ArrayList<>());
            params.put("forbiddenNEs", new ArrayList<>());
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
            String tagVal1 = (String) next.get("tag1_id");
            String tagVal2 = (String) next.get("tag2_id");
            Long tag1Start = toLong(next.get("sourceStartPosition"));
            Long tag2Start = toLong(next.get("destinationStartPosition"));
            List<String> pos1 = next.get("pos1") != null ? Arrays.asList((String[]) next.get("pos1")) : new ArrayList<>();
            List<String> pos2 = next.get("pos2") != null ? Arrays.asList((String[]) next.get("pos2")) : new ArrayList<>();

            // check whether POS of both tags are admitted
            boolean bPOS1 = pos1.stream().filter(pos -> admittedPOSs.contains(pos)).count() != 0  ||  pos1.size() == 0;
            boolean bPOS2 = pos2.stream().filter(pos -> admittedPOSs.contains(pos)).count() != 0  ||  pos2.size() == 0;
            //System.out.println("  " + tagVal1 + " -> " + tagVal2);

            // fill tag co-occurrences (adjacency matrix)
            if (bPOS1 && bPOS2 && tagVal1 != null && tagVal2 != null) {
                //System.out.println("    passed");
                prelim.add(new CoOccurrenceItem(tag1, tag1Start.intValue(), tag2, tag2Start.intValue()));
            }

            // for logging purposses and for `expandNamedEntities()`
            if (tag1!=null)
                idToValue.put(tag1, tagVal1);
            if (tag2!=null)
                idToValue.put(tag2, tagVal2);
        }

        Map<Long, List<Pair<Long, Long>>> neExp;
        if (expandNEs && !fromDependencies) {
            // process named entities: split them into individual tokens by calling ga.nlp.annotate(), assign them IDs and create co-occurrences
            neExp = expandNamedEntities();
            neExpanded = neExp.entrySet().stream()
                            .collect(Collectors.toMap( Map.Entry::getKey, e -> e.getValue().stream().map(p -> p.second()).collect(Collectors.toList()) ));
        } else
            neExp = new HashMap<>();

        //System.out.println(" >> Creating co-occurrence graph:");
        //String gr = "";
        //Long prev = -1L; 
        Map<Long, Map<Long, CoOccurrenceItem>> results = new HashMap<>();
        long neVisited = 0L;
        for (CoOccurrenceItem it: prelim) {
            Long tag1 = it.getSource();
            Long tag2 = it.getDestination();
            int tag1Start = it.getStartingPositions().get(0).first().intValue();
            int tag2Start = it.getStartingPositions().get(0).second().intValue();

            if (expandNEs && !fromDependencies) {
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
            }

            //System.out.println("  Creating co-occurrence: " + idToValue.get(tag1) + " -> " + idToValue.get(tag2));
            addTagToCoOccurrence(results, tag1, tag1Start, tag2, tag2Start);
            if (!directionsMatter) { // when direction of co-occurrence relationships is not important
                addTagToCoOccurrence(results, tag2, tag2Start, tag1, tag1Start);
            }

            // print co-occurrence sequences (graph)
            /*if (prev.equals("")) {
                gr = idToValue.get(tag1) + " -> " + idToValue.get(tag2);
            } else if (tag1.equals(prev)) {
                gr += " -> " + idToValue.get(tag2);
            } else {
                System.out.println("   " + gr);
                gr = idToValue.get(tag1) + " -> " + idToValue.get(tag2);
            }
            prev = tag2;*/
        }

        return results;
    }

    private static Long toLong(Object value) {
        Long returnValue;
        if (value == null) {
            return null;
        }else if (value instanceof Integer) {
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

    /*private void addTagToCoOccurrenceNew(Map<Long, Map<Long, CoOccurrenceItem>> results, CoOccurrenceItem cooccurrence) {
        Map<Long, CoOccurrenceItem> mapTag1;
        if (!results.containsKey(cooccurrence.getSource())) {
            mapTag1 = new HashMap<>();
            results.put(cooccurrence.getSource(), mapTag1);
        } else {
            mapTag1 = results.get(cooccurrence.getSource());
        }

        if (mapTag1.containsKey(cooccurrence.getDestination())) {
            CoOccurrenceItem ccEntry = mapTag1.get(cooccurrence.getDestination());
            ccEntry.incCount();
            ccEntry.addPositions(cooccurrence.getStartingPositions().first(), cooccurrence.getStartingPositions().second());
        } else {
            mapTag1.put(destination, cooccurrence);
        }
    }*/

    private void connectTagsInNE(Map<Long, Map<Long, CoOccurrenceItem>> results, List<Pair<Long, Long>> tags, int startOffset) {
        int n = tags.size();
        for (int i=0; i<n-1; i++) {
            for (int j=i+1; j<n; j++) {
                addTagToCoOccurrence(results, tags.get(i).second(), startOffset + tags.get(i).first().intValue(), tags.get(j).second(), startOffset + tags.get(j).first().intValue());
                if (!directionsMatter) { // when direction of co-occurrence relationships is not important
                    addTagToCoOccurrence(results, tags.get(j).second(), startOffset + tags.get(j).first().intValue(), tags.get(i).second(), startOffset + tags.get(i).first().intValue());
                }
            }
        }
    }

    private  Map<Long, List<Pair<Long, Long>>> expandNamedEntities() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", PIPELINE_WITHOUT_NER);

        Map<String, Object> p = new HashMap<>();
        p.put("params", parameters);

        Map<Long, List<Pair<Long, Long>>> result = new HashMap<>();
        Map<Long, String> newIdsToVal = new HashMap<>();

        long nextNewId = -2L;
        for (Long valueL: idToValue.keySet()) {
                if (idToValue.get(valueL).trim().split(" ").length < 2)
                    continue;
                String str = idToValue.get(valueL).toLowerCase().split("_")[0].trim();
                p.put("text", str);
                List<Pair<Long, Long>> res = new ArrayList<>();
                try (Transaction tx = database.beginTx()) {
                    Result r = database.execute(
                        "WITH ga.nlp.processor.annotate({text}, {params}) AS annotated\n"
                        + "with keys(annotated.sentences[0].tagOccurrences) as keys, annotated\n"
                        + "unwind keys as k\n"
                        + "with toInteger(k) as kInt, annotated\n"
                        + "order by kInt asc\n"
                        + "return kInt as start, annotated.sentences[0].tagOccurrences[toString(kInt)][0].element.id as tagVal"
                        , p);
                    while (r.hasNext()) {
                        Map<String, Object> next = r.next();
                        Long start = (Long) next.get("start");
                        String val = (String) next.get("tagVal");
                        List<Long> lId = idToValue.entrySet().stream().filter(en -> en.getValue().equals(val) || en.getValue().equalsIgnoreCase(val)).map(Map.Entry::getKey).collect(Collectors.toList());
                        List<Long> lIdNew = newIdsToVal.entrySet().stream().filter(en -> en.getValue().equals(val) || en.getValue().equalsIgnoreCase(val)).map(Map.Entry::getKey).collect(Collectors.toList());
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
                    result.put(valueL, res); // map: id(NE) -> ListOfIndividualTags(Pair(startPostion, tagId))
        }

        // finish by adding newly assigned IDs to idToValue map
        for (Long key: newIdsToVal.keySet())
             idToValue.put(key, newIdsToVal.get(key));

        return result;
    }

    public boolean evaluate(Node annotatedText, int iter, double damp, double threshold) {
        Map<Long, Map<Long, CoOccurrenceItem>> coOccurrence = createCooccurrences(annotatedText, cooccurrencesFromDependencies);
        PageRank pageRank = new PageRank(database);
        //if (useTfIdfWeights) {
        //    pageRank.setNodeWeights(initializeNodeWeights_TfIdf(annotatedText, coOccurrence));
        //}
        Map<Long, Double> pageRanks = pageRank.run(coOccurrence, iter, damp, threshold);

        if (cooccurrencesFromDependencies) {
            coOccurrence.clear();
            coOccurrence = createCooccurrences(annotatedText, false); // co-occurrences from natural word flow; needed for merging keywords into key phrases
        }

        if (pageRanks == null) {
            LOG.error("Page ranks not retrieved, aborting evaluate() method ...");
            return false;
        }

        // get tf*idf: useful for cleanFinalKeywords()
        final Map<Long, TfIdfObject> tfidfMap = new HashMap<>();
        if (useDependencies)
            initializeNodeWeights_TfIdf(tfidfMap, annotatedText, null);

        // for z-scores: calculate mean and sigma of relevances and tf*idf
        relevanceAvg = pageRanks.entrySet().stream().mapToDouble(e -> e.getValue()).average().orElse(0.);
        relevanceSigma = Math.sqrt(pageRanks.entrySet().stream().mapToDouble(e -> Math.pow((e.getValue() - relevanceAvg), 2)).average().orElse(0.));
        tfidfAvg = tfidfMap.entrySet().stream().mapToDouble(e -> e.getValue().getTfIdf()).average().orElse(0.);
        tfidfSigma = Math.sqrt(tfidfMap.entrySet().stream().mapToDouble(e -> Math.pow(e.getValue().getTfIdf() - tfidfAvg, 2)).average().orElse(0.));

        int n_oneThird = (int) (pageRanks.size() * topxTags);
        List<Long> topThird = getTopX(pageRanks, n_oneThird);

        pageRanks.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).forEach(en -> System.out.println("   " + idToValue.get(en.getKey()) + ": " + en.getValue()));
        
        Map<String, Object> params = new HashMap<>();
        params.put("id", annotatedText.getId());
        params.put("posList", admittedPOSs);
        params.put("stopwords", removeStopWords ? stopWords : new ArrayList<>());

        List<KeywordExtractedItem> keywordsOccurrences = new ArrayList<>();
        Map<Long, KeywordExtractedItem> keywordMap = new HashMap<>();
        List<Long> wrongNEs = new ArrayList<>();
        try (Transaction tx = database.beginTx()) {
            Result res = database.execute(GET_TAG_QUERY, params);
            while (res != null && res.hasNext()) {
                Map<String, Object> next = res.next();
                long tagId = (long) next.get("tagId");

                // remove stop-NEs
                if (iterableToList((Iterable<String>) next.get("labels")).stream().anyMatch(el -> forbiddenNEs.contains(el))) {
                    wrongNEs.add(tagId);
                    continue;
                }

                KeywordExtractedItem item = new KeywordExtractedItem(tagId);
                item.setStartPosition(((Number) next.get("sP")).intValue());
                item.setValue(((String) next.get("tag")));
                item.setEndPosition(((Number) next.get("eP")).intValue());
                item.setRelatedTags(iterableToList((Iterable<Long>) next.get("rel_tags")));
                item.setRelTagStartingPoints(iterableToList((Iterable<Number>) next.get("rel_tos")));
                item.setRelTagEndingPoints(iterableToList((Iterable<Number>) next.get("rel_toe")));
                item.setRelevance(pageRanks.containsKey(tagId) ? pageRanks.get(tagId) : 0);
                keywordsOccurrences.add(item);
                if (!keywordMap.containsKey(tagId)) {
                    keywordMap.put(tagId, item);
                } else { // new
                    keywordMap.get(tagId).update(item); // new
                }
                //System.out.println(" Adding for " + item.getValue() + ": " + item.getRelatedTags());
            }
            if (res != null) {
                res.close();
            }
            tx.success();
        } catch (Exception e) {
            LOG.error("Error while running TextRank evaluation: ", e);
            return false;
        }

        Map<String, Keyword> results = new HashMap<>();

        while (!keywordsOccurrences.isEmpty()) {
            final AtomicReference<KeywordExtractedItem> keywordOccurrence
                    = new AtomicReference<>(keywordsOccurrences.remove(0));
            final AtomicReference<String> currValue = new AtomicReference<>(keywordOccurrence.get().getValue());
            final AtomicReference<Double> currRelevance = new AtomicReference<>(keywordOccurrence.get().getRelevance());
            final AtomicReference<TfIdfObject> currTfIdf = new AtomicReference<>(!tfidfMap.isEmpty() ? tfidfMap.get(keywordOccurrence.get().getTagId()) : new TfIdfObject(1.0d, 1.0d));
            final AtomicReference<Integer> currNTopRated = new AtomicReference<>(0);
            Set<Long> relTagIDs = getRelTagsIntoDepth(keywordOccurrence.get(), keywordsOccurrences);
            //System.out.println("\n val: " + keywordOccurrence.get().getValue() + ", relTagIDs: " + relTagIDs.stream().map(el -> idToValue.get(el)).collect(Collectors.joining(", ")));
            relTagIDs.retainAll(topThird); // keep only those that are among top 1/3
            //System.out.println("   relTagIDs among top 1/3: " + relTagIDs.stream().map(el -> idToValue.get(el)).collect(Collectors.joining(", ")));
            if (!useDependencies && !topThird.contains(keywordOccurrence.get().getTagId())) // if useDependencies==false, keep only those keywords that are among top 1/3
                continue;
            if (useDependencies && !topThird.contains(keywordOccurrence.get().getTagId()) && relTagIDs.size()==0)
                continue;
            //System.out.println("\n> " + currValue.get() + " - " + keywordOccurrence.get().getStartPosition());
            Map<String, Keyword> localResults;
            if (topThird.contains(keywordOccurrence.get().getTagId()))
                currNTopRated.set(currNTopRated.get() + 1);
            do {
                int endPosition = keywordOccurrence.get().getEndPosition();
                //System.out.println("  cur: " + currValue.get() + ". Examining next level");
                localResults = checkNextKeyword(keywordOccurrence.get(), coOccurrence, keywordMap);
                if (localResults.size() > 0) {
                    //System.out.println("    related tags: " + localResults.entrySet().stream().map(en -> en.getKey()).collect(Collectors.joining(", ")));
                    keywordOccurrence.set(null);
                    localResults.entrySet().stream().forEach((item) -> {
                        KeywordExtractedItem nextKeyword = keywordsOccurrences.get(0);
                        //System.out.println("      " + nextKeyword.getValue() + ": " + nextKeyword.getStartPosition());
                        if (nextKeyword != null && nextKeyword.getValue().equalsIgnoreCase(item.getKey())
                                && (topThird.contains(nextKeyword.getTagId()) || useDependencies)
                                && (nextKeyword.getStartPosition() - endPosition) == 1) // crucial condition for graphs from co-occurrences, but very useful also for graphs from dependencies
                                //&& ((nextKeyword.getStartPosition() - endPosition) == 1 || useDependencies))
                        {
                            String newCurrValue = currValue.get().trim().split("_")[0] + " " + item.getKey();
                            //System.out.println(">> " + newCurrValue);
                            double newCurrRelevance = currRelevance.get() + item.getValue().getRelevance();
                            if (topThird.contains(nextKeyword.getTagId()))
                                currNTopRated.set(currNTopRated.get() + 1);
                            currValue.set(newCurrValue);
                            currRelevance.set(newCurrRelevance);
                            if (tfidfMap != null && tfidfMap.containsKey(nextKeyword.getTagId())) {
                                // tf and idf are sums of tf and idf of all words in a phrase
                                double tf  = currTfIdf.get().getTf() + tfidfMap.get(nextKeyword.getTagId()).getTf();
                                double idf = currTfIdf.get().getIdf() + tfidfMap.get(nextKeyword.getTagId()).getIdf();

                                // minimal tf and idf
                                //double tf  = currTfIdf.get().getTf() < tfidfMap.get(nextKeyword.getTagId()).getTf() ? currTfIdf.get().getTf() : tfidfMap.get(nextKeyword.getTagId()).getTf();
                                //double idf = currTfIdf.get().getIdf() < tfidfMap.get(nextKeyword.getTagId()).getIdf() ? currTfIdf.get().getIdf() : tfidfMap.get(nextKeyword.getTagId()).getIdf();

                                currTfIdf.set(new TfIdfObject(tf, idf));
                            }
                            keywordOccurrence.set(nextKeyword);
                            keywordsOccurrences.remove(0);
                        }// else {
                            //LOG.warn("Next keyword not found!");
                        //    keywordOccurrence.set(null);
                        //}
                    });
                }
            } while (!localResults.isEmpty() && keywordOccurrence.get() != null);
            if (currNTopRated.get() > 0)
                addToResults(currValue.get(), currRelevance.get(), currTfIdf.get(), currNTopRated.get(), results, 1);
            //System.out.println("< " + currValue.get());
        }

        if (expandNEs) {
            // add named entities that contain at least some of the top 1/3 of words
            for (Long key: neExpanded.keySet()) {
                if (neExpanded.get(key).stream().filter(v -> topThird.contains(v)).count() == 0)
                    continue;
                if (wrongNEs.contains(key))
                    continue;
                String keystr = idToValue.get(key);//.toLowerCase();
                double pr = pageRanks.containsKey(key) ? pageRanks.get(key) : 0.;
                if (pr == 0.) // set PageRank value of a NE to max value of PR of it's composite words
                    pr = (double) pageRanks.entrySet().stream()
                            .filter(en -> neExpanded.get(key).contains(en.getKey()))
                            .mapToDouble(en -> en.getValue())
                            .max().orElse(0.);
                addToResults(keystr,
                    pr,
                    tfidfMap != null && tfidfMap.containsKey(key) ? tfidfMap.get(key) : new TfIdfObject(1., 1.),
                    (int) (neExpanded.get(key).stream().filter(v -> topThird.contains(v)).count()),
                    results,
                    1);
            }
        }

        computeTotalOccurrence(results);
        if (cleanKeywords) {
            results = cleanFinalKeywords(results, n_oneThird);
        }
        peristKeyword(results, annotatedText);

        return true;
    }

    private Map<String, Keyword> checkNextKeyword(KeywordExtractedItem keywordOccurrence, Map<Long, Map<Long, CoOccurrenceItem>> coOccurrences, Map<Long, KeywordExtractedItem> keywords) {
        long tagId = keywordOccurrence.getTagId();
        Map<String, Keyword> results = new HashMap<>();
        if (!coOccurrences.containsKey(tagId))
            return results;

        Map<Integer, Set<Long>> mapStartId = createThisMapping(coOccurrences.get(tagId)); // mapping: sourceStartPosition -> Set(destination tagIDs)
        Set<Long> coOccurrence = mapStartId.get(keywordOccurrence.getStartPosition());
        if (coOccurrence == null) {
            return results;
        }

        Iterator<Long> iterator = coOccurrence.stream()
                .filter((ccEntry) -> ccEntry != tagId)
                .filter((ccEntry) -> keywords.containsKey(ccEntry))
                .iterator();

        while (iterator.hasNext()) {
            Long ccEntry = iterator.next();
            String relValue = keywords.get(ccEntry).getValue();
            //System.out.println("checkNextKeyword >> " + relValue);
            List<Long> merged = new ArrayList<>(keywords.get(ccEntry).getRelatedTags());
            merged.retainAll(keywordOccurrence.getRelatedTags()); // new
            //System.out.println("    co-occurring tag = " + idToValue.get(ccEntry) + ", related tags = " + keywords.get(ccEntry).getRelatedTags().stream().collect(Collectors.joining(", ")));
            //System.out.println("      merged = " + merged.stream().collect(Collectors.joining(", ")));
            // TO DO: even when using dependencies, we should be able to merge words that are next to each other but that have no dependency (?)
            if (!useDependencies || keywordOccurrence.getRelatedTags().contains(keywords.get(ccEntry).getTagId()) || merged.size()>0) {
                //System.out.println("checkNextKeyword >>> " + relValue);
                addToResults(relValue,
                        keywords.get(ccEntry).getRelevance(),
                        new TfIdfObject(0., 0.),
                        0,
                        results, 1);
            }
        }

        return results;
    }

    private void addToResults(String res, double relevance, TfIdfObject tfidf, int nTopRated, Map<String, Keyword> results, int occurrences) {
        //System.out.println("addToResults: " + res + " " + relevance + " " + occurrences);
        if (res != null) {
            String resLower = res.toLowerCase();
            if (results.containsKey(resLower)) {
                results.get(resLower).incCountsBy(occurrences);
                //System.out.println("+inc");
            } else {
                final Keyword keyword = new Keyword(resLower, occurrences);
                keyword.setOriginalTagId(res);
                keyword.setRelevance(relevance);
                keyword.setTf(tfidf.getTf());
                keyword.setIdf(tfidf.getIdf());
                keyword.setNTopRated(nTopRated);
                results.put(resLower, keyword);
            }
        }
    }

    private void peristKeyword(Map<String, Keyword> results, Node annotatedText) {
        LOG.info("--- Results: ");
        KeywordPersister persister = NLPManager.getInstance().getPersister(Keyword.class);
        persister.setLabel(keywordLabel);
        results.entrySet().stream()
                .forEach(en -> {
                    // check keyword consistency
                    if (en.getKey().split("_").length > 2) {
                        LOG.warn("Tag " + en.getKey() + " has more than 1 underscore symbols, newly created " + keywordLabel.name() + " node might be wrong.");
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

    private Set<Long> getRelTagsIntoDepth(KeywordExtractedItem kwOccurrence, List<KeywordExtractedItem> kwOccurrences) {
        //Map<String, Long> valToId = idToValue.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        Set<Long> relTags = new HashSet<Long>(kwOccurrence.getRelatedTags());
        kwOccurrences.stream()
            //.filter(el -> relTags.contains(el.getValue()))
            .filter(el -> relTags.contains(el.getTagId()) || el.getRelatedTags().stream().anyMatch(l -> relTags.contains(l)))
            .forEach(el -> {
                relTags.addAll(el.getRelatedTags());
                relTags.add(el.getTagId());
                //System.out.println("  ADDING (" + el.getValue() + "): " + el.getRelatedTags() + ", size = " + el.getRelatedTags().size());
            });
        return relTags;//.stream().map(el -> valToId.get(el)).collect(Collectors.toSet());
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

    private void initializeNodeWeights_TfIdf(Map<Long, TfIdfObject> nodeWeights, Node annotatedText, Map<Long, Map<Long, CoOccurrenceItem>> coOccurrences) {
        //Map<Long, TfIdfObject> nodeWeights = new HashMap<>();
        if (coOccurrences != null) {
            coOccurrences.entrySet().stream().forEach((coOccurrence) -> {
                nodeWeights.put(coOccurrence.getKey(), new TfIdfObject(1.0d, 1.0d)); // source
                coOccurrence.getValue().entrySet().stream()
                    .filter((entry) -> !nodeWeights.containsKey(entry.getKey()))
                    .forEach((entry) -> {
                        nodeWeights.put(entry.getValue().getDestination(), new TfIdfObject(1.0d, 1.0d)); // destination
                    });
            });
        }

        String query = "MATCH (doc:AnnotatedText)\n"
                + "WITH count(doc) as documentsCount\n"
                + "MATCH (a:AnnotatedText)-[:CONTAINS_SENTENCE]->(:Sentence)-[ht:HAS_TAG]->(t:Tag)\n"
                + "WHERE id(a) = {id} \n"
                + "WITH t, sum(ht.tf) as tf, documentsCount\n"
                + "MATCH (a:AnnotatedText)-[:CONTAINS_SENTENCE]->(:Sentence)-[:HAS_TAG]->(t)\n"
                + "RETURN id(t) as tag, t.id as tagVal, tf, count(distinct a) as docCountForTag, documentsCount\n";

        try (Transaction tx = database.beginTx();) {
            Result res = database.execute(query, Collections.singletonMap("id", annotatedText.getId()));
            while (res != null && res.hasNext()) {
                Map<String, Object> next = res.next();
                Long tag = (Long) next.get("tag");
                if (coOccurrences != null && !nodeWeights.keySet().contains(tag)) // initialize only those that are needed
                    continue;
                long tf = ((Long) next.get("tf"));

                long docCount = (long) next.get("documentsCount");
                long docCountTag = (long) next.get("docCountForTag");
                double idf = Math.log10(1.0d * docCount / docCountTag);

                if (nodeWeights.containsKey(tag)) {
                    nodeWeights.get(tag).setTf(tf);
                    nodeWeights.get(tag).setIdf(idf);
                } else {
                    nodeWeights.put(tag, new TfIdfObject(tf, idf));
                }
                
                //LOG.info((String) next.get("tagVal") + ": tf = " + tf + ", idf = " + idf + " (docCountTag = " + docCountTag + "), tf*idf = " + tf*idf);
            }
            tx.success();
        } catch (Exception e) {
            LOG.error("Error while initializing node weights: ", e);
            return;// nodeWeights;
        }

        return;// nodeWeights;
    }

    private <T> List<T> iterableToList(Iterable<T> it) {
        List<T> newList = new ArrayList<>();
        for (T obj : it) {
            newList.add(obj);
        }
        return newList;
    }

    private <T> Set<T> iterableToSet(Iterable<T> it) {
        Set<T> newList = new HashSet<>();
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

    private Map<Integer, Set<Long>> createThisMapping(Map<Long, CoOccurrenceItem> coOccorrence) {
        Map<Integer, Set<Long>> result = new HashMap<>();
        coOccorrence.entrySet().stream().forEach((entry) -> {
                entry.getValue().getStartingPositions()
                        .forEach((pairStartingPoint) -> {
                            if (pairStartingPoint.first() < pairStartingPoint.second()) {
                                if (!result.containsKey(pairStartingPoint.first())) {
                                    result.put(pairStartingPoint.first(), new TreeSet());
                                }
                                result.get(pairStartingPoint.first()).add(entry.getValue().getDestination());
                            }
                        });
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
    
    private Map<String, Keyword> cleanFinalKeywords(Map<String, Keyword> results, int topx) {
        Map<String, Keyword> newResults = new HashMap<>(results);
        results.entrySet().stream().forEach((entry) -> {
            results.entrySet().stream().forEach((innerEntry) -> {
                if (entry.getValue().getWordsCount() < innerEntry.getValue().getWordsCount()
                        && innerEntry.getValue().getRawKeyword().contains(entry.getValue().getRawKeyword())
                        //&& entry.getValue().getWordsCount() == 1 // new
                        //&& entry.getValue().getMeanRelevance() < innerEntry.getValue().getMeanRelevance() // new
                    ) {
                    //newResults.remove(entry.getKey());
                    int nDiff = innerEntry.getValue().getWordsCount() - entry.getValue().getWordsCount();
                    double entryMult = entry.getValue().getRelevance() * entry.getValue().getTfIdf(); ///entry.getValue().getWordsCount();
                    double innentryMult = innerEntry.getValue().getRelevance() * innerEntry.getValue().getTfIdf(); ///innerEntry.getValue().getWordsCount();
                    if (entry.getValue().getWordsCount() == 1 )//&& innentryMult/entryMult > (1 + 1.0f * nDiff/entry.getValue().getWordsCount()))
                        newResults.remove(entry.getKey());
                    /*else {
                        //double v = ((entry.getValue().getRelevance() - relevanceAvg)/relevanceSigma + (entry.getValue().getTfIdf() - tfidfAvg)/tfidfSigma) / entry.getValue().getWordsCount();
                        //double vInn = ((innerEntry.getValue().getRelevance() - relevanceAvg)/relevanceSigma + (innerEntry.getValue().getTfIdf() - tfidfAvg)/tfidfSigma ) / innerEntry.getValue().getWordsCount();
                        double v = entry.getValue().getRelevance() / entry.getValue().getWordsCount();
                        double vInn = innerEntry.getValue().getRelevance() / innerEntry.getValue().getWordsCount();
                        if (vInn > 1.2 * v)
                            newResults.remove(entry.getKey());
                        else if (vInn < 0.8 * v)
                            newResults.remove(innerEntry.getKey());
                    }*/
                    //else if (1.0f * innerEntry.getValue().getNTopRated() / innerEntry.getValue().getWordsCount() < 0.5)
                    //    newResults.remove(innerEntry.getKey());
                    //if ( (innerEntry.getValue().getTfIdf() - entry.getValue().getTfIdf()) > 1.0 * nDiff && innerEntry.getValue().getTfIdf() / entry.getValue().getTfIdf() > (1 + 0.2 * nDiff))
                    //    newResults.remove(entry.getKey());
                    //if ( !( Math.abs(entry.getValue().getRelevance() - innerEntry.getValue().getRelevance()) < (0.10 * innerEntry.getValue().getWordsCount()) * entry.getValue().getRelevance() ) )//|| entry.getValue().getWordsCount() == 1 )
                    //    newResults.remove(entry.getKey());
                    //if ( Math.abs(entry.getValue().getRelevance() - innerEntry.getValue().getRelevance()) > 0.8 * entry.getValue().getMeanRelevance() )
                    //    newResults.remove(entry.getKey());
                    //if ( innentryMult / innerEntry.getValue().getWordsCount() > entryMult/entry.getValue().getWordsCount() )
                    //    newResults.remove(entry.getKey());
                    //else if ( innentryMult / innerEntry.getValue().getWordsCount() < 0.9 * entryMult/entry.getValue().getWordsCount() )
                    //    newResults.remove(innerEntry.getKey());
                }
            });
        });

        // Use (PR * tf*idf) for selecting top 1/3 of keywords / key phrases
        // Crucial piece of code for TextRank with dependencies enrichment
        if (useDependencies && cooccurrencesFromDependencies) {
            Map<String, Double> pom = newResults.entrySet().stream()
                //.collect(Collectors.toMap( Map.Entry::getKey, e -> e.getValue().getRelevance() * e.getValue().getTfIdf()/e.getValue().getWordsCount()/e.getValue().getWordsCount() ));
                //.collect(Collectors.toMap( Map.Entry::getKey, e -> e.getValue().getRelevance() * e.getValue().getIdf() ));
                //.collect(Collectors.toMap( Map.Entry::getKey, e -> (e.getValue().getRelevance() - relevanceAvg)/relevanceSigma + (e.getValue().getTfIdf() - tfidfAvg)/tfidfSigma ));
                .collect(Collectors.toMap( Map.Entry::getKey, e -> e.getValue().getTfIdf() ));
            pom.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .skip(topx)
                .forEach(entry -> newResults.remove(entry.getKey()));
        }

        return newResults;
    }

    public static class Builder {

        private static final String[] STOP_WORDS = {"new", "old", "large", "big", "vast", "small", "many", "few", "good", "better", "best", "bad", "worse", "worst"};
        private static final String[] ADMITTED_POS = {"NN", "NNS", "NNP", "NNPS", "JJ", "JJR", "JJS"};
        private static final String[] FORBIDDEN_NE = {"NER_Number", "NER_Ordinal", "NER_Percent", "NER_Date", "NER_Duration"}; //"NER_Date", "NER_Duration"
        private static final String[] FORBIDDEN_POS = {"CC", "DT", "EX", "IN", "LS", "MD", "PDT", "PRP", "PRP$", "RBR", "RBS", "TO", "UH", "WDT", "WP", "WP$", "WRB"};
        private static final String[] STOP_WORDS_MEDIUM = {"now", "later", "least", "well", "always", "new", "old", "good", "better", "best", "great", "bad", "worse", "worst", "much", "more", "less", "several", "larger", "smaller", "big", "lower", "widely", "highly", "many", "few", "with", "without", "via", "therefore", "furthermore", "whose", "whether", "though", "although", "to", "not", "of", "prior", "instead", "upon", "every", "together", "across", "toward", "towards", "since", "around", "along", "onto", "into", "already", "whilst", "while", "than", "then", "anyway", "whole", "thus", "throughout", "through", "during", "above", "below", "use", "due", "do", "be", "have", "got", "might", "may", "shall", "can", "could", "would", "will", "such", "like", "other", "another", "far", "away"};
        private static final String[] STOP_WORDS_LARGE = {"now", "recently", "late", "later", "lately", "recent", "finally", "often", "always", "new", "old", "novel", "least", "last", "well", "good", "better", "best", "great", "bad", "worse", "worst", "much", "more", "less", "several", "large", "larger", "small", "smaller", "big", "vast", "little", "lower", "long", "short", "wide", "widely", "highly", "many", "few", "with", "without", "via", "therefore", "furthermore", "whose", "whether", "though", "although", "to", "not", "of", "prior", "instead", "upon", "every", "together", "across", "toward", "towards", "since", "around", "along", "onto", "into", "already", "whilst", "while", "than", "then", "anyway", "whole", "thus", "throughout", "through", "during", "above", "below", "use", "due", "do", "be", "have", "got", "make", "might", "may", "shall", "can", "could", "would", "will", "entire", "entirely", "overall", "useful", "usefully", "easy", "easier", "certain", "such", "like", "difficult", "necessary", "unnecessary", "full", "fully", "empty", "successful", "successfully", "unsuccessful", "unsuccessfully", "especially", "usual", "usually", "other", "another", "far", "away"};

        private static final boolean DEFAULT_REMOVE_STOP_WORDS = false;
        private static final boolean DEFAULT_DIRECTION_MATTER = false;
        private static final boolean DEFAULT_RESPECT_SENTENCES = false;
        private static final boolean DEFAULT_USE_TYPED_DEPENDENCIES = true;
        private static final boolean DEFAULT_COOCURRENCES_FROM_DEPENDENCIES = false;
        private static final boolean DEFAULT_CLEAN_KEYWORDS = true;
        private static final int DEFAULT_CO_OCCURRENCE_WINDOW = 2;
        private static final double DEFAULT_TAGS_TOPX = 1/3.0f;

        private final GraphDatabaseService database;
        private boolean removeStopWords = DEFAULT_REMOVE_STOP_WORDS;
        private boolean directionsMatter = DEFAULT_DIRECTION_MATTER;
        private boolean respectSentences = DEFAULT_RESPECT_SENTENCES;
        private boolean useDependencies = DEFAULT_USE_TYPED_DEPENDENCIES;
        private boolean cooccurrencesFromDependencies = DEFAULT_COOCURRENCES_FROM_DEPENDENCIES;
        private boolean cleanKeywords = DEFAULT_CLEAN_KEYWORDS;
        private int cooccurrenceWindow = DEFAULT_CO_OCCURRENCE_WINDOW;
        private double topxTags = DEFAULT_TAGS_TOPX;
        private Label keywordLabel;
        //private Set<String> stopWords = new HashSet<>(Arrays.asList(STOP_WORDS));
        private Set<String> stopWords = new HashSet<>(Arrays.asList(STOP_WORDS_MEDIUM));
        //private Set<String> stopWords = new HashSet<>(Arrays.asList(STOP_WORDS_LARGE));
        private List<String> admittedPOSs = Arrays.asList(ADMITTED_POS);
        private List<String> forbiddenNEs = Arrays.asList(FORBIDDEN_NE);
        private List<String> forbiddenPOSs = Arrays.asList(FORBIDDEN_POS);


        public Builder(GraphDatabaseService database, DynamicConfiguration configuration) {
            this.database = database;
            this.keywordLabel = configuration.getLabelFor(Labels.Keyword);
        }

        public TextRank build() {
            TextRank result = new TextRank(database,
                    removeStopWords,
                    directionsMatter,
                    respectSentences,
                    useDependencies,
                    cooccurrencesFromDependencies,
                    cleanKeywords,
                    cooccurrenceWindow,
                    topxTags,
                    keywordLabel,
                    stopWords,
                    admittedPOSs,
                    forbiddenNEs,
                    forbiddenPOSs);
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

        public Builder useDependencies(boolean val) {
            this.useDependencies = val;
            return this;
        }

        public Builder useDependenciesForCooccurrences(boolean val) {
            this.cooccurrencesFromDependencies = val;
            return this;
        }

        public Builder setCooccurrenceWindow(int val) {
            this.cooccurrenceWindow = val;
            return this;
        }

        public Builder setAdmittedPOSs(List<String> admittedPOSs) {
            this.admittedPOSs = admittedPOSs;
            return this;
        }

        public Builder setForbiddenNEs(List<String> forbiddenNEs) {
            this.forbiddenNEs = forbiddenNEs;
            return this;
        }

        public Builder setTopXTags(double topXTags) {
            this.topxTags = topXTags;
            return this;
        }

        public Builder setKeywordLabel(String keywordLabel) {
            //this.keywordLabel = Labels.valueOf(keywordLabel); // doesn't work because Labels is enum, while we want customizable keyword labels
            this.keywordLabel = Label.label(keywordLabel);
            return this;
        }
        
        public Builder setCleanKeywords(boolean cleanKeywords) {
            this.cleanKeywords = cleanKeywords;
            return this;
        }
    }

    private class KeywordExtractedItem {

        private final long tagId;
        private int startPosition;
        private int endPosition;
        private String value;
        private double relevance;
        private List<Long> relatedTags;
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

        public List<Long> getRelatedTags() {
            return relatedTags;
        }

        public void setRelatedTags(List<Long> relatedTags) {
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

        public void update(KeywordExtractedItem item) {
            this.relatedTags.addAll(item.getRelatedTags());
            this.relTagStartingPoints.addAll(item.getRelTagStartingPoints());
            this.relTagEndingPoints.addAll(item.getRelTagEndingPoints());
        }
    }

}
