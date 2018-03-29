/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.workflow;

import java.util.Map;

/**
 *
 * @author ale
 */
public class WorkflowBaseConfiguration implements WorkflowConfiguration {
    
    private final Map<String, Object> configuration;

    public WorkflowBaseConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }
    
    @Override
    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    @Override
    public String getPipelineItemClassName() {
        return (String) configuration.get(WorkflowConfiguration.CONF_CLASS_NAME);
    }
    
}
