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
package com.graphaware.nlp.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.graphaware.common.kv.GraphKeyValueStore;
import com.graphaware.nlp.configuration.DynamicConfiguration;
import org.neo4j.graphdb.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractPersister {

    private static final String KV_CONFIGURATION_KEY = "GA_NLP_CONFIGURATION";

    protected final GraphDatabaseService database;

    private final DynamicConfiguration configuration;

    private final ObjectMapper mapper = new ObjectMapper();

    public AbstractPersister(GraphDatabaseService database, DynamicConfiguration dynamicConfiguration) {
        this.database = database;
        this.configuration = dynamicConfiguration;
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }
//
//    public void updateConfiguration(Map<String, Object> configuration) {
//        try (Transaction tx = getDatabase().beginTx()) {
//            storeUserConfiguration(configuration);
//            persistenceConfiguration.update(configuration);
//
//            tx.success();
//        }
//    }
//
//    public void updateConfigurationSetting(String key, Object value) {
//        try (Transaction tx = getDatabase().beginTx()) {
//            Map<String, Object> config = persistenceConfiguration.updateSetting(key, value);
//            storeUserConfiguration(config);
//            tx.success();
//        }
//    }

    protected GraphDatabaseService getDatabase() {
        return database;
    }

    protected DynamicConfiguration configuration() {
        return configuration;
    }

    protected ObjectMapper mapper() {
        return mapper;
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
