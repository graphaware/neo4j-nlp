/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.pipeline;

import com.graphaware.nlp.dsl.result.PipelineInstanceItemInfo;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;

public abstract class PipelineItem<C extends PipelineConfiguration> {

    private final String name;
    private final GraphDatabaseService database;
    private C configuration;
    private boolean valid;

    public PipelineItem(String name, GraphDatabaseService database) {
        this.name = name;
        this.database = database;
    }

    public abstract String getPrefix();

    public abstract void init(Map<String, Object> parameters);

    public String getName() {
        return name;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public C getConfiguration() {
        return configuration;
    }

    public void setConfiguration(C configuration) {
        this.configuration = configuration;
    }

    public GraphDatabaseService getDatabase() {
        return database;
    }

    public PipelineInstanceItemInfo getInfo() {
        return new PipelineInstanceItemInfo(
                this.getClass().getName(),
                getName(),
                getConfiguration().getConfiguration(),
                isValid());
    }

}
