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

import com.graphaware.nlp.ml.queue.SimilarityItem;
import com.graphaware.nlp.ml.queue.SimilarityItemProcessEntry;
import com.graphaware.nlp.ml.queue.SimilarityQueueProcessor;
import com.graphaware.nlp.persistence.constants.Labels;
import com.graphaware.nlp.util.FixedSizeOrderedList;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.graphaware.nlp.vector.SparseVector;
import java.util.concurrent.Executors;

public class VectorProcessLogic {

    private static final Logger LOG = LoggerFactory.getLogger(VectorProcessLogic.class);

    protected final Similarity similarityFunction;
    protected final GraphDatabaseService database;

    protected final SimilarityQueueProcessor queueProcessor;

    public VectorProcessLogic(GraphDatabaseService database) {
        this.similarityFunction = new CosineSimilarity();
        this.queueProcessor = new SimilarityQueueProcessor(database);
        this.database = database;
    }

    public void start() {
        Executors.newSingleThreadExecutor().execute(queueProcessor);
    }

    private List<Float> getVector(Node node, String propertyName) {
        float[] vector = (float[]) node.getProperty(propertyName);
        List<Float> result = new ArrayList<>();
        for (int i = 0; i < vector.length; i++) {
            result.add(vector[i]);
        }
        return result;
    }

    public int computeFeatureSimilarityForNodes(List<Node> nodes, String propertyName, String similarityType, int kSize) {
        long startTime = System.currentTimeMillis();
        final AtomicInteger countProcessed = new AtomicInteger(0);
        final AtomicInteger countStored = new AtomicInteger(0);
        final AtomicInteger nodeAnalyzed = new AtomicInteger(0);
        if (nodes == null) {
            nodes = new ArrayList<>();
            ResourceIterator<Node> properties = database.findNodes(Labels.AnnotatedText);
            while (properties.hasNext()) {
                nodes.add(properties.next());
            }
        }
        int totalNodeSize = nodes.size();
        nodes.parallelStream().forEach((node) -> {
            int nodeProcessed = nodeAnalyzed.incrementAndGet();
            if (nodeProcessed % 500 == 0) {
                LOG.warn("Node Processed: " + nodeProcessed + " over " + totalNodeSize);
            }
            computeFeatureSimilarityForNode(node, propertyName, similarityType, countProcessed, countStored, kSize);
        });
        long totalTime = System.currentTimeMillis() - startTime;
        LOG.warn("Total node processed: " + nodeAnalyzed.get() + " over " + totalNodeSize + " in " + totalTime);
        LOG.warn("Total relationships computed: " + countProcessed.get() + " stored: " + countStored.get());
        return countProcessed.get();
    }

    private void computeFeatureSimilarityForNode(Node node, String propertyName, String similarityType, AtomicInteger countProcessed, AtomicInteger countStored, int kSize) {
        FixedSizeOrderedList<SimilarityItem> kNN = new FixedSizeOrderedList<>(kSize);
        try (Transaction tx0 = database.beginTx()) {
            ResourceIterator<Node> otherProperties = database.findNodes(Labels.AnnotatedText);
            List<Node> secondNodes = new ArrayList<>();
            otherProperties.stream().forEach((secondNode) -> {
                secondNodes.add(secondNode);
            });
            secondNodes.stream()
                    .forEach((secondNode) -> {
                        if (secondNode.getId() != node.getId()) {
                            float similarity = getSimilarity(getVector(node, propertyName), getVector(secondNode, propertyName));
                            if (similarity > 0) {
                                kNN.add(new SimilarityItem(node.getId(), secondNode.getId(), similarity, similarityType));
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
        queueProcessor.offer(new SimilarityItemProcessEntry(node.getId(), kNN));
    }

    public static float getSimilarity(List<Float> x, List<Float> y) {
        SparseVector xVector = SparseVector.fromList(x);
        SparseVector yVector = SparseVector.fromList(y);
        float a = xVector.dot(yVector);
        float b = xVector.norm() * yVector.norm();
        if (b > 0) {
            return a / b;
        } else {
            return 0f;
        }
    }
}
