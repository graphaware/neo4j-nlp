/*
 * Copyright (c) 2013-2017 GraphAware
 *
 * This file is part of the GraphAware Framework.
 *
 * GraphAware Framework is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package com.graphaware.nlp.workflow;

import com.graphaware.nlp.workflow.processor.WorkflowProcessor;
import com.graphaware.common.log.LoggerFactory;
import com.graphaware.nlp.annotation.NLPInput;
import com.graphaware.nlp.annotation.NLPOutput;
import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.dsl.result.WorkflowInstanceItemInfo;
import com.graphaware.nlp.dsl.result.WorkflowItemInfo;
import com.graphaware.nlp.module.NLPConfiguration;
import com.graphaware.nlp.util.ServiceLoader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.logging.Log;
import com.graphaware.nlp.workflow.input.WorkflowInput;
import com.graphaware.nlp.annotation.NLPProcessor;
import com.graphaware.nlp.annotation.NLPTask;
import com.graphaware.nlp.workflow.output.WorkflowOutput;
import com.graphaware.nlp.workflow.task.WorkflowTask;

public class WorkflowManager {

    private static final Log LOG = LoggerFactory.getLogger(WorkflowManager.class);
    private final Map<String, Class<WorkflowProcessor>> workflowProcessorClasses = new HashMap<>();
    private final Map<String, Class<WorkflowInput>> workflowInputClasses = new HashMap<>();
    private final Map<String, Class<WorkflowOutput>> workflowOutputClasses = new HashMap<>();
    private final Map<String, Class<WorkflowTask>> workflowTaskClasses = new HashMap<>();

    private final Map<String, WorkflowProcessor> workflowProcessorInstances = new HashMap<>();
    private final Map<String, WorkflowInput> workflowInputInstances = new HashMap<>();
    private final Map<String, WorkflowOutput> workflowOutputInstances = new HashMap<>();
    private final Map<String, WorkflowTask> workflowTaskInstances = new HashMap<>();

    private boolean initialized = false;
    private NLPConfiguration nlpConfiguration;
    private GraphDatabaseService database;
    private DynamicConfiguration configuration;
    
    private static WorkflowManager instance = null;

    private WorkflowManager() {
    }

    public static WorkflowManager getInstance() {
        if (WorkflowManager.instance == null) {
            synchronized (WorkflowManager.class) {
                if (WorkflowManager.instance == null) {
                    WorkflowManager.instance = new WorkflowManager();
                }
            }
        }

        return WorkflowManager.instance;
    }

    public void init(GraphDatabaseService database, NLPConfiguration nlpConfiguration, DynamicConfiguration configuration) {
        if (initialized) {
            return;
        }
        this.nlpConfiguration = nlpConfiguration;
        this.configuration = configuration;
        this.database = database;
        initialized = true;
        //Load classes from jar
        loadWorkflowProcessorClasses();
        loadWorkflowInputClasses();
        loadWorkflowOutputClasses();
        loadWorkflowTaskClasses();

        //Load instances from configuration
        loadWorkflowProcessorInstances();
        loadWorkflowInputInstances();
        loadWorkflowOutputInstances();
        loadWorkflowTaskInstances();

    }

    public WorkflowProcessor createWorkflowProcessor(String id, String className, Map<String, Object> parameters) {
        return createWorkflowItem(id, className, parameters, workflowProcessorClasses, workflowProcessorInstances);
    }

    public WorkflowInput createWorkflowInput(String id, String className, Map<String, Object> parameters) {
        return createWorkflowItem(id, className, parameters, workflowInputClasses, workflowInputInstances);
    }

    public WorkflowOutput createWorkflowOutput(String id, String className, Map<String, Object> parameters) {
        return createWorkflowItem(id, className, parameters, workflowOutputClasses, workflowOutputInstances);
    }

    public WorkflowTask createWorkflowTask(String id, String className, Map<String, Object> parameters) {
        return createWorkflowItem(id, className, parameters, workflowTaskClasses, workflowTaskInstances);
    }

    private <T extends WorkflowItem> T createWorkflowItem(String id,
            String className,
            Map<String, Object> parameters,
            Map<String, Class<T>> classes,
            Map<String, T> instances) {
        if (!classes.containsKey(className)) {
            throw new RuntimeException("Processor Item class with name " + className + " not found!");
        }
        //Check if it exist
        if (instances.containsKey(id)) {
            throw new RuntimeException("Processor Item instance with id " + id + " already exist!");
        }

        Class<T> pipelineItemClass = classes.get(className);
        Constructor<T> constructor;
        try {
            constructor = pipelineItemClass.getConstructor(String.class, GraphDatabaseService.class);
            T newProcessorInstance = constructor.newInstance(id, database);
            newProcessorInstance.init(parameters);
            instances.put(id, newProcessorInstance);
            storeWorkflowProcessorItem(newProcessorInstance);
        } catch (NoSuchMethodException | SecurityException
                | InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException ex) {
            String errorDescription = "Error while initializing the processor " + id + " of class " + className;
            LOG.error(errorDescription, ex);
            throw new RuntimeException(errorDescription);
        }
        return instances.get(id);
    }
    
    public WorkflowInput deleteWorkflowInput(String id) {
        return workflowInputInstances.remove(id);
    }
    
    public WorkflowOutput deleteWorkflowOutput(String id) {
        return workflowOutputInstances.remove(id);
    }
    
    public WorkflowProcessor deleteWorkflowProcessor(String id) {
        return workflowProcessorInstances.remove(id);
    }
    
    public WorkflowTask deleteWorkflowTask(String id) {
        return workflowTaskInstances.remove(id);
    }

    public Set<WorkflowItemInfo> getWorkflowProcessorClasses() {
        return getWorkflowItemInfo(workflowProcessorClasses);
    }

    public Set<WorkflowItemInfo> getWorkflowInputClasses() {
        return getWorkflowItemInfo(workflowInputClasses);
    }
    
    public Set<WorkflowItemInfo> getWorkflowOutputClasses() {
        return getWorkflowItemInfo(workflowOutputClasses);
    }
    
    public Set<WorkflowItemInfo> getWorkflowTaskClasses() {
        return getWorkflowItemInfo(workflowTaskClasses);
    }

    private <T extends WorkflowItem> Set<WorkflowItemInfo> getWorkflowItemInfo(Map<String, Class<T>> classes) {
        Set<WorkflowItemInfo> result = new HashSet<>();
        classes.entrySet().stream().forEach(row -> {
            WorkflowItemInfo processor = new WorkflowItemInfo(row.getKey(), row.getValue().getName());
            result.add(processor);
        });
        return result;
    }
    
    public WorkflowInput getWorkflowInput(String name) {
        return workflowInputInstances.get(name);
    }
    
    public WorkflowOutput getWorkflowOutput(String name) {
        return workflowOutputInstances.get(name);
    }
    
    public WorkflowProcessor getWorkflowProcessor(String name) {
        return workflowProcessorInstances.get(name);
    }
    
    public WorkflowTask getWorkflowTask(String name) {
        return workflowTaskInstances.get(name);
    }

    public Set<WorkflowInstanceItemInfo> getWorkflowProcessorInstances() {
        return getWorkflowInstanceInfo(workflowProcessorInstances);
    }

    public Set<WorkflowInstanceItemInfo> getWorkflowInputInstances() {
        return getWorkflowInstanceInfo(workflowInputInstances);
    }
    
    public Set<WorkflowInstanceItemInfo> getWorkflowOutputInstances() {
        return getWorkflowInstanceInfo(workflowOutputInstances);
    }
    
    public Set<WorkflowInstanceItemInfo> getWorkflowTaskInstances() {
        return getWorkflowInstanceInfo(workflowTaskInstances);
    }

    private <T extends WorkflowItem> Set<WorkflowInstanceItemInfo> getWorkflowInstanceInfo(Map<String, T> instances) {
        Set<WorkflowInstanceItemInfo> result = new HashSet<>();
        instances.values().stream().forEach(row -> {
            result.add(row.getInfo());
        });
        return result;
    }

    private void loadWorkflowProcessorInstances() {
        List<WorkflowInstanceItemInfo> loadPipelineProcessor = configuration.loadPipelineInstanceItems(WorkflowProcessor.WORFKLOW_PROCESSOR_KEY_PREFIX);
        loadPipelineProcessor.stream().forEach((proc) -> {
            createWorkflowProcessor(proc.name, proc.className, proc.parameters);
        });
    }

    private void loadWorkflowInputInstances() {
        List<WorkflowInstanceItemInfo> loadPipelineInstances = configuration.loadPipelineInstanceItems(WorkflowInput.WORKFLOW_INPUT_KEY_PREFIX);
        loadPipelineInstances.stream().forEach((proc) -> {
            createWorkflowInput(proc.name, proc.className, proc.parameters);
        });
    }

    private void loadWorkflowOutputInstances() {
        List<WorkflowInstanceItemInfo> loadPipelineInstances = configuration.loadPipelineInstanceItems(WorkflowOutput.WORFKLOW_OUTPUT_KEY_PREFIX);
        loadPipelineInstances.stream().forEach((proc) -> {
            createWorkflowOutput(proc.name, proc.className, proc.parameters);
        });
    }

    private void loadWorkflowTaskInstances() {
        List<WorkflowInstanceItemInfo> loadPipelineInstances = configuration.loadPipelineInstanceItems(WorkflowTask.WORFKLOW_TASK_KEY_PREFIX);
        loadPipelineInstances.stream().forEach((proc) -> {
            createWorkflowTask(proc.name, proc.className, proc.parameters);
        });
    }

    private void loadWorkflowInputClasses() {
        Map<String, Class<WorkflowInput>> loadedInstances = ServiceLoader.loadClass(NLPInput.class);
        workflowInputClasses.putAll(loadedInstances);
    }

    private void loadWorkflowOutputClasses() {
        Map<String, Class<WorkflowOutput>> loadedInstances = ServiceLoader.loadClass(NLPOutput.class);
        workflowOutputClasses.putAll(loadedInstances);
    }

    private void loadWorkflowTaskClasses() {
        Map<String, Class<WorkflowTask>> loadedInstances = ServiceLoader.loadClass(NLPTask.class);
        workflowTaskClasses.putAll(loadedInstances);
    }

    private void loadWorkflowProcessorClasses() {
        Map<String, Class<WorkflowProcessor>> loadedInstances = ServiceLoader.loadClass(NLPProcessor.class);
        workflowProcessorClasses.putAll(loadedInstances);
    }

    private void storeWorkflowProcessorItem(WorkflowItem processorItemInstance) {
        configuration.storePipelineItem(processorItemInstance);
    }

}
