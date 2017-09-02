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
package com.graphaware.nlp.ml.similarity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import static com.graphaware.nlp.domain.Constants.KNN_SIZE;

import com.graphaware.nlp.persistence.constants.Labels;
import com.graphaware.nlp.persistence.constants.Relationships;
import com.graphaware.nlp.ml.queue.SimilarityItemProcessEntry;
import com.graphaware.nlp.ml.queue.SimilarityItem;
import com.graphaware.nlp.ml.queue.SimilarityQueueProcessor;
import com.graphaware.nlp.util.FixedSizeOrderedList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureBasedProcessLogic {

    private static final Logger LOG = LoggerFactory.getLogger(FeatureBasedProcessLogic.class);

    private final static String DEFAULT_VECTOR_QUERY = "MATCH (doc:AnnotatedText)\n"
            + "WITH count(doc) as documentsCount\n"
            + "MATCH (input:AnnotatedText)-[:CONTAINS_SENTENCE]->(s:Sentence)-[ht:HAS_TAG]->(tag:Tag)\n"
            + "WHERE id(input) = {id}\n"
            + "MATCH (tag)<-[:HAS_TAG]-(:Sentence)<-[:CONTAINS_SENTENCE]-(document:AnnotatedText)\n"
            + "WITH tag, ht.tf as tf, count(distinct document) as documentsCountForTag, documentsCount\n"
            + "RETURN distinct id(tag) as tagId, sum(tf) as tf, (1.0f + 1.0f*documentsCount)/documentsCountForTag as idf";

    private final static String DEFAULT_VECTOR_QUERY_WITH_CONCEPT = "MATCH (doc:AnnotatedText)\n"
            + "WITH count(doc) as documentsCount\n"
            + "MATCH (document:AnnotatedText)-[:CONTAINS_SENTENCE]->(s:Sentence)-[ht:HAS_TAG]->(tag:Tag)\n"
            + "WHERE id(document) = {id} and not any (p in tag.pos where p in [\"CC\", \"CD\", \"DT\", \"IN\", \"MD\", \"PRP\", \"PRP$\", \"UH\", \"WDT\", \"WP\", \"WRB\", \"TO\", \"PDT\", \"RP\", \"WP$\"])\n" // JJR, JJS ?
            + "WITH tag, sum(ht.tf) as tf, documentsCount, document.numTerms as nTerms\n"
            + "OPTIONAL MATCH (tag)-[rt:IS_RELATED_TO]->(t2_l1:Tag)\n"
            + "WHERE id(t2_l1) = tag.idMaxConcept  and exists(t2_l1.word2vec) and com.graphaware.nlp.ml.similarity.cosine(tag.word2vec, t2_l1.word2vec)>0.2\n"
            + "WITH tag, tf, nTerms, id(t2_l1) as cn5_l1_tag, rt.weight as cn5_l1_tag_w, documentsCount\n"
            + "MATCH (a:AnnotatedText)-[:CONTAINS_SENTENCE]->(s:Sentence)-[ht:HAS_TAG]->(tag)\n"
            + "RETURN id(tag) as tagId, tf, (1.0f*documentsCount)/count(distinct a) as idf, nTerms, (case cn5_l1_tag when null then -1 else cn5_l1_tag end) as cn5_l1_tag, cn5_l1_tag_w\n"
            + "ORDER BY tagId, cn5_l1_tag";
    
    protected final Similarity similarityFunction;
    protected final GraphDatabaseService database;

    protected final SimilarityQueueProcessor queueProcessor;

    public FeatureBasedProcessLogic(GraphDatabaseService database) {
        this.similarityFunction = new CosineSimilarity();
        this.queueProcessor = new SimilarityQueueProcessor(database);
        this.database = database;
    }

    private final Cache<Long, Map<Long, Float>> tfCache
            = CacheBuilder.newBuilder().maximumSize(10000).expireAfterAccess(30, TimeUnit.MINUTES).build();

    public float getFeatureCosine(long firstNode, long secondNode, String query) {
        return similarityFunction.getSimilarity(getTFMap(firstNode, query), getTFMap(secondNode, query));
    }

    public float getFeatureCosine(long firstNode, long secondNode, int depth) {
        return similarityFunction.getSimilarity(getTFMap(firstNode, depth), getTFMap(secondNode, depth));
    }

    private Map<Long, Float> getTFMap(long node, String query) throws QueryExecutionException {
        Map<Long, Float> tfMap = tfCache.getIfPresent(node);
        if (tfMap != null) {
            return tfMap;
        }
        tfMap = createFeatureMap(node, query);
        tfCache.put(node, tfMap);
        return tfMap;
    }

    private Map<Long, Float> getTFMap(long node, int depth) throws QueryExecutionException {
        Map<Long, Float> tfMap = tfCache.getIfPresent(node);
        if (tfMap != null) {
            return tfMap;
        }
        tfMap = createFeatureMapWithCN5New(node);
        tfCache.put(node, tfMap);
        return tfMap;
    }

    private Map<Long, Float> createFeatureMap(long firstNode, String query) throws QueryExecutionException {
        Map<String, Object> params = new HashMap<>();
        params.put("id", firstNode);
        Result res = database.execute(query, params);
        Map<Long, Float> result = new HashMap<>();
        while (res != null && res.hasNext()) {
            Map<String, Object> next = res.next();
            long id = (long) next.get("tagId");
            float tf = getFloatValue(next.get("tf"));
            //int nTerms = (int) next.get("nTerms");
            //float tf = getFloatValue(next.get("tf")) / nTerms; // normalize to document length
            float idf = Double.valueOf(Math.log10(Float.valueOf(getFloatValue(next.get("idf"))).doubleValue())).floatValue();
            result.put(id, tf * idf);
        }
        return result;
    }

    private Map<Long, Float> createFeatureMapWithCN5New(long firstNode) throws QueryExecutionException {
        Map<String, Object> params = new HashMap<>();
        params.put("id", firstNode);
        Result res = database.execute(DEFAULT_VECTOR_QUERY_WITH_CONCEPT, params);
        Map<Long, Float> result = new HashMap<>();
        Map<Long, Float> result_idf = new HashMap<>();
        while (res != null && res.hasNext()) {
            Map<String, Object> next = res.next();
            long id = (long) next.get("tagId");
            int nTerms = (int) next.get("nTerms");
            //float tf = getFloatValue(next.get("tf"));
            float tf = getFloatValue(next.get("tf")) / nTerms;
            float idf = Double.valueOf(Math.log10(Float.valueOf(getFloatValue(next.get("idf"))).doubleValue())).floatValue();

            // ConceptNet5 Level_1 tags
            //long cn5_tag = Long.valueOf((String) next.get("cn5_l1_tag"));
            long cn5_tag = (long) next.get("cn5_l1_tag");
            float cn5_tag_w = getFloatValue(next.get("cn5_l1_tag_w"));

            if (cn5_tag > -1) {
                if (!result.containsKey(cn5_tag)) {
                    result.put(cn5_tag, tf);
                    result_idf.put(cn5_tag, idf);
                } else {
                    result.put(cn5_tag, result.get(cn5_tag) + tf);
                    if (result_idf.get(cn5_tag) < idf) // use the highest idf
                    {
                        result_idf.put(cn5_tag, idf);
                    }
                }
            } else {
                result.put(id, tf);
                result_idf.put(id, idf);
            }
        }

        result.keySet().forEach((key) -> {
            result.put(key, result.get(key) * result_idf.get(key));
        });
        return result;
    }

    public int computeFeatureSimilarityForNodes(List<Long> firstNodeIds) {
        return computeFeatureSimilarityForNodes(firstNodeIds, DEFAULT_VECTOR_QUERY, Relationships.SIMILARITY_COSINE.name(), 0);
    }

    public int computeFeatureSimilarityForNodes(List<Long> firstNodeIds, Integer depth) {
        return computeFeatureSimilarityForNodes(firstNodeIds, DEFAULT_VECTOR_QUERY_WITH_CONCEPT, Relationships.SIMILARITY_COSINE_CN5.name(), depth);
    }

    public int computeFeatureSimilarityForNodes(List<Long> firstNodeIds, String query, String similarityType, int depth) {
        long startTime = System.currentTimeMillis();
        final AtomicInteger countProcessed = new AtomicInteger(0);
        final AtomicInteger countStored = new AtomicInteger(0);
        final AtomicInteger nodeAnalyzed = new AtomicInteger(0);
        if (firstNodeIds == null) {
            firstNodeIds = new ArrayList<>();
            ResourceIterator<Node> properties = database.findNodes(Labels.AnnotatedText);
            while (properties.hasNext()) {
                firstNodeIds.add(properties.next().getId());
            }
        }
        int totalNodeSize = firstNodeIds.size();
        firstNodeIds.parallelStream().forEach((firstNode) -> {
            int nodeProcessed = nodeAnalyzed.incrementAndGet();
            if (nodeProcessed % 500 == 0) {
                LOG.warn("Node Processed: " + nodeProcessed + " over " + totalNodeSize);
            }
            computeFeatureSimilarityForNode(firstNode, query, similarityType, countProcessed, countStored, depth);
        });
        tfCache.invalidateAll();
        long totalTime = System.currentTimeMillis() - startTime;
        LOG.warn("Total node processed: " + nodeAnalyzed.get() + " over " + totalNodeSize + " in " + totalTime);
        LOG.warn("Total relationships computed: " + countProcessed.get() + " stored: " + countStored.get());
        return countProcessed.get();
    }

    private void computeFeatureSimilarityForNode(long firstNodeId, String query, String similarityType, AtomicInteger countProcessed, AtomicInteger countStored, Integer depth) {
        FixedSizeOrderedList<SimilarityItem> kNN = new FixedSizeOrderedList<>(KNN_SIZE);
        try (Transaction tx0 = database.beginTx()) {
            ResourceIterator<Node> otherProperties = database.findNodes(Labels.AnnotatedText);
            List<Long> secondNodeIds = new ArrayList<>();
            otherProperties.stream().forEach((node) -> {
                secondNodeIds.add(node.getId());
            });
            secondNodeIds.stream()
                    .forEach((secondNode) -> {
                        if (secondNode != firstNodeId) {
                            //if (secondNode > firstNodeId) { // this way, only one relationship between the same AnnotatedTexts will be stored (lower_id -> higher_id)
                            float similarity;
                            if (depth != null && depth > 0) {
                                similarity = getFeatureCosine(firstNodeId, secondNode, depth);
                            } else {
                                similarity = getFeatureCosine(firstNodeId, secondNode, query);
                            }

                            if (similarity > 0) {
                                kNN.add(new SimilarityItem(firstNodeId, secondNode, similarity, similarityType));
                                countStored.incrementAndGet();
                            }
                            int processed = countProcessed.incrementAndGet();
                            if (processed % 10000 == 0) {
                                LOG.warn("Relationships computed: " + processed);
                            }
                        }
                    });
            tx0.success();
        }
        queueProcessor.offer(new SimilarityItemProcessEntry(firstNodeId, kNN));
    }

    protected float getFloatValue(Object value) {
        if (value == null) {
            return 1.0f;
        }
        if (value instanceof Double) {
            return ((Double) value).floatValue();
        }
        if (value instanceof Float) {
            return ((Float) value);
        } else {
            try {
                return Float.valueOf(String.valueOf(value));
            } catch (Exception ex) {
                LOG.error("Error while parsing float value from string: " + value, ex);
                return 1.0f;
            }
        }
    }
}
