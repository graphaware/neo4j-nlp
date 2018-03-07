/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.pipeline.input;

import com.graphaware.nlp.pipeline.PipelineItem;
import com.graphaware.nlp.pipeline.PipelineConfiguration;
import org.neo4j.graphdb.GraphDatabaseService;

public abstract class PipelineInput<C extends PipelineConfiguration, T>
        extends PipelineItem<C>
        implements Iterable<PipelineInputEntry<T>> {

    public static final String PIPELINE_INPUT_KEY_PREFIX = "PIPELINE_INPUT_";

    public PipelineInput(String name, GraphDatabaseService database) {
        super(name, database);
    }

    @Override
    public String getPrefix() {
        return PIPELINE_INPUT_KEY_PREFIX;
    }
    
    
}
