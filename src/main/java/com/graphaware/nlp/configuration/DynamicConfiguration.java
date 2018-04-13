/*
 * Copyright (c) 2013-2018 GraphAware
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

import org.codehaus.jackson.map.ObjectMapper;
import com.graphaware.common.kv.GraphKeyValueStore;
import com.graphaware.nlp.dsl.request.PipelineSpecification;
import com.graphaware.nlp.dsl.result.WorkflowInstanceItemInfo;
import com.graphaware.nlp.workflow.WorkflowItem;
import java.io.IOException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.codehaus.jackson.annotate.JsonTypeInfo;

public class DynamicConfiguration {

    protected static final String STORE_KEY = "GA__NLP__";
    protected static final String LABEL_KEY_PREFIX = "LABEL_";
    protected static final String RELATIONSHIP_TYPE_KEY_PREFIX = "RELATIONSHIP_";
    protected static final String PROPERTY_KEY_PREFIX = "PROPERTY_";
    protected static final String SETTING_KEY_PREFIX = "SETTING_";
    protected static final String PIPELINE_KEY_PREFIX = "PIPELINE_";
    protected static final String MODEL_KEY_PREFIX = "MODEL_";

    protected final GraphDatabaseService database;
    protected final GraphKeyValueStore keyValueStore;
    protected Map<String, Object> userProvidedConfiguration;
    protected final ObjectMapper mapper = new ObjectMapper();

    public DynamicConfiguration(GraphDatabaseService database) {
        this.database = database;
        this.keyValueStore = new GraphKeyValueStore(database);
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
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

    public boolean hasSettingValue(String key) {
        return userProvidedConfiguration.containsKey(SETTING_KEY_PREFIX + key);
    }

    public boolean hasStoreValue(String k) {
        boolean result;
        try (Transaction tx = database.beginTx()) {
            result = keyValueStore.hasKey(STORE_KEY + k);
            tx.success();
        }

        return result;
    }

    public void removeSettingValue(String key) {
        String k = SETTING_KEY_PREFIX + key;
        if (userProvidedConfiguration.containsKey(k)) {
            removeKey(STORE_KEY + k);
            userProvidedConfiguration.remove(k);
        }
    }

    public void removeValue(String k) {
        try (Transaction tx = database.beginTx()) {
            String ck = STORE_KEY + k;
            if (keyValueStore.hasKey(ck)) {
                keyValueStore.remove(ck);
            }
            tx.success();
        }
    }

    public void update(String key, Object value) {
        try (Transaction tx = database.beginTx()) {
            keyValueStore.set(STORE_KEY + key, value);
            tx.success();
        }
        loadUserConfiguration();
    }

    public void storeCustomPipeline(PipelineSpecification pipelineSpecification) {
        try {
            String serialized = mapper.writeValueAsString(pipelineSpecification);
            String key = PIPELINE_KEY_PREFIX + pipelineSpecification.getName();
            update(key, serialized);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<PipelineSpecification> loadCustomPipelines() {
        List<PipelineSpecification> list = new ArrayList<>();
        Map<String, Object> config = getAllConfigValuesFromStore();
        config.keySet().forEach(k -> {
            if (k.startsWith(PIPELINE_KEY_PREFIX)) {
                try {
                    PipelineSpecification pipelineSpecification = mapper.readValue(config.get(k).toString(), PipelineSpecification.class);
                    list.add(pipelineSpecification);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        return list;
    }

    public void storeWorkflowInstanceItem(WorkflowItem item) {
        try {
            String serialized = mapper.writeValueAsString(item.getInfo());
            String key = item.getPrefix() + item.getName();
            update(key, serialized);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<WorkflowInstanceItemInfo> loadPipelineInstanceItems(String prefix) {
        List<WorkflowInstanceItemInfo> list = new ArrayList<>();
        Map<String, Object> config = getAllConfigValuesFromStore();
        config.keySet().forEach(k -> {
            if (k.startsWith(prefix)) {
                try {
                    WorkflowInstanceItemInfo pipelineSpecification = mapper.readValue(config.get(k).toString(), WorkflowInstanceItemInfo.class);
                    list.add(pipelineSpecification);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return list;
    }

    public PipelineSpecification loadPipeline(String name) {
        Map<String, Object> config = getAllConfigValuesFromStore();
        AtomicReference<PipelineSpecification> result = new AtomicReference<>();
        config.keySet().forEach(k -> {
            if (k.startsWith(PIPELINE_KEY_PREFIX)) {
                try {
                    String s = config.get(k).toString();
                    PipelineSpecification pipelineSpecification = mapper.readValue(config.get(k).toString(), PipelineSpecification.class);
                    if (pipelineSpecification.getName().equals(name)) {
                        result.set(pipelineSpecification);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
        });

        return result.get();
    }

    public void removePipeline(String name, String textProcessor) {
        Map<String, Object> config = getAllConfigValuesFromStore();
        config.keySet().forEach(k -> {
            if (k.startsWith(PIPELINE_KEY_PREFIX)) {
                try {
                    PipelineSpecification pipelineSpecification = mapper.readValue(config.get(k).toString(), PipelineSpecification.class);
                    if (pipelineSpecification.getName().equals(name) && pipelineSpecification.getTextProcessor().equals(textProcessor)) {
                        removeKey(STORE_KEY + k);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
        });
    }

    public void updateInternalSetting(String key, Object value) {
        try (Transaction tx = database.beginTx()) {
            keyValueStore.set(STORE_KEY + SETTING_KEY_PREFIX + key, value);
            tx.success();
        }
        loadUserConfiguration();
    }

    private void removeKey(String key) {
        try (Transaction tx = database.beginTx()) {
            if (keyValueStore.hasKey(key)) {
                keyValueStore.remove(key);
            }
            tx.success();
        }
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

    public void saveModelPath(String key, String modelPaths) {
        update(MODEL_KEY_PREFIX + key, modelPaths);
    }

    public String getModelPaths(String key) {
        String value = null;
        if (hasStoreValue(MODEL_KEY_PREFIX + key)) {
            value = getAllConfigValuesFromStore().get(MODEL_KEY_PREFIX + key).toString();
        }

        return value;
    }

    private void loadUserConfiguration() {
        userProvidedConfiguration = getAllConfigValuesFromStore();
    }
}
