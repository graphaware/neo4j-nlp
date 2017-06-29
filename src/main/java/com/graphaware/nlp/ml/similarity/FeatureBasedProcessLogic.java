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
package com.graphaware.nlp.ml.similarity;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import static com.graphaware.nlp.domain.Constants.KNN_SIZE;
import com.graphaware.nlp.domain.Labels;
import com.graphaware.nlp.domain.Relationships;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FeatureBasedProcessLogic {

    private static final Logger LOG = LoggerFactory.getLogger(FeatureBasedProcessLogic.class);
    
    private final static String DEFAULT_VECTOR_QUERY = "MATCH (doc:AnnotatedText)\n"
                + "WITH count(doc) as documentsCount\n"
                + "MATCH (input:AnnotatedText)-[:CONTAINS_SENTENCE]->(s:Sentence)-[ht:HAS_TAG]->(tag:Tag)\n"
                + "WHERE id(input) = {id}\n"
                + "MATCH (tag)<-[:HAS_TAG]-(:Sentence)<-[:CONTAINS_SENTENCE]-(document:AnnotatedText)\n"
                + "WITH tag, ht.tf as tf, count(distinct document) as documentsCountForTag, documentsCount\n"
                + "RETURN distinct id(tag) as tagId, sum(tf) as tf, (1.0f + 1.0f*documentsCount)/documentsCountForTag as idf";

    protected final Similarity similarityFunction;
    protected final GraphDatabaseService database;

    @Autowired
    protected SimilarityQueueProcessor queueProcessor;

    @Autowired
    public FeatureBasedProcessLogic(GraphDatabaseService database) {
        this.similarityFunction = new CosineSimilarity();
        this.database = database;
    }

    private final Cache<Long, Map<Long, Float>> tfCache
            = CacheBuilder.newBuilder().maximumSize(10000).expireAfterAccess(30, TimeUnit.MINUTES).build();

    public float getFeatureCosine(long firstNode, long secondNode, String query) {
        return similarityFunction.getSimilarity(getTFMap(firstNode, query), getTFMap(secondNode, query));
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

    private Map<Long, Float> createFeatureMap(long firstNode, String query) throws QueryExecutionException {
        Map<String, Object> params = new HashMap<>();
        params.put("id", firstNode);
        Result res = database.execute(query, params);
        Map<Long, Float> result = new HashMap<>();
        while (res != null && res.hasNext()) {
            Map<String, Object> next = res.next();
            long id = (long) next.get("tagId");
            float tf = getFloatValue(next.get("tf"));
            float idf = Double.valueOf(Math.log(Float.valueOf(getFloatValue(next.get("idf"))).doubleValue())).floatValue();
            result.put(id, tf*idf);
        }
        return result;
    }

    public int computeFeatureSimilarityForNodes(List<Long> firstNodeIds) {
        return computeFeatureSimilarityForNodes(firstNodeIds, DEFAULT_VECTOR_QUERY, Relationships.SIMILARITY_COSINE.name());
    }
    
    public int computeFeatureSimilarityForNodes(List<Long> firstNodeIds, String query, String similarityType) {
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
            computeFeatureSimilarityForNode(firstNode, query, similarityType, countProcessed, countStored);
        });
        tfCache.invalidateAll();
        long totalTime = System.currentTimeMillis() - startTime;
        LOG.warn("Total node processed: " + nodeAnalyzed.get() + " over " + totalNodeSize + " in " + totalTime);
        LOG.warn("Total relationships computed: " + countProcessed.get() + " stored: " + countStored.get());
        return countProcessed.get();
    }

    private void computeFeatureSimilarityForNode(long firstNodeId, String query, String similarityType, AtomicInteger countProcessed, AtomicInteger countStored) {
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
                            float similarity = getFeatureCosine(firstNodeId, secondNode, query);
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
