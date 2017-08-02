/*
 * Copyright (c) 2013-2016 GraphAware
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

import static com.graphaware.nlp.domain.Labels.AnnotatedText;
import static com.graphaware.nlp.domain.Labels.Keyword;
import static com.graphaware.nlp.domain.Relationships.DESCRIBES;
import com.graphaware.nlp.util.MapUtil;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TextRank {

    private static final Logger LOG = LoggerFactory.getLogger(TextRank.class);
    private final GraphDatabaseService database;
    private boolean removeStopWords;
    private List<String> stopWords;

    public TextRank(GraphDatabaseService database) {
        this.database = database;
        this.stopWords = Arrays.asList("new", "old", "large", "big", "small", "many", "few");
        this.removeStopWords = false;
    }

    public void setStopwords(String stopwords) {
        this.stopWords = Arrays.asList(stopwords.split(",")).stream().map(str -> str.trim().toLowerCase()).collect(Collectors.toList());
        this.removeStopWords = true;
    }

    public void removeStopWords(boolean val) {
        this.removeStopWords = val;
    }

    public boolean createCooccurrencesOld(Node annotatedText, String relType, String relWeight) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", annotatedText.getId());
        params.put("relType", relType);
        params.put("relWeight", relWeight);
        try (Transaction tx = database.beginTx();) {
            database.execute("MATCH (a:AnnotatedText)-[:CONTAINS_SENTENCE]->(s:Sentence)-[:SENTENCE_TAG_OCCURRENCE]->(to:TagOccurrence)\n"
                    + "WHERE a.id = {id} \n"
                    + "WITH s, to\n"
                    + "ORDER BY s.sentenceNumber, to.startPosition\n"
                    + "MATCH (to)-[:TAG_OCCURRENCE_TAG]->(t:Tag)\n"
                    + "WHERE size(t.value) > 2 AND ANY (p in t.pos where p in ['NN', 'NNS', 'NNP', 'NNPS', 'JJ', 'JJR', 'JJS'] )\n" // only nouns and adjectives
                    + "WITH s, collect(t) as tags\n"
                    + "UNWIND range(0, size(tags) - 2, 1) as i\n"
                    + "WITH s, tags[i] as tag1, tags[i+1] as tag2\n"
                    + "MERGE (tag1)-[c:" + relType + "]-(tag2)\n"
                    + "ON CREATE SET c.weight = 1\n"
                    + "ON MATCH SET c.weight = c.weight + 1\n",
                    params);
            tx.success();
        } catch (Exception e) {
            LOG.error("createCooccurrences() failed with QueryExecutionException: " + e.getMessage());
            return false;
        }
        return true;
    }

    public Map<Long, Map<Long, CoOccurrenceItem>> createCooccurrences(Node annotatedText) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", annotatedText.getId());
        Result res = database.execute("MATCH (a:AnnotatedText)-[:CONTAINS_SENTENCE]->(s:Sentence)-[:SENTENCE_TAG_OCCURRENCE]->(to:TagOccurrence)\n"
                + "WHERE id(a) = {id} \n"
                + "WITH s, to\n"
                + "ORDER BY s.sentenceNumber, to.startPosition\n"
                + "MATCH (to)-[:TAG_OCCURRENCE_TAG]->(t:Tag)\n"
                + "WHERE size(t.value) > 2 AND ANY (p in t.pos where p in ['NN', 'NNS', 'NNP', 'NNPS', 'JJ', 'JJR', 'JJS'] )\n" // only nouns and adjectives
                + "WITH s, collect(t) as tags\n"
                + "UNWIND range(0, size(tags) - 2, 1) as i\n"
                + "RETURN s, id(tags[i]) as tag1, id(tags[i+1]) as tag2\n",
                params);
        Map<Long, Map<Long, CoOccurrenceItem>> results = new HashMap<>();
        while (res != null && res.hasNext()) {
            Map<String, Object> next = res.next();
            Long tag1 = (Long) next.get("tag1");
            Long tag2 = (Long) next.get("tag2");
            addTagToCoOccurrence(results, tag1, tag2);
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

    public boolean deleteCooccurrences(Long annotatedID, String relType) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", annotatedID);
        try (Transaction tx = database.beginTx();) {
            database.execute("MATCH (a:AnnotatedText)-[:CONTAINS_SENTENCE]->(s:Sentence)\n"
                    + "WHERE a.id = {id}\n"
                    + "MATCH (s)-[:HAS_TAG]->(:Tag)-[co:" + relType + "]->(:Tag)\n"
                    + "DELETE co",
                    params);
            tx.success();
        } catch (Exception e) {
            LOG.error("deleteCooccurrences() failed with QueryExecutionException: " + e.getMessage());
            return false;
        }
        return true;
    }

    public boolean evaluate(Node annotatedText, Map<Long, Map<Long, CoOccurrenceItem>> coOccurrence, int iter, double damp) {
        PageRank pageRank = new PageRank(database);
        Map<Long, Double> pageRanks = pageRank.run(coOccurrence, iter, damp);
        List<Map.Entry<Long, Double>> sortedPageRanks = MapUtil.getSortedListByValue(pageRanks);
        int maxTop = pageRanks.size() > 10 ? 10 : pageRanks.size();
        List<Long> top10 = sortedPageRanks.subList(0, maxTop).stream()
                .map((item) -> item.getKey()).collect(Collectors.toList());

        Map<String, Object> params = new HashMap<>();
        params.put("id", annotatedText.getId());
        params.put("nodeList", top10);
        Result res = database.execute(
                "MATCH (node)<-[:TAG_OCCURRENCE_TAG]-(to:TagOccurrence)<-[:SENTENCE_TAG_OCCURRENCE]-(:Sentence)<-[:CONTAINS_SENTENCE]-(a:AnnotatedText)\n"
                + "WHERE id(a) = {id} and id(node) in {nodeList}\n"
                + "RETURN node.id as tag, to.startPosition as sP, to.endPosition as eP, id(node) as tagId",
                params);
        // First: merge neighboring words into phrases
        long prev_eP = -1000;
        String lang = "";
        String keyword = "";
        Map<String, Integer> results = new HashMap<>();
        Map<String, Double> keywords = new HashMap<>();
        while (res != null && res.hasNext()) {
            Map<String, Object> next = res.next();
            int sP = (int) next.get("sP");
            int eP = (int) next.get("eP");
            Long tagId = (Long) next.get("tagId");
            Double score = pageRanks.get(tagId);
            //LOG.debug(tag + "\t" + sP + "\t" + eP + "\t" + score);

            String tag = (String) next.get("tag");
            String tag_val = tag.split("_")[0];
            if (tag.split("_").length > 2) {
                LOG.warn("Tag " + tag + " has more than 1 underscore symbols");
            }
            if (removeStopWords && stopWords.stream().anyMatch(str -> str.equals(tag_val))) {
                continue;
            }

            if (sP - prev_eP <= 1) {
                keyword += " " + tag_val;
            } else {
                if (keyword.split(" ").length > 1) {
                    if (results.containsKey(keyword + "_" + lang)) {
                        results.put(keyword + "_" + lang, results.get(keyword + "_" + lang) + 1);
                    } else {
                        results.put(keyword + "_" + lang, 1);
                    }
                }
                keyword = tag_val;
                lang = tag.split("_")[1];
            }
            prev_eP = eP;
            keywords.put(tag, score);
        }

        // Next: include into final result simply top-x single words
        LOG.debug("--- Keywords ---");
        keywords.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(8)
                .forEach(en -> {
                    results.put(en.getKey(), 1);
                    LOG.debug(en.getKey());
                });

        results.keySet().stream().map((key) -> {
            if (key.split("_").length > 2) {
                LOG.warn("Tag " + key + " has more than 1 underscore symbols, newly created Keyword node might be wrong");
            }
            return key;
        }).forEachOrdered((key) -> {
            String val = key.split("_")[0];
            if (!(removeStopWords && stopWords.stream().anyMatch(str -> str.equals(val)))) {
                Node newNode;
                ResourceIterator<Node> findNodes = database.findNodes(Keyword, "id", key);
                if (findNodes.hasNext()) {
                    newNode = findNodes.next();
                } else {
                    newNode = database.createNode(Keyword);
                    newNode.setProperty("id", key);
                    newNode.setProperty("value", val);
                }   if (newNode != null) {
                    Relationship rel = newNode.createRelationshipTo(annotatedText, DESCRIBES);
                    rel.setProperty("count", results.get(key));
                }
            }
        });

        return true;
    }

//    public boolean evaluateOld(Node annotatedText, String relType, String relWeight, Integer iter, Double damp) {
//        Map<String, Object> params = new HashMap<>();
//        params.put("id", annotatedText.getId());
//        params.put("rel", relType);
//        params.put("relw", relWeight);
//        params.put("iter", iter);
//        params.put("damp", damp);
//        Result res;
//        try (Transaction tx = database.beginTx();) {
//            res = database.execute(
//                    "CALL ga.nlp.ml.textrank.computePageRank({annotatedID: {id}, relationshipType: \"" + relType + "\", relationshipWeight: \" " + relWeight + "\","
//                    + " iter: {iter}, damp: {damp}}) YIELD node, score, node_w\n"
//                    + "WITH node, score, node_w\n"
//                    + "ORDER BY score DESC\n"
//                    + "LIMIT 10\n"
//                    + "MATCH (node)<-[:TAG_OCCURRENCE_TAG]-(to:TagOccurrence)<-[:SENTENCE_TAG_OCCURRENCE]-(:Sentence)<-[:CONTAINS_SENTENCE]-(a:AnnotatedText)\n"
//                    + "WHERE a.id = {id}\n"
//                    + "RETURN node.id as tag, to.startPosition as sP, to.endPosition as eP, score, node_w\n"
//                    + "ORDER BY sP\n",
//                    params);
//            tx.success();
//        } catch (Exception e) {
//            LOG.error("evaluate(): calculating PageRank failed with QueryExecutionException: " + e.getMessage());
//            return false;
//        }
//
//        // First: merge neighboring words into phrases
//        long prev_eP = -1000;
//        String lang = "";
//        String keyword = "";
//        Map<String, Integer> results = new HashMap<>();
//        Map<String, Double> keywords = new HashMap<>();
//        while (res != null && res.hasNext()) {
//            Map<String, Object> next = res.next();
//            int sP = (int) next.get("sP");
//            int eP = (int) next.get("eP");
//            Double score = (Double) next.get("score");
//            //LOG.debug(tag + "\t" + sP + "\t" + eP + "\t" + score);
//
//            String tag = (String) next.get("tag");
//            String tag_val = tag.split("_")[0];
//            if (tag.split("_").length > 2) {
//                LOG.warn("Tag " + tag + " has more than 1 underscore symbols");
//            }
//            if (removeStopWords && stopWords.stream().anyMatch(str -> str.equals(tag_val))) {
//                continue;
//            }
//
//            if (sP - prev_eP <= 1) {
//                keyword += " " + tag_val;
//            } else {
//                if (keyword.split(" ").length > 1) {
//                    if (results.containsKey(keyword + "_" + lang)) {
//                        results.put(keyword + "_" + lang, results.get(keyword + "_" + lang) + 1);
//                    } else {
//                        results.put(keyword + "_" + lang, 1);
//                    }
//                }
//                keyword = tag_val;
//                lang = tag.split("_")[1];
//            }
//            prev_eP = eP;
//            keywords.put(tag, score);
//        }
//
//        // Next: include into final result simply top-x single words
//        LOG.debug("--- Keywords ---");
//        keywords.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
//                .limit(8)
//                .forEach(en -> {
//                    results.put(en.getKey(), 1);
//                    LOG.debug(en.getKey());
//                });
//
//        // prepare query for storing results into the graph
//        /*String query = "MATCH (a:AnnotatedText) WHERE a.id = {id}\n WITH a\n";
//        int c = 1;
//        for (String key: results.keySet()) {
//            if (key.split("_").length>2)
//                LOG.warn("Tag " + key + " has more than 1 underscore symbols, newly created Keyword node might be wrong");
//            String val = key.split("_")[0];
//            if (removeStopWords && stopWords.stream().anyMatch(str -> str.equals(val)))
//                continue;
//            query += "MERGE (k" + c + ":Keyword {id: \"" + key + "\", value: \"" + val + "\"})\n";
//            query += "MERGE (k" + c + ")-[r" + c + ":DESCRIBES {count: " + results.get(key) + "}]->(a)\nON CREATE SET r" + c + ".count=" + results.get(key) + " ON MATCH SET r" + c + ".count=" + results.get(key);
//            c++;
//        }
//
//        // store results into the graph
//        try (Transaction tx = database.beginTx();) {
//            LOG.debug("Query for storing results: \n" + query);
//            database.execute(query, params);
//            tx.success();
//        } catch (Exception e) {
//            LOG.error("evaluate(): storing results into graph failed with QueryExecutionException: " + e.getMessage());
//            return false;
//        }*/
//        ResourceIterator<Node> findNodes = database.findNodes(AnnotatedText, "id", annotatedID);
//        Node ann;
//        if (findNodes.hasNext()) {
//            ann = findNodes.next();
//        } else {
//            LOG.error("Error. AnnotatedText node with id " + annotatedID + " not found, aborting saving results into the graph.");
//            return false;
//        }
//
//        try (Transaction tx = database.beginTx();) {
//            for (String key : results.keySet()) {
//                if (key.split("_").length > 2) {
//                    LOG.warn("Tag " + key + " has more than 1 underscore symbols, newly created Keyword node might be wrong");
//                }
//                String val = key.split("_")[0];
//                if (removeStopWords && stopWords.stream().anyMatch(str -> str.equals(val))) {
//                    continue;
//                }
//
//                Node newNode;
//                findNodes = database.findNodes(Keyword, "id", key);
//                if (findNodes.hasNext()) {
//                    newNode = findNodes.next();
//                } else {
//                    newNode = database.createNode(Keyword);
//                    newNode.setProperty("id", key);
//                    newNode.setProperty("value", val);
//                }
//
//                if (newNode != null) {
//                    Relationship rel = newNode.createRelationshipTo(ann, DESCRIBES);
//                    rel.setProperty("count", results.get(key));
//                }
//            }
//            tx.success();
//        }
//
//        return true;
//    }
}
