package com.graphaware.nlp.dsl.result;

import com.graphaware.nlp.workflow.task.WorkflowTask;

public class WorkflowTaskResult {

    public String taskName;

    public String input;

    public String processor;

    public String output;

    public String status;

    public String info;

    public WorkflowTaskResult(WorkflowTask task) {
        this.taskName = task.getName();
        this.input = task.getInput().getName();
        this.processor = task.getProcess().getName();
        this.output = task.getOutput().getName();
        this.status = task.getStatus().name();
        this.info = task.getAdditionalInfo();
    }
}
