/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.workflow.output;

import com.graphaware.nlp.workflow.WorkflowBaseConfiguration;
import java.util.Map;

public class StoreAnnotatedTextWorkflowConfiguration extends WorkflowBaseConfiguration {
    private final static String POST_QUERY = "query";
    
    public StoreAnnotatedTextWorkflowConfiguration(Map<String, Object> configuration) {
        super(configuration);
    }
    
    public String getQuery() {
        return (String)getConfiguration().get(POST_QUERY);
    }
    
}
