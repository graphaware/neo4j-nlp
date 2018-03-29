/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.workflow.task;

import com.graphaware.nlp.dsl.result.WorkflowInstanceItemInfo;
import java.util.Map;

/**
 *
 * @author ale
 */
public class WorkflowTaskInstanceItemInfo extends WorkflowInstanceItemInfo {
    private final TaskStatus status;
    
    public WorkflowTaskInstanceItemInfo(String className, String name, Map<String, Object> parameters, boolean valid, TaskStatus status) {
        super(className, name, parameters, valid);
        this.status = status;
    }

    public TaskStatus getStatus() {
        return status;
    }
}
