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
import com.graphaware.nlp.domain.TfIdfObject;
import com.graphaware.nlp.persistence.constants.Labels;
import com.graphaware.nlp.persistence.persisters.KeywordPersister;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.graphaware.nlp.persistence.constants.Relationships.DESCRIBES;
import java.util.concurrent.atomic.AtomicReference;

public class TextRankSummarizer {

    private static final Logger LOG = LoggerFactory.getLogger(TextRank.class);

    private final GraphDatabaseService database;
    private final Label keywordLabel;
    private final Set<String> stopWords;
    private final List<String> admittedPOSs;
    private final List<String> forbiddenPOSs;

    public TextRankSummarizer(GraphDatabaseService database,
            Label keywordLabel,
            List<String> admittedPOSs,
            List<String> forbiddenPOSs,
            Set<String> stopWords) {
        this.database = database;
        this.keywordLabel = keywordLabel;
        this.stopWords = stopWords;
        this.admittedPOSs = admittedPOSs;
        this.forbiddenPOSs = forbiddenPOSs;
    }

    private Map<Long, Map<Long, CoOccurrenceItem>> createGraph(Node annotatedText) {
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

    public boolean evaluate(Node annotatedText, int iter, double damp, double threshold) {
        Map<Long, Map<Long, CoOccurrenceItem>> coOccurrence = createGraph(annotatedText);
        if (coOccurrence == null || coOccurrence.size() == 0) {
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
                //.limit(x)
                //.map((item) -> item.getKey())
                //.collect(Collectors.toList());
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

    private <T> Set<T> iterableToSet(Iterable<T> it) {
        Set<T> newList = new HashSet<>();
        for (T obj : it) {
            newList.add(obj);
        }
        return newList;
    }


    public static class Builder {

        //private static final String[] ADMITTED_POS = {"NN", "NNS", "NNP", "NNPS", "JJ", "JJR", "JJS"};
        //private static final String[] ADMITTED_POS = {"NN", "NNS", "NNP", "NNPS", "VB", "VBG", "VBD", "VBN", "VBP", "VBZ"};
        private static final String[] ADMITTED_POS = {"NN", "NNS", "NNP", "NNPS", "VB", "VBG", "VBD", "VBN", "VBP", "VBZ", "JJ", "JJR", "JJS"};
        private static final String[] FORBIDDEN_POS = {"CC", "DT", "EX", "IN", "LS", "MD", "PDT", "PRP", "PRP$", "RBR", "RBS", "TO", "UH", "WDT", "WP", "WP$", "WRB"};
        private static final String[] STOP_WORDS_MEDIUM = {"now", "later", "least", "well", "always", "new", "old", "good", "better", "best", "great", "bad", "worse", "worst", "much", "more", "less", "several", "larger", "smaller", "big", "lower", "widely", "highly", "many", "few", "with", "without", "via", "therefore", "furthermore", "whose", "whether", "though", "although", "to", "not", "of", "prior", "instead", "upon", "every", "together", "across", "toward", "towards", "since", "around", "along", "onto", "into", "already", "whilst", "while", "than", "then", "anyway", "whole", "thus", "throughout", "through", "during", "above", "below", "use", "due", "do", "be", "have", "got", "might", "may", "shall", "can", "could", "would", "will", "such", "like", "other", "another", "far", "away"};

        private final GraphDatabaseService database;
        private Label keywordLabel;
        private List<String> admittedPOSs = Arrays.asList(ADMITTED_POS);
        private List<String> forbiddenPOSs = Arrays.asList(FORBIDDEN_POS);
        private Set<String> stopWords = new HashSet<>(Arrays.asList(STOP_WORDS_MEDIUM));

        public Builder(GraphDatabaseService database, DynamicConfiguration configuration) {
            this.database = database;
            this.keywordLabel = configuration.getLabelFor(Labels.Keyword);
        }

        public TextRankSummarizer build() {
            TextRankSummarizer result = new TextRankSummarizer(database,
                    keywordLabel,
                    admittedPOSs,
                    forbiddenPOSs,
                    stopWords);
            return result;
        }

        public Builder setStopwords(String stopwords) {
            if (stopwords.split(",").length > 0 && stopwords.split(",")[0].equals("+")) { // if the stopwords list starts with "+,....", append the list to the default 'stopWords' set
                this.stopWords.addAll(Arrays.asList(stopwords.split(",")).stream().filter(str -> !str.equals("+")).map(str -> str.trim().toLowerCase()).collect(Collectors.toSet()));
            } else {
                this.stopWords = Arrays.asList(stopwords.split(",")).stream().map(str -> str.trim().toLowerCase()).collect(Collectors.toSet());
            }
            //this.removeStopWords = true;
            return this;
        }

        public Builder setKeywordLabel(String keywordLabel) {
            this.keywordLabel = Label.label(keywordLabel);
            return this;
        }
    }

}
