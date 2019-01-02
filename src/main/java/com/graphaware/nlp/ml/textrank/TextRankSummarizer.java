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

import static com.graphaware.nlp.util.TypeConverter.*;

import com.graphaware.nlp.ml.pagerank.CoOccurrenceItem;
import com.graphaware.nlp.ml.pagerank.PageRank;
import com.graphaware.nlp.summatization.Summarizer;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import com.graphaware.common.log.LoggerFactory;

import java.util.*;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class TextRankSummarizer implements Summarizer {

    private static final List<String> DEFAULT_ADMITTED_POS = Arrays.asList("NN", "NNS", "NNP", "NNPS", "VB", "VBG", "VBD", "VBN", "VBP", "VBZ", "JJ", "JJR", "JJS");
    private static final List<String> DEFAULT_FORBIDDEN_POS = Arrays.asList("CC", "DT", "EX", "IN", "LS", "MD", "PDT", "PRP", "PRP$", "RBR", "RBS", "TO", "UH", "WDT", "WP", "WP$", "WRB");
    private static final Set<String> DEFAULT_STOP_WORDS_MEDIUM = new HashSet<>(Arrays.asList("now", "later", "least", "well", "always", "new", "old", "good", "better", "best", "great", "bad", "worse", "worst", "much", "more", "less", "several", "larger", "smaller", "big", "lower", "widely", "highly", "many", "few", "with", "without", "via", "therefore", "furthermore", "whose", "whether", "though", "although", "to", "not", "of", "prior", "instead", "upon", "every", "together", "across", "toward", "towards", "since", "around", "along", "onto", "into", "already", "whilst", "while", "than", "then", "anyway", "whole", "thus", "throughout", "through", "during", "above", "below", "use", "due", "do", "be", "have", "got", "might", "may", "shall", "can", "could", "would", "will", "such", "like", "other", "another", "far", "away"));
    private static final int DEFAULT_ITERATIONS = 30;
    private static final double DEFAULT_DUMPING_FACTOR = 0.85d;
    private static final double DEFAULT_THRESHOLD = 0.0001d;

    private static final Log LOG = LoggerFactory.getLogger(TextRank.class);
    public static final String TEXT_RANK_SUMMARIZER = "TextRank";

    public static final String PARAMETER_STOP_WORDS = "stopWords";
    public static final String PARAMETER_ADMITTED_POSS = "admittedPOSs";
    public static final String PARAMETER_FORBIDDEN_POSS = "forbiddenPOSs";
    public static final String PARAMETER_ANNOTATED_NODE = "annotatedNode";
    public static final String PARAMETER_ITERATIONS = "iter";
    public static final String PARAMETER_DAMP = "damp";
    public static final String PARAMETER_THRESHOLD = "threshold";

    private GraphDatabaseService database;


    public TextRankSummarizer() {
    }

    @Override
    public String getType() {
        return TEXT_RANK_SUMMARIZER;
    }

    @Override
    public void setDatabase(GraphDatabaseService database) {
        this.database = database;
    }

    public boolean evaluate(Map<String, Object> params) {
        Label keywordLabel;
        Node annotatedText = (Node) params.getOrDefault(PARAMETER_ANNOTATED_NODE, null);
        if (annotatedText == null) {
            throw new RuntimeException("Missing parameter " + PARAMETER_ANNOTATED_NODE);
        }
        Set<String> stopWords = (Set<String>) params.getOrDefault(PARAMETER_STOP_WORDS, DEFAULT_STOP_WORDS_MEDIUM);
        List<String> admittedPOSs = (List<String>) params.getOrDefault(PARAMETER_ADMITTED_POSS, DEFAULT_ADMITTED_POS);
        List<String> forbiddenPOSs = (List<String>) params.getOrDefault(PARAMETER_FORBIDDEN_POSS, DEFAULT_FORBIDDEN_POS);
        int iter = getIntegerValue(params.getOrDefault(PARAMETER_ITERATIONS, DEFAULT_ITERATIONS));
        double damp = getDoubleValue(params.getOrDefault(PARAMETER_DAMP, DEFAULT_DUMPING_FACTOR));
        double threshold = getDoubleValue(params.getOrDefault(PARAMETER_THRESHOLD, DEFAULT_THRESHOLD));
        return evaluate(annotatedText, iter, damp, threshold, stopWords, admittedPOSs, forbiddenPOSs);
    }

    private Map<Long, Map<Long, CoOccurrenceItem>> createGraph(Node annotatedText,
                                                               Set<String> stopWords,
                                                               List<String> admittedPOSs,
                                                               List<String> forbiddenPOSs) {
        String query =
                "match (a:AnnotatedText)-[:CONTAINS_SENTENCE]->(s:Sentence)\n"
                        + "where id(a) = {id}\n"
                        + "match (s)-[:HAS_TAG]->(t:Tag)\n"
                        + "where size(t.value) > 2 AND NOT(toLower(t.value) IN {stopwords}) AND ANY(pos IN t.pos WHERE pos in {admittedPOSs})\n"
                        //+ "where size(t.value) > 2 AND NOT(toLower(t.value) IN {stopwords}) AND NOT ANY(pos IN t.pos WHERE pos IN {forbiddenPOSs})\n"
                        + "return s.sentenceNumber as sentenceNumber, collect(id(t)) as tags\n"
                        + "order by sentenceNumber";

        Map<String, Object> params = new HashMap<>();
        params.put("id", annotatedText.getId());
        params.put("stopwords", stopWords);
        params.put("admittedPOSs", admittedPOSs);
        params.put("forbiddenPOSs", forbiddenPOSs);

        Result res = null;
        try (Transaction tx = database.beginTx();) {
            res = database.execute(query, params);
            tx.success();
        } catch (Exception e) {
            LOG.error("Error while creating co-occurrences: ", e);
        }

        Map<Long, Set<Long>> sentences = new HashMap<>();
        while (res != null && res.hasNext()) {
            Map<String, Object> next = res.next();
            Long num = toLong(next.get("sentenceNumber"));
            Set<Long> tags = iterableToSet((Iterable<Long>) next.get("tags"));
            sentences.put(num, tags);
        }

        //System.out.println("\n >> Graph for AnnotatedText " + annotatedText.getId() + ":");
        Map<Long, Map<Long, CoOccurrenceItem>> results = new HashMap<>();
        sentences.entrySet().stream()
                .forEach(entry -> {
                    sentences.entrySet().stream()
                            .filter(en -> en.getKey().longValue() > entry.getKey().longValue()) // similarity between two sentences is commutative (no need to calculate it twice)
                            .forEach(innerEntry -> {
                                Set<Long> tags = new HashSet<>(entry.getValue());
                                tags.retainAll(innerEntry.getValue());
                                int n = tags.size();
                                double denom = Math.log(1.0d * innerEntry.getValue().size()) + Math.log(1.0d * entry.getValue().size());
                                //double denom = innerEntry.getValue().size() + entry.getValue().size();
                                //double denom = entry.getValue().size() * innerEntry.getValue().size(); // for cosine similarity
                                if (n > 0 && denom > 0) {
                                    double val = n / denom;
                                    //double val = n / Math.sqrt(denom); // cosine similarity
                                    addToCoOccurrence(results, entry.getKey(), innerEntry.getKey(), val);
                                    addToCoOccurrence(results, innerEntry.getKey(), entry.getKey(), val); // needed because we want an undirected PageRank
                                    //System.out.println(" " + entry.getKey() + " -> " + innerEntry.getKey() + ": " + val);
                                }
                            });
                });

        return results;
    }

    private void addToCoOccurrence(Map<Long, Map<Long, CoOccurrenceItem>> results, Long source, Long destination, double w) {
        Map<Long, CoOccurrenceItem> mapTag1;
        if (!results.containsKey(source)) {
            mapTag1 = new HashMap<>();
            results.put(source, mapTag1);
        } else {
            mapTag1 = results.get(source);
        }

        if (mapTag1.containsKey(destination)) {
            CoOccurrenceItem ccEntry = mapTag1.get(destination);
            ccEntry.setCount(w);
        } else {
            mapTag1.put(destination, new CoOccurrenceItem(source, 0, destination, 0));
            mapTag1.get(destination).setCount(w);
        }
    }


    private boolean evaluate(Node annotatedText,
                             int iter,
                             double damp,
                             double threshold,
                             Set<String> stopWords,
                             List<String> admittedPOSs,
                             List<String> forbiddenPOSs) {
        Map<Long, Map<Long, CoOccurrenceItem>> coOccurrence = createGraph(annotatedText, stopWords, admittedPOSs, forbiddenPOSs);
        if (coOccurrence == null || coOccurrence.isEmpty()) {
            LOG.info("Graph of co-occurrences is empty, aborting ...");
            return true;
        }

        PageRank pageRank = new PageRank(database);
        Map<Long, Double> pageRanks = pageRank.run(coOccurrence, iter, damp, threshold);

        if (pageRanks == null) {
            LOG.error("Page ranks not retrieved, aborting evaluate() method ...");
            return false;
        }

        System.out.println("\n >> Ranked sentences:");
        AtomicReference<Integer> currOrder = new AtomicReference<>(1);
        AtomicReference<String> saveQuery = new AtomicReference<>("MATCH (a:AnnotatedText) WHERE id(a) = {id}\n");
        pageRanks.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach(en -> {
                    System.out.println("  " + en.getKey() + ": " + en.getValue());
                    saveQuery.set(saveQuery.get() + "WITH a\n"
                            + "MATCH (a)-[:CONTAINS_SENTENCE]->(s:Sentence {sentenceNumber: " + en.getKey() + "})\n"
                            + "SET s.summaryRank = " + currOrder.get() + ", s.summaryRelevance = " + en.getValue() + "\n");
                    currOrder.set(currOrder.get() + 1);
                });

        // Save results
        //System.out.println(saveQuery.get());
        Map<String, Object> params = new HashMap<>();
        params.put("id", annotatedText.getId());
        try (Transaction tx = database.beginTx();) {
            database.execute(saveQuery.get(), params);
            tx.success();
        } catch (Exception e) {
            LOG.error("Error while saving results: ", e);
        }

        return true;
    }

    public Set<String> getStopwords(Map<String, Object> params) {
        Object stopWords = params.get(PARAMETER_STOP_WORDS);
        if (stopWords == null) {
            return DEFAULT_STOP_WORDS_MEDIUM;
        }
        if (stopWords instanceof Collection) {
            return new HashSet<>((Collection)stopWords);
        } else if (stopWords instanceof String) {
            return getStopwords((String)stopWords);
        } else {
            throw new RuntimeException("The stopWords parameter is of an unsupported type: " + stopWords.getClass() + ". Supported types are: String or Collection");
        }
    }

    public Set<String> getStopwords(String stopwords) {
        Set<String> stopWords = new HashSet<>();
        if (stopwords.split(",").length > 0 && stopwords.split(",")[0].equals("+")) { // if the stopwords list starts with "+,....", append the list to the default 'stopWords' set
            stopWords.addAll(DEFAULT_STOP_WORDS_MEDIUM);
            stopWords.addAll(Arrays.asList(stopwords.split(",")).stream().filter(str -> !str.equals("+")).map(str -> str.trim().toLowerCase()).collect(Collectors.toSet()));
        } else {
            stopWords = Arrays.asList(stopwords.split(",")).stream().map(str -> str.trim().toLowerCase()).collect(Collectors.toSet());
        }
        return stopWords;
    }


}
