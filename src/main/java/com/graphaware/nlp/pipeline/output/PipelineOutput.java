/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.pipeline.output;

import com.graphaware.nlp.pipeline.PipelineConfiguration;
import com.graphaware.nlp.pipeline.PipelineItem;
import com.graphaware.nlp.pipeline.processor.PipelineProcessorOutputEntry;
import org.neo4j.graphdb.GraphDatabaseService;

/**
 *
 * @author ale
 */
public abstract class PipelineOutput<C extends PipelineConfiguration>
        extends PipelineItem<C> {

    public static final String PIPELINE_OUTPUT_KEY_PREFIX = "PIPELINE_OUTPUT_";

    public PipelineOutput(String name, GraphDatabaseService database) {
        super(name, database);
    }
    
    public abstract void process(PipelineProcessorOutputEntry entry);

    @Override
    public String getPrefix() {
        return PIPELINE_OUTPUT_KEY_PREFIX;
    }

}
