/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.workflow.output;

import com.graphaware.nlp.workflow.WorkflowItem;
import com.graphaware.nlp.workflow.processor.WorkflowProcessorOutputEntry;
import org.neo4j.graphdb.GraphDatabaseService;
import com.graphaware.nlp.workflow.WorkflowConfiguration;

/**
 *
 * @author ale
 */
public abstract class WorkflowOutput<C extends WorkflowConfiguration>
        extends WorkflowItem<C> {

    public static final String PIPELINE_OUTPUT_KEY_PREFIX = "PIPELINE_OUTPUT_";

    public WorkflowOutput(String name, GraphDatabaseService database) {
        super(name, database);
    }
    
    public abstract void process(WorkflowProcessorOutputEntry entry);

    @Override
    public String getPrefix() {
        return PIPELINE_OUTPUT_KEY_PREFIX;
    }

}
