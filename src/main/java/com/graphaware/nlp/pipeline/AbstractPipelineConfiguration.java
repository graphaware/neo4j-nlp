/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.pipeline;

import java.util.Map;

/**
 *
 * @author ale
 */
public class AbstractPipelineConfiguration implements PipelineConfiguration {
    
    private final Map<String, Object> configuration;

    public AbstractPipelineConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }
    
    @Override
    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    @Override
    public String getPipelineItemClassName() {
        return (String) configuration.get(PipelineConfiguration.CONF_CLASS_NAME);
    }
    
}
