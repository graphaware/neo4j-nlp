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
package com.graphaware.nlp.ml.queue;

import com.graphaware.nlp.persistence.constants.Properties;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SimilarityQueueProcessor implements Runnable {

    private final BlockingQueue<SimilarityItemProcessEntry> queue;
    private final GraphDatabaseService database;
    private static final Logger LOG = LoggerFactory.getLogger(SimilarityQueueProcessor.class);

    public SimilarityQueueProcessor(GraphDatabaseService database) {
        this.queue = new LinkedBlockingQueue<>();
        this.database = database;
    }

    @Override
    public void run() {
        while (true) {
            try {
                List<SimilarityItemProcessEntry> items = new ArrayList<>();
                items.add(queue.take());
                queue.drainTo(items, 99);
                LOG.warn("Processing: " + items.size() + " over " + queue.size() + " element at " + System.currentTimeMillis());
                try (Transaction tx = database.beginTx()) {
                    items.stream().filter((item) -> (item.getkNN() != null && item.getkNN().size() > 0))
                            .forEach((item) -> {
                                clearCurrentSimilarity(item);
                                createRelationship(item);
                            });
                    tx.success();
                } catch (Exception ex) {
                    LOG.error("Errors occur during storing of similarity data. Reenqueing ...", ex);
                    items.stream().filter((item) -> (item.getkNN() != null && item.getkNN().size() > 0))
                            .forEach((item) -> {
                                queue.offer(item);
                            });
                }
                LOG.warn("Completed processing : " + items.size() + " element at " + System.currentTimeMillis());

            } catch (InterruptedException ex) {
                LOG.error("Error while getting elements from queue", ex);
            } catch (Exception ex) {
                LOG.error("Error while processing elements from queue", ex);
            }
        }
    }

    private void createRelationship(SimilarityItemProcessEntry item) throws QueryExecutionException {
        final RelationshipType simType = RelationshipType.withName(item.getkNN().get(0).getSimilarityType());
        Node node = database.getNodeById(item.getNodeId());
        item.getkNN().stream().forEach((simItem) -> {
            Relationship simRel = node.createRelationshipTo(database.getNodeById(simItem.getSecondNode()), simType);
            simRel.setProperty(Properties.SIMILARITY_VALUE, simItem.getSimilarity());
        });
    }

    private void clearCurrentSimilarity(SimilarityItemProcessEntry item) {
        String similartyType = item.getkNN().get(0).getSimilarityType();
        Map<String, Object> params = new HashMap<>();
        params.put("id", item.getNodeId());
        database.execute("MATCH (input:AnnotatedText)-[r:" + similartyType + "]->() WHERE id(input) = {id} DELETE r", params);
    }

    public void offer(SimilarityItemProcessEntry similarityItemProcessEntry) {
        queue.offer(similarityItemProcessEntry);
    }

}
