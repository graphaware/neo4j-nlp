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
package com.graphaware.nlp.persistence.persisters;

import com.graphaware.nlp.NLPManager;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.persistence.PersistenceRegistry;
import org.codehaus.jackson.map.SerializationConfig;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractPersister {

    protected final GraphDatabaseService database;

    protected final NLPManager manager;

    private final PersistenceRegistry registry;

    private final ObjectMapper mapper = new ObjectMapper();

    public AbstractPersister(GraphDatabaseService database, PersistenceRegistry registry) {
        this.database = database;
        this.registry = registry;
        this.manager = NLPManager.getInstance();
        mapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    protected GraphDatabaseService getDatabase() {
        return database;
    }

    protected DynamicConfiguration configuration() {
        return manager.getConfiguration();
    }

    protected ObjectMapper mapper() {
        return mapper;
    }

    protected Persister getPersister(Class clazz) {
        return registry.getPersister(clazz);
    }

    protected Node getIfExist(Label label, String key, Object value) {
        ResourceIterator<Node> nodes = getDatabase().findNodes(label, key, value);
        List<Node> all = new ArrayList<>();
        while (nodes.hasNext()) {
            all.add(nodes.next());
        }

        if (all.size() > 1) {
            throw new RuntimeException("More than one node found");
        }

        return all.isEmpty() ? null : all.get(0);
    }
}
