/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.pipeline.input;

import com.graphaware.nlp.pipeline.AbstractPipelineConfiguration;
import java.util.Map;

/**
 *
 * @author ale
 */
public class PipelineInputQueryConfiguration extends AbstractPipelineConfiguration {

    public static final String CONF_QUERY = "query";

    public PipelineInputQueryConfiguration(Map<String, Object> configuration) {
        super(configuration);
    }

    public String getQuery() {
        return (String) getConfiguration().get(CONF_QUERY);
    }
}
