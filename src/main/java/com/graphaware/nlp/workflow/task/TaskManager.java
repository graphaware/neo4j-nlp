/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.workflow.task;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TaskManager {

    private final ExecutorService executors;
    private final Cache<String, WorkflowTaskExecutionInstance> executions;
    private static final int DEFAULT_EXPIRATION_MINUTES = 30;

    private TaskManager() {
        executors = Executors.newFixedThreadPool(10);//This will be configurable
        executions = CacheBuilder.newBuilder()
                .expireAfterAccess(DEFAULT_EXPIRATION_MINUTES, TimeUnit.MINUTES).build();
    }

    public static TaskManager getInstance() {
        return TaskManagerHolder.INSTANCE;
    }

    public void stop(WorkflowTask workflowTask) {
        if (workflowTask.getStatus() == TaskStatus.RUNNING) {
            workflowTask.cancel();
        }
    }

    private static class TaskManagerHolder {

        private static final TaskManager INSTANCE = new TaskManager();
    }

    public WorkflowTask execute(WorkflowTask task) {
        if (task.getStatus() == TaskStatus.RUNNING) {
            throw new RuntimeException("The task " + task.getName() + " is already running");
        }
        task.reset();
        if (!task.isSync()) {
            executors.execute(() -> {
                doExecute(task);
            });
        } else {
            doExecute(task);
        }

        return task;
    }

    private void doExecute(WorkflowTask task) {
        WorkflowTaskExecutionInstance instanceInfo = getInstanceInfo(task);
        executions.put(instanceInfo.getExecutionId(), instanceInfo);
        task.doProcess();
        instanceInfo.setEndTime(System.currentTimeMillis());
        instanceInfo.setEndStatus(task.getStatus());
    }

    private WorkflowTaskExecutionInstance getInstanceInfo(WorkflowTask task) {
        return new WorkflowTaskExecutionInstance(UUID.randomUUID().toString(), task.getName(), System.currentTimeMillis());

    }
}
