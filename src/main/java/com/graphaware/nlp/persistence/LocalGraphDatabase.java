/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.persistence;

import com.graphaware.nlp.domain.AnnotatedText;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.Transaction;

public class LocalGraphDatabase implements GraphPersistence {

    private final GraphDatabaseService database;

    public LocalGraphDatabase(GraphDatabaseService database) {
        this.database = database;
    }

    @Override
    public void persistOnGraph(AnnotatedText text) {
        try (Transaction tx = database.beginTx()) {
            text.storeOnGraph(database);
            tx.success();
        } catch (QueryExecutionException ex) {
            throw new RuntimeException("Error while persisting", ex);
        }
    }
}
