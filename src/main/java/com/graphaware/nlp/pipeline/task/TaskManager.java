/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.pipeline.task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskManager {

    private final ExecutorService executors;

    private TaskManager() {
        executors = Executors.newFixedThreadPool(10);
    }

    public static TaskManager getInstance() {
        return TaskManagerHolder.INSTANCE;
    }

    private static class TaskManagerHolder {

        private static final TaskManager INSTANCE = new TaskManager();
    }

    public void execute(PipelineTask task) {
        if (!task.isSync()) {
            executors.execute(task);
        } else {
            task.doProcess();
        }
            
    }
}
