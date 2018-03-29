/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.workflow.task;

/**
 *
 * @author ale
 */
public class WorkflowTaskExecutionInstance {
    private final String executionId;
    private final String taskName;
    private final long startTime;
    private long endTime;
    private TaskStatus endStatus;

    public WorkflowTaskExecutionInstance(String executionId, String taskName, long startTime) {
        this.executionId = executionId;
        this.taskName = taskName;
        this.startTime = startTime;
    }
    
    public String getExecutionId() {
        return executionId;
    }

    public String getTaskName() {
        return taskName;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public void setEndStatus(TaskStatus endStatus) {
        this.endStatus = endStatus;
    }

    public TaskStatus getEndStatus() {
        return endStatus;
    }
}
