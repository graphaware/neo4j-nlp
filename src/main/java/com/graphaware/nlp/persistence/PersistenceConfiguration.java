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

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PersistenceConfiguration {

    private final Map<String, Object> userProvidedConfiguration;

    private PersistenceConfiguration(Map<String, Object> userProvidedConfiguration) {
        this.userProvidedConfiguration = userProvidedConfiguration;
    }

    public static PersistenceConfiguration defaultConfiguration() {
        return new PersistenceConfiguration(new HashMap<>());
    }

    public static PersistenceConfiguration withConfiguration(Map<String, Object> configuration) {
        return new PersistenceConfiguration(configuration);
    }

    public Label getLabelFor(Label label) {
        if (!userProvidedConfiguration.containsKey(label.toString())) {
            return label;
        }

        return Label.label(userProvidedConfiguration.get(label.toString()).toString());
    }

    public RelationshipType getRelationshipFor(RelationshipType relationship) {
        if (!userProvidedConfiguration.containsKey(relationship.name())) {
            return relationship;
        }

        return RelationshipType.withName(userProvidedConfiguration.get(relationship.name()).toString());
    }

    public String getPropertyKeyFor(String key) {
        if (!userProvidedConfiguration.containsKey(key)) {
            return key;
        }

        return userProvidedConfiguration.get(key).toString();
    }

    public void update(Map<String, Object> map) {
        userProvidedConfiguration.clear();
        userProvidedConfiguration.putAll(map);
    }

    public Map<String, Object> updateSetting(String key, Object value) {
        userProvidedConfiguration.put(key, value);

        return userProvidedConfiguration;
    }
}
