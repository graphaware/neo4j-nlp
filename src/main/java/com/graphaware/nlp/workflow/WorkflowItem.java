/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.workflow;

import com.graphaware.nlp.dsl.result.WorkflowInstanceItemInfo;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;

public abstract class WorkflowItem<C extends WorkflowConfiguration, T> extends AbstractMessageHandler<T>{

    private final String name;
    private final GraphDatabaseService database;
    private C configuration;
    private boolean valid;

    public WorkflowItem(String name, GraphDatabaseService database) {
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

    public WorkflowInstanceItemInfo getInfo() {
        return new WorkflowInstanceItemInfo(
                this.getClass().getName(),
                getName(),
                getConfiguration().getConfiguration(),
                isValid());
    }

    public abstract void stop();

}
