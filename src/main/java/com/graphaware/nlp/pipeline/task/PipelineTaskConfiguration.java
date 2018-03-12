/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.pipeline.task;

import com.graphaware.nlp.pipeline.AbstractPipelineConfiguration;
import java.util.Map;

/**
 *
 * @author ale
 */
public class PipelineTaskConfiguration extends AbstractPipelineConfiguration {

    public static final String PIPELINE_INPUT_NAME = "input";
    public static final String PIPELINE_OUTPUT_NAME = "output";
    public static final String PIPELINE_PROCESSOR_NAME = "processor";
    public static final String SYNCRONOUS = "sync";

    public PipelineTaskConfiguration(Map<String, Object> configuration) {
        super(configuration);
    }

    public String getInput() {
        return (String) getConfiguration().get(PIPELINE_INPUT_NAME);
    }

    public String getOutput() {
        return (String) getConfiguration().get(PIPELINE_OUTPUT_NAME);
    }

    public String getProcessor() {
        return (String) getConfiguration().get(PIPELINE_PROCESSOR_NAME);
    }
    
    public boolean isSync() {
        return (Boolean) getConfiguration().getOrDefault(SYNCRONOUS, true);
    }

}
