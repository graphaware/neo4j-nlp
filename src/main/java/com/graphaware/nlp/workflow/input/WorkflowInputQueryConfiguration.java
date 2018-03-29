/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.workflow.input;

import com.graphaware.nlp.workflow.WorkflowBaseConfiguration;
import java.util.Map;

/**
 *
 * @author ale
 */
public class WorkflowInputQueryConfiguration extends WorkflowBaseConfiguration {

    public static final String CONF_QUERY = "query";

    public WorkflowInputQueryConfiguration(Map<String, Object> configuration) {
        super(configuration);
    }

    public String getQuery() {
        return (String) getConfiguration().get(CONF_QUERY);
    }
}
