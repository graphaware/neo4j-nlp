/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.pipeline.output;

import com.graphaware.nlp.pipeline.AbstractPipelineConfiguration;
import java.util.Map;

public class StoreAnnotatedTextPipelineConfiguration extends AbstractPipelineConfiguration {
    private final static String POST_QUERY = "query";
    
    public StoreAnnotatedTextPipelineConfiguration(Map<String, Object> configuration) {
        super(configuration);
    }
    
    public String getQuery() {
        return (String)getConfiguration().get(POST_QUERY);
    }
    
}
