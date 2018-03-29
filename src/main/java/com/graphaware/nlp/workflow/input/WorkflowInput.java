/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.workflow.input;

import com.graphaware.nlp.workflow.WorkflowItem;
import org.neo4j.graphdb.GraphDatabaseService;
import com.graphaware.nlp.workflow.WorkflowConfiguration;

public abstract class WorkflowInput<C extends WorkflowConfiguration, T>
        extends WorkflowItem<C, Void>
        implements Iterable<WorkflowInputEntry<T>> {

    public static final String WORKFLOW_INPUT_KEY_PREFIX = "WORKFLOW_INPUT_";

    public WorkflowInput(String name, GraphDatabaseService database) {
        super(name, database);
    }

    @Override
    public String getPrefix() {
        return WORKFLOW_INPUT_KEY_PREFIX;
    }
    
    
}
