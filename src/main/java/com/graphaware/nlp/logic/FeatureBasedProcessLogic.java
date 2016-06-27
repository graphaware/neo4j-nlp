/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.logic;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.graphaware.nlp.domain.AnnotatedText;
import static com.graphaware.nlp.domain.Constants.KNN_SIZE;
import com.graphaware.nlp.domain.Labels;
import com.graphaware.nlp.queue.SimilarityItemProcessEntry;
import com.graphaware.nlp.queue.SimilarityItem;
import com.graphaware.nlp.queue.SimilarityQueueProcessor;
import com.graphaware.nlp.util.FixedSizeOrderedList;
import com.graphaware.nlp.util.similarity.CosineSimilarity;
import com.graphaware.nlp.util.similarity.Similarity;
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

/**
 *
 * @author alessandro@graphaware.com
 */
@Component
public class FeatureBasedProcessLogic {

    private static final Logger LOG = LoggerFactory.getLogger(FeatureBasedProcessLogic.class);

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

    public float getFeatureCosine(long firstNode, long secondNode) {
        return similarityFunction.getSimilarity(getTFMap(firstNode), getTFMap(secondNode));
    }

    private Map<Long, Float> getTFMap(long node) throws QueryExecutionException {
        Map<Long, Float> tfMap = tfCache.getIfPresent(node);
        if (tfMap != null) {
            return tfMap;
        }
        tfMap = createFeatureMap(node);
        tfCache.put(node, tfMap);
        return tfMap;
    }

    private Map<Long, Float> createFeatureMap(long firstNode) throws QueryExecutionException {
        Map<String, Object> params = new HashMap<>();
        params.put("id", firstNode);
        Result res = database.execute("MATCH (doc:AnnotatedText)\n"
                + "WITH count(doc) as documentsCount\n"
                + "MATCH (input:AnnotatedText)-[:CONTAINS_SENTENCE]->(s:Sentence)-[ht:HAS_TAG]->(tag:Tag)\n"
                + "WHERE id(input) = {id}\n"
                + "MATCH (tag)<-[:HAS_TAG]-(:Sentence)<-[:CONTAINS_SENTENCE]-(document:AnnotatedText)\n"
                + "WITH tag, ht.tf as tf, count(distinct document) as documentsCountForTag, documentsCount\n"
                + "RETURN distinct id(tag) as tagId, sum(tf) as tf, (1.0f*documentsCount)/documentsCountForTag as idf", params);
        Map<Long, Float> result = new HashMap<>();
        while (res != null && res.hasNext()) {
            Map<String, Object> next = res.next();
            long id = (long) next.get("tagId");
            float tf = getFloatValue(next.get("tf"));
            float idf = Double.valueOf(Math.log(1f + Float.valueOf(getFloatValue(next.get("idf"))).doubleValue())).floatValue();
            result.put(id, tf*idf);
        }
        return result;
    }

    public int computeFeatureSimilarityForNodes(List<Long> firstNodeIds) {
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
            computeFeatureSimilarityForNode(firstNode, countProcessed, countStored);
        });
        long totalTime = System.currentTimeMillis() - startTime;
        LOG.warn("Total node processed: " + nodeAnalyzed.get() + " over " + totalNodeSize + " in " + totalTime);
        LOG.warn("Total relationships computed: " + countProcessed.get() + " stored: " + countStored.get());
        return countProcessed.get();
    }

    private void computeFeatureSimilarityForNode(long firstNodeId, AtomicInteger countProcessed, AtomicInteger countStored) {
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
                            float similarity = getFeatureCosine(firstNodeId, secondNode);
                            if (similarity > 0) {
                                kNN.add(new SimilarityItem(firstNodeId, secondNode, similarity, "SIMILARITY_COSINE"));
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
