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

import static com.graphaware.nlp.persistence.constants.Relationships.DESCRIBES;

import java.util.*;
import java.util.stream.Collectors;

import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.persistence.constants.Labels;
import com.graphaware.nlp.persistence.constants.Properties;
import com.graphaware.nlp.persistence.constants.Relationships;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

public class TextRank {

    private static final Logger LOG = LoggerFactory.getLogger(TextRank.class);
    private final GraphDatabaseService database;
    private final DynamicConfiguration configuration;
    private boolean removeStopWords;
    private boolean directionsMatter;
    private boolean respectSentences;
    private boolean useTfIdfWeights;
    private boolean useDependencies;
    private int cooccurrenceWindow;
    private int maxSingles;
    private double phrases_topx_words;
    private double singles_topx_words;
    private Label keywordLabel;
    private Set<String> stopWords;
    private List<String> admittedPOSs;
    private Map<Long, String> idToValue = new HashMap<>();

    public TextRank(GraphDatabaseService database, DynamicConfiguration configuration) {
        this.database = database;
        this.configuration = configuration;
        this.stopWords = new HashSet<>(Arrays.asList("new", "old", "large", "big", "small", "many", "few", "best", "worst"));
        this.removeStopWords = false;
        this.directionsMatter = false;
        this.respectSentences = false;
        this.useTfIdfWeights = false;
        this.useDependencies = false;
        this.cooccurrenceWindow = 2;
        this.maxSingles = 15;
        this.keywordLabel = Label.label("Keyword");
        this.admittedPOSs = Arrays.asList("NN", "NNS", "NNP", "NNPS", "JJ", "JJR", "JJS", "Unknown"); // "Unknown" because we want to keep tags that have unknown POS

    }

    public void setStopwords(String stopwords) {
        if (stopwords.split(",").length > 0 && stopwords.split(",")[0].equals("+")) // if the stopwords list starts with "+,....", append the list to the default 'stopWords' set
            this.stopWords.addAll( Arrays.asList(stopwords.split(",")).stream().filter(str -> !str.equals("+")).map(str -> str.trim().toLowerCase()).collect(Collectors.toSet()) );
        else
            this.stopWords = Arrays.asList(stopwords.split(",")).stream().map(str -> str.trim().toLowerCase()).collect(Collectors.toSet());
        this.removeStopWords = true;
    }

    public void removeStopWords(boolean val) {
        this.removeStopWords = val;
    }

    public void respectDirections(boolean val) {
        this.directionsMatter = val;
    }

    public void respectSentences(boolean val) {
        this.respectSentences = val;
    }

    public void useTfIdfWeights(boolean val) {
        this.useTfIdfWeights = val;
    }

    public void useDependencies(boolean val) {
        this.useDependencies = val;
    }

    public void setCooccurrenceWindow(int val) {
        this.cooccurrenceWindow = val;
    }

    public void setMaxSingleKeywords(int val) {
        this.maxSingles = val;
    }

    public void setTopXWordsForPhrases(double val) {
        this.phrases_topx_words = val;
    }

    public void setTopXSinglewordKeywords(double val) {
        this.singles_topx_words = val;
    }

    public void setKeywordLabel(String lab) {
        this.keywordLabel = Label.label(lab);
    }

    public Map<Long, Map<Long, CoOccurrenceItem>> createCooccurrences(Node annotatedText) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", annotatedText.getId());

        // query based on orignal TextRank: it doesn't care about sentence boundaries (it connects last word of a sentence with 1st word of the next sentence)
        String query = "MATCH (a:AnnotatedText)-[:CONTAINS_SENTENCE]->(s:Sentence)-[:SENTENCE_TAG_OCCURRENCE]->(to:TagOccurrence)\n"
                + "WHERE id(a) = {id} \n"
                + "WITH to\n"
                + "ORDER BY to.startPosition\n"
                + "MATCH (to)-[:TAG_OCCURRENCE_TAG]->(t:Tag)\n"
                + "WHERE size(t.value) > 2\n"
                + "WITH collect(t) as tags\n"
                + "UNWIND range(0, size(tags) - 2, 1) as i\n"
                + "RETURN id(tags[i]) as tag1, id(tags[i+1]) as tag2, tags[i].value as tag1_val, tags[i+1].value as tag2_val, "
                    + "(case tags[i].pos when null then 'Unknown,' else reduce(pos='', p in tags[i].pos | pos+p+',') end) as pos1, (case tags[i+1].pos when null then 'Unknown,' else reduce(pos='', p in tags[i+1].pos | pos+p+',') end) as pos2\n";

        // a query for creating co-occurrences per sentence
        if (respectSentences) {
            query = "MATCH (a:AnnotatedText)-[:CONTAINS_SENTENCE]->(s:Sentence)-[:SENTENCE_TAG_OCCURRENCE]->(to:TagOccurrence)\n"
                + "WHERE id(a) = {id} \n"
                + "WITH s, to\n"
                + "ORDER BY s.sentenceNumber, to.startPosition\n"
                + "MATCH (to)-[:TAG_OCCURRENCE_TAG]->(t:Tag)\n"
                + "WHERE size(t.value) > 2\n"
                + "WITH s, collect(t) as tags\n"
                + "ORDER BY s.sentenceNumber\n"
                + "UNWIND range(0, size(tags) - 2, 1) as i\n"
                + "RETURN s, id(tags[i]) as tag1, id(tags[i+1]) as tag2, tags[i].value as tag1_val, tags[i+1].value as tag2_val, "
                    + "(case tags[i].pos when null then 'Unknown,' else reduce(pos='', p in tags[i].pos | pos+p+',') end) as pos1, (case tags[i+1].pos when null then 'Unknown,' else reduce(pos='', p in tags[i+1].pos | pos+p+',') end) as pos2\n";
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
        int n_skips = 1;
        while (res != null && res.hasNext()) {
            Map<String, Object> next = res.next();
            Long tag1 = (Long) next.get("tag1");
            Long tag2 = (Long) next.get("tag2");
            List<String> pos1 = Arrays.asList( ((String) next.get("pos1")).split(",") );
            List<String> pos2 = Arrays.asList( ((String) next.get("pos2")).split(",") );

            // check whether POS of both tags are admitted
            boolean bPOS1 = pos1.stream().filter(pos -> pos!=null && admittedPOSs.contains(pos)).count()!=0;
            boolean bPOS2 = pos2.stream().filter(pos -> pos!=null && admittedPOSs.contains(pos)).count()!=0;

            // fill tag co-occurrences (adjacency matrix)
            //   * window of words N = 2 (i.e. neighbours only => both neighbours must pass cleaning requirements)
            if (bPOS1 && bPOS2) {
                addTagToCoOccurrence(results, tag1, tag2);
                if (!directionsMatter) // when direction of co-occurrence relationships is not important
                    addTagToCoOccurrence(results, tag2, tag1);
                //LOG.info("Adding co-occurrence: " + (String) next.get("tag1_val") + " -> " + (String) next.get("tag2_val"));
                n_skips = 1;
            }
            //   * window of words N > 2
            else if (bPOS2) { // after possibly skipping some words, we arrived to a tag2 that complies with cleaning requirements
                if (n_skips < cooccurrenceWindow) {
                    addTagToCoOccurrence(results, previous1, tag2);
                    if (!directionsMatter)
                        addTagToCoOccurrence(results, tag2, previous1);
                    //LOG.info("  window N=" + (n_skips+1) + " co-occurrence: " + idToValue.get(previous1) + " -> " + (String) next.get("tag2_val"));
                }
                n_skips = 1;
            } else { // skip to another word
                n_skips++;
                if (bPOS1) previous1 = tag1;
            }

            // for logging purposses
            idToValue.put(tag1, (String) next.get("tag1_val"));
            idToValue.put(tag2, (String) next.get("tag2_val"));
        }
        return results;
    }

    private void addTagToCoOccurrence(Map<Long, Map<Long, CoOccurrenceItem>> results, Long tag1, Long tag2) {
        Map<Long, CoOccurrenceItem> mapTag1;
        if (!results.containsKey(tag1)) {
            mapTag1 = new HashMap<>();
            results.put(tag1, mapTag1);
        } else {
            mapTag1 = results.get(tag1);
        }
        if (mapTag1.containsKey(tag2)) {
            mapTag1.get(tag2).incCount();
        } else {
            mapTag1.put(tag2, new CoOccurrenceItem(tag1, tag2));
        }
    }

    public boolean evaluate(Node annotatedText, Map<Long, Map<Long, CoOccurrenceItem>> coOccurrence, int iter, double damp, double threshold) {
        PageRank pageRank = new PageRank(database);
        if (useTfIdfWeights)
            pageRank.setNodeWeights( initializeNodeWeights_TfIdf(annotatedText, coOccurrence) );
        Map<Long, Double> pageRanks = pageRank.run(coOccurrence, iter, damp, threshold);

        int n_oneThird = (int) (pageRanks.size() * phrases_topx_words);
        List<Long> topThird = getTopX(pageRanks, n_oneThird);

        int n_topx_singles = (int) (pageRanks.size() * singles_topx_words);
        n_topx_singles = n_topx_singles > maxSingles ? maxSingles : n_topx_singles;
        List<Long> topSingles = getTopX(pageRanks, n_topx_singles);

        //pageRanks.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
        //    .forEach(en -> LOG.info(idToValue.get(en.getKey()) + ": " + en.getValue()));
        //LOG.info("Sum of PageRanks = " + pageRanks.values().stream().mapToDouble(Number::doubleValue).sum());
        LOG.info("Top " + n_oneThird + " tags: " + topThird.stream().map(id -> idToValue.get(id)).collect(Collectors.joining(", ")));

        // if the use of dependencies is required, check that they actually exist; if not, behave like `useDependencies` was set to false
        if (useDependencies) {
            useDependencies = checkDependencies(annotatedText);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("id", annotatedText.getId());
        params.put("nodeList", topThird);
        params.put("posList", admittedPOSs);
        Result res;
        try (Transaction tx = database.beginTx();) {
            res = database.execute(
                "MATCH (node:Tag)<-[:TAG_OCCURRENCE_TAG]-(to:TagOccurrence)<-[:SENTENCE_TAG_OCCURRENCE]-(:Sentence)<-[:CONTAINS_SENTENCE]-(a:AnnotatedText)\n"
                + "WHERE id(a) = {id} and id(node) IN {nodeList}\n"
                + "OPTIONAL MATCH (to)-[:COMPOUND|AMOD]-(to2:TagOccurrence)-[:TAG_OCCURRENCE_TAG]->(t2:Tag)\n"
                + "WHERE not exists(t2.pos) or any(p in t2.pos where p in {posList})\n"
                + "RETURN node.id as tag, to.startPosition as sP, to.endPosition as eP, id(node) as tagId, "
                    + "collect(t2.value) as rel_tags, collect(to2.startPosition) as rel_tos,  collect(to2.endPosition) as rel_toe\n"
                + "ORDER BY sP asc",
                params);
            tx.success();
        } catch (Exception e) {
            LOG.error("Error while running TextRank evaluation: ", e);
            return false;
        }

        // First: merge neighboring words into phrases
        long prev_eP = -1000;
        String prev_tag = "";
        String lang = "";
        Map<Integer, String> keyphrase = new HashMap<>();
        Map<String, KeywordItem> results = new HashMap<>();
        Map<String, KeywordItem> single_keywords = new HashMap<>();
        Map<String, List<WordItem>> dependencies = new HashMap<>();
        while (res != null && res.hasNext()) {
            Map<String, Object> next = res.next();
            long tagId = (long) next.get("tagId");
            int startPosition = ((Number) next.get("sP")).intValue();
            int endPosition = ((Number) next.get("eP")).intValue();

            List<String> rel_tags = iterableToList( (Iterable<String>) next.get("rel_tags") );
            List<Number> rel_tos = iterableToList( (Iterable<Number>) next.get("rel_tos") );
            List<Number> rel_toe = iterableToList( (Iterable<Number>) next.get("rel_toe") );
            List<WordItem> rel_dep = new ArrayList<>();
            for (int i=0; i<rel_tags.size(); i++)  {
                String t = rel_tags.get(i);
                if (removeStopWords && stopWords.stream().anyMatch(str -> str.equals(t)))
                    continue;
                rel_dep.add(new WordItem(
                        ((Number) rel_tos.get(i)).intValue(),
                        ((Number) rel_toe.get(i)).intValue(),
                        rel_tags.get(i)));
            }
            
            String tag = (String) next.get("tag");
            final String[] tagSplit = tag.split("_");
            if (tagSplit.length > 2) {
                LOG.warn("Tag " + tag + " has more than 1 underscore symbols");
            }
            String tagVal = tagSplit[0];
            lang = tagSplit[1];

            if (removeStopWords && stopWords.stream().anyMatch(str -> str.equals(tagVal))) {
                continue;
            }

            // store single keywords and their total counts (`counts_exactMatch` value is wrong here, but it's corrected later on)
            if (topSingles.contains(tagId)) {
                if (single_keywords.containsKey(tag))
                    single_keywords.get(tag).incCounts();
                else
                    single_keywords.put(tag, new KeywordItem(tag));
            }

            if (startPosition - prev_eP <= 1 && dependencies.containsKey(prev_tag)) {
                List<WordItem> merged = new ArrayList<>(dependencies.get(prev_tag));
                merged.retainAll(rel_dep); // 'merged' now contains only elements that are shared between the two lists
                if (merged.size() > 0 || dependencies.get(prev_tag).stream().filter(wi -> wi.getWord().equals(tagVal)).count() > 0 || !useDependencies) {
                    keyphrase.put(startPosition, tagVal);
                    dependencies.put(tagVal, rel_dep);
                }
            } else {
                addKeyphraseToResults(keyphrase, dependencies, prev_eP, lang, results);

                keyphrase.clear();
                keyphrase.put(startPosition, tagVal);
                dependencies.clear();
                dependencies.put(tagVal, rel_dep);
            }
            prev_eP = endPosition;
            prev_tag = tagVal;
        }
        addKeyphraseToResults(keyphrase, dependencies, prev_eP, lang, results);

        // post-process phrases: need to recover `count_total` of all keyphrases
        results.entrySet().stream()
            .forEach(enMain -> {
                results.entrySet().stream()
                    .filter(en -> en.getValue().getNWords() > enMain.getValue().getNWords())
                    .forEach(en -> {
                        if (en.getValue().contains(enMain.getValue()))
                            enMain.getValue().incTotalCount();
                    });
            });

        // Next: include into final result top-x single words
        single_keywords.entrySet().stream()
                .filter(en -> !(removeStopWords && stopWords.stream().anyMatch(str -> str.equals(en.getValue().getRawKeyword()))))
                .forEach(en -> {
                    results.put(en.getKey(), en.getValue());

                    // now: recover `count_exactMatch`
                    results.entrySet().stream()
                        .filter(enSub -> enSub.getValue().getNWords() > 1)
                        .forEach(enSub -> {
                            if (enSub.getValue().contains(en.getValue()))
                                en.getValue().incExactMatchCountBy( (-1) * enSub.getValue().getExactMatchCount() );
                        });
                });

        LOG.info("--- Results: ");
        results.entrySet().stream()
            .forEach(en -> {
                // check keyword consistency
                if (en.getKey().split("_").length > 2) {
                    LOG.warn("Tag " + en.getKey() + " has more than 1 underscore symbols, newly created " + keywordLabel.name() + " node might be wrong");
                }

                // create/merge keyword node
                String val = en.getKey().split("_")[0];
                Node newNode;
                ResourceIterator<Node> findNodes = database.findNodes(keywordLabel, "id", en.getKey());
                if (findNodes.hasNext()) {
                    newNode = findNodes.next();
                } else {
                    newNode = database.createNode(keywordLabel);
                    newNode.setProperty("id", en.getKey());
                    newNode.setProperty("value", val);
                }
                if (newNode != null) {
                    Relationship rel = mergeRelationship(annotatedText, newNode);
                    // override existing properties
                    rel.setProperty("count_exactMatch", en.getValue().getExactMatchCount());
                    rel.setProperty("count", en.getValue().getTotalCount());
                }
                LOG.info(val);
            });

        return true;
    }

    public boolean postprocess() {
        // if a keyphrase in current document contains a keyphrase from any other document, create also DESCRIBES relationship to that other keyphrase
        String query = "match (k:" + keywordLabel.name() + ")\n"
            + "where size(split(k.value, \" \")) > 1\n"
            + "with k, split(k.value, \" \") as ks_orig\n"
            + "match (k2:" + keywordLabel.name() + ")\n"
            + "where size(split(k2.value, \" \")) > size(ks_orig) and not exists( (k)-[:DESCRIBES]->(:AnnotatedText)<-[:DESCRIBES]-(k2) )\n"
            + "with ks_orig, k, k2, split(k2.value, \" \") as ks_check\n"
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
            + "with k, split(k.value, \" \") as ks_orig\n"
            + "match (k2:" + keywordLabel.name() + ")\n"
            + "where size(split(k2.value, \" \")) > size(ks_orig)\n"
            + "with ks_orig, k, k2, split(k2.value, \" \") as ks_check\n"
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

    private void addKeyphraseToResults(Map<Integer, String> keyphrase, Map<String, List<WordItem>> dependencies, long prev_eP, String lang, Map<String, KeywordItem> results) {
        if (keyphrase == null || keyphrase.size() < 2)
            return;
        // append dependency word if it's missing
        if (useDependencies) {
            List<WordItem> missing_words = new ArrayList<>();
            for (String key: dependencies.keySet()) {
                missing_words.addAll(dependencies.get(key));
            }
            missing_words.removeAll(keyphrase.values());
            missing_words.stream()
                .filter(wi -> keyphrase.entrySet().stream().filter(en -> (en.getKey() - wi.getEnd()) == 1 || (wi.getStart() - prev_eP) == 1).count() > 0 )
                .forEach(wi -> keyphrase.put(wi.getStart(), wi.getWord()));
        }

        // create keyphrase from a list of words
        String key = keyphrase.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(en -> en.getValue())
            .collect(Collectors.joining(" "));
        key += "_" + lang;

        // store keyphrase into 'results'
        if (results.containsKey(key)) {
            results.get(key).incCounts();
        } else {
            results.put(key, new KeywordItem(key));
        }
    }

    private List<Long> getTopX(Map<Long, Double> pageRanks, int x) {
        List<Long> topx = pageRanks.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .limit(x)
            .map((item) -> item.getKey()).collect(Collectors.toList());
        return topx;
    }

    private Map<Long, Double> initializeNodeWeights_TfIdf(Node annotatedText, Map<Long, Map<Long, CoOccurrenceItem>> coOccurrences) {
        Map<Long, Double> nodeWeights = new HashMap<Long, Double>();
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
                    continue;
                long tf = ((Long) next.get("tf")).longValue();

                long docCount = (long) next.get("documentsCount");
                long docCountTag = (long) next.get("docCountForTag");
                double idf = Double.valueOf(Math.log10(Double.valueOf(1.0f*docCount/docCountTag).doubleValue())).floatValue();

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
        for (T obj: it) {
            newList.add(obj);
        }
        return newList;
    }

    private boolean checkDependencies(Node annotatedText) {
        try (Transaction tx = database.beginTx();) {
            Result res = database.execute("MATCH (n:AnnotatedText) WHERE id(n) = " + annotatedText.getId() + "\n"
                + "MATCH (n)-[:CONTAINS_SENTENCE]->(s)-[:SENTENCE_TAG_OCCURRENCE]->(sot:TagOccurrence)-[r]->(other:TagOccurrence)\n"
                + "WHERE type(r) IN [\"AMOD\",\"COMPOUND\"]\n"
                + "RETURN count(r) as c"
            );
            tx.success();
            if (res.hasNext() && ((Number) res.next().get("c")).intValue() > 0)
                return true;
        } catch (Exception e) {
            LOG.error("checkDependencies() exception: " + e);
        }
        return false;
    }

    private Relationship mergeRelationship(Node annotatedText, Node newNode) {
        Relationship rel = null;
        Iterable<Relationship> itr = newNode.getRelationships(Direction.OUTGOING, DESCRIBES);
        for (Relationship r: itr) {
            if (r.getEndNode().equals(annotatedText)) {
                rel = r;
                break;
            }
        }
        if (rel == null)
            rel = newNode.createRelationshipTo(annotatedText, DESCRIBES);
        return rel;
    }
}
