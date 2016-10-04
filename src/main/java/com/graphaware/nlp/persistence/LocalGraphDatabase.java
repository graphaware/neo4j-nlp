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
package com.graphaware.nlp.persistence;

import com.graphaware.nlp.domain.Persistable;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.Transaction;

public class LocalGraphDatabase implements GraphPersistence {

    private final GraphDatabaseService database;

    public LocalGraphDatabase(GraphDatabaseService database) {
        this.database = database;
    }

    @Override
    public void persistOnGraph(Persistable text, boolean force) {
        try (Transaction tx = database.beginTx()) {
            text.storeOnGraph(database, force);
            tx.success();
        } catch (QueryExecutionException ex) {
            throw new RuntimeException("Error while persisting", ex);
        }
    }
}
