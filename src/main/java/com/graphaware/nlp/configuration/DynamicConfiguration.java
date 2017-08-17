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
package com.graphaware.nlp.configuration;

import com.graphaware.common.kv.GraphKeyValueStore;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

import java.util.HashMap;
import java.util.Map;

public class DynamicConfiguration {

    private static final String STORE_KEY = "GA__NLP__";
    private static final String LABEL_KEY_PREFIX = "LABEL_";
    private static final String RELATIONSHIP_TYPE_KEY_PREFIX = "RELATIONSHIP_";
    private static final String PROPERTY_KEY_PREFIX = "PROPERTY_";
    private static final String SETTING_KEY_PREFIX = "SETTING_";
    
    private final GraphDatabaseService database;
    private final GraphKeyValueStore keyValueStore;
    private Map<String, Object> userProvidedConfiguration;

    public DynamicConfiguration(GraphDatabaseService database) {
        this.database = database;
        this.keyValueStore = new GraphKeyValueStore(database);
        loadUserConfiguration();
    }

    public Label getLabelFor(Label label) {
        if (!userProvidedConfiguration.containsKey(LABEL_KEY_PREFIX + label.toString())) {
            return label;
        }

        return Label.label(userProvidedConfiguration.get(LABEL_KEY_PREFIX + label.toString()).toString());
    }

    public RelationshipType getRelationshipFor(RelationshipType relationship) {
        if (!userProvidedConfiguration.containsKey(RELATIONSHIP_TYPE_KEY_PREFIX + relationship.name())) {
            return relationship;
        }

        return RelationshipType.withName(userProvidedConfiguration.get(RELATIONSHIP_TYPE_KEY_PREFIX + relationship.name()).toString());
    }

    public String getPropertyKeyFor(String key) {
        if (!userProvidedConfiguration.containsKey(PROPERTY_KEY_PREFIX + key)) {
            return key;
        }

        return userProvidedConfiguration.get(PROPERTY_KEY_PREFIX + key).toString();
    }

    public Object getSettingValueFor(String key) {
        if (!userProvidedConfiguration.containsKey(SETTING_KEY_PREFIX + key)) {
            return key;
        }

        return userProvidedConfiguration.get(SETTING_KEY_PREFIX + key);
    }

    public void update(String key, Object value) {
        try (Transaction tx = database.beginTx()) {
            keyValueStore.set(STORE_KEY + key, value);
            tx.success();
        }
        loadUserConfiguration();
    }

    public void updateInternalSetting(String key, Object value) {
        try (Transaction tx = database.beginTx()) {
            keyValueStore.set(STORE_KEY + SETTING_KEY_PREFIX + key, value);
            tx.success();
        }
        loadUserConfiguration();
    }

    private void loadUserConfiguration() {
        userProvidedConfiguration = getAllConfigValuesFromStore();
    }

    public Map<String, Object> getAllConfigValuesFromStore() {
        Map<String, Object> map = new HashMap<>();
        try (Transaction tx = database.beginTx()) {
            keyValueStore.getKeys().forEach(k -> {
                if (k.startsWith(STORE_KEY)) {
                    String fkey = k.replace(STORE_KEY, "");
                    map.put(fkey, keyValueStore.get(k));
                }
            });
            tx.success();
        }

        return map;
    }
}
