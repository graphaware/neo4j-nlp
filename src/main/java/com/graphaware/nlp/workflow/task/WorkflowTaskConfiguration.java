/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.workflow.task;

import com.graphaware.nlp.workflow.WorkflowBaseConfiguration;
import java.util.Map;

/**
 *
 * @author ale
 */
public class WorkflowTaskConfiguration extends WorkflowBaseConfiguration {

    public static final String WORFKLOW_INPUT_NAME = "input";
    public static final String WORFKLOW_OUTPUT_NAME = "output";
    public static final String WORFKLOW_PROCESSOR_NAME = "processor";
    public static final String SYNCRONOUS = "sync";

    public WorkflowTaskConfiguration(Map<String, Object> configuration) {
        super(configuration);
    }

    public String getInput() {
        return (String) getConfiguration().get(WORFKLOW_INPUT_NAME);
    }

    public String getOutput() {
        return (String) getConfiguration().get(WORFKLOW_OUTPUT_NAME);
    }

    public String getProcessor() {
        return (String) getConfiguration().get(WORFKLOW_PROCESSOR_NAME);
    }
    
    public boolean isSync() {
        return (Boolean) getConfiguration().getOrDefault(SYNCRONOUS, true);
    }

}
