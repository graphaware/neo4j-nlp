package com.graphaware.nlp.queue;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import com.graphaware.nlp.domain.Properties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
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
public class SimilarityQueueProcessor implements Runnable {

    private final BlockingQueue<SimilarityItemProcessEntry> queue;
    private final GraphDatabaseService database;
    private static final Logger LOG = LoggerFactory.getLogger(SimilarityQueueProcessor.class);

    @Autowired
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
