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
package com.graphaware.nlp.pipeline;

import com.graphaware.nlp.pipeline.processor.PipelineProcessor;
import com.graphaware.common.log.LoggerFactory;
import com.graphaware.nlp.annotation.NLPInput;
import com.graphaware.nlp.annotation.NLPOutput;
import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.dsl.result.PipelineInstanceItemInfo;
import com.graphaware.nlp.dsl.result.PipelineItemInfo;
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
import com.graphaware.nlp.pipeline.input.PipelineInput;
import com.graphaware.nlp.annotation.NLPProcessor;
import com.graphaware.nlp.annotation.NLPTask;
import com.graphaware.nlp.pipeline.output.PipelineOutput;
import com.graphaware.nlp.pipeline.task.PipelineTask;

public class PipelineManager {

    private static final Log LOG = LoggerFactory.getLogger(PipelineManager.class);
    private final Map<String, Class<PipelineProcessor>> pipelineProcessorClasses = new HashMap<>();
    private final Map<String, Class<PipelineInput>> pipelineInputClasses = new HashMap<>();
    private final Map<String, Class<PipelineOutput>> pipelineOutputClasses = new HashMap<>();
    private final Map<String, Class<PipelineTask>> pipelineTaskClasses = new HashMap<>();

    private final Map<String, PipelineProcessor> pipelineProcessorInstances = new HashMap<>();
    private final Map<String, PipelineInput> pipelineInputInstances = new HashMap<>();
    private final Map<String, PipelineOutput> pipelineOutputInstances = new HashMap<>();
    private final Map<String, PipelineTask> pipelineTaskInstances = new HashMap<>();

    private boolean initialized = false;
    private NLPConfiguration nlpConfiguration;
    private GraphDatabaseService database;
    private DynamicConfiguration configuration;

    private PipelineManager() {
    }

    public static PipelineManager getInstance() {
        return PipelineManagerHolder.INSTANCE;
    }

    private static class PipelineManagerHolder {

        private static final PipelineManager INSTANCE = new PipelineManager();
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
        loadPipelineProcessorClasses();
        loadPipelineInputClasses();
        loadPipelineOutputClasses();
        loadPipelineTaskClasses();

        //Load instances from configuration
        loadPipelineProcessorInstances();
        loadPipelineInputInstances();
        loadPipelineOutputInstances();
        loadPipelineTaskInstances();

    }

    public PipelineProcessor createPipelineProcessor(String id, String className, Map<String, Object> parameters) {
        return createPipelineItem(id, className, parameters, pipelineProcessorClasses, pipelineProcessorInstances);
    }

    public PipelineInput createPipelineInput(String id, String className, Map<String, Object> parameters) {
        return createPipelineItem(id, className, parameters, pipelineInputClasses, pipelineInputInstances);
    }

    public PipelineOutput createPipelineOutput(String id, String className, Map<String, Object> parameters) {
        return createPipelineItem(id, className, parameters, pipelineOutputClasses, pipelineOutputInstances);
    }

    public PipelineTask createPipelineTask(String id, String className, Map<String, Object> parameters) {
        return createPipelineItem(id, className, parameters, pipelineTaskClasses, pipelineTaskInstances);
    }

    private <T extends PipelineItem> T createPipelineItem(String id,
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
            storePipelineProcessorItem(newProcessorInstance);
        } catch (NoSuchMethodException | SecurityException
                | InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException ex) {
            String errorDescription = "Error while initializing the processor " + id + " of class " + className;
            LOG.error(errorDescription, ex);
            throw new RuntimeException(errorDescription);
        }
        return instances.get(id);
    }

    public Set<PipelineItemInfo> getPipelineProcessors() {
        return getPipelineItemInfo(pipelineProcessorClasses);
    }

    public Set<PipelineItemInfo> getPipelineInputs() {
        return getPipelineItemInfo(pipelineInputClasses);
    }

    private <T extends PipelineItem> Set<PipelineItemInfo> getPipelineItemInfo(Map<String, Class<T>> classes) {
        Set<PipelineItemInfo> result = new HashSet<>();
        classes.entrySet().stream().forEach(row -> {
            PipelineItemInfo processor = new PipelineItemInfo(row.getKey(), row.getValue().getName());
            result.add(processor);
        });
        return result;
    }
    
    public PipelineInput getPipelineInput(String name) {
        return pipelineInputInstances.get(name);
    }
    
    public PipelineOutput getPipelineOutput(String name) {
        return pipelineOutputInstances.get(name);
    }
    
    public PipelineProcessor getPipelineProcessor(String name) {
        return pipelineProcessorInstances.get(name);
    }
    
    public PipelineTask getPipelineTask(String name) {
        return pipelineTaskInstances.get(name);
    }

    public Set<PipelineInstanceItemInfo> getPipelineProcessorInstances() {
        return getPipelineInstanceInfo(pipelineInputInstances);
    }

    public Set<PipelineInstanceItemInfo> getPipelineInputInstances() {
        return getPipelineInstanceInfo(pipelineInputInstances);
    }
    
    public Set<PipelineInstanceItemInfo> getPipelineOutputInstances() {
        return getPipelineInstanceInfo(pipelineOutputInstances);
    }
    
    public Set<PipelineInstanceItemInfo> getPipelineTaskInstances() {
        return getPipelineInstanceInfo(pipelineTaskInstances);
    }

    private <T extends PipelineItem> Set<PipelineInstanceItemInfo> getPipelineInstanceInfo(Map<String, T> instances) {
        Set<PipelineInstanceItemInfo> result = new HashSet<>();
        instances.values().stream().forEach(row -> {
            result.add(row.getInfo());
        });
        return result;
    }

    private void loadPipelineProcessorInstances() {
        List<PipelineInstanceItemInfo> loadPipelineProcessor = configuration.loadPipelineInstanceItems(PipelineProcessor.PIPELINE_PROCESSOR_KEY_PREFIX);
        loadPipelineProcessor.stream().forEach((proc) -> {
            createPipelineProcessor(proc.name, proc.className, proc.parameters);
        });
    }

    private void loadPipelineInputInstances() {
        List<PipelineInstanceItemInfo> loadPipelineInstances = configuration.loadPipelineInstanceItems(PipelineInput.PIPELINE_INPUT_KEY_PREFIX);
        loadPipelineInstances.stream().forEach((proc) -> {
            createPipelineInput(proc.name, proc.className, proc.parameters);
        });
    }

    private void loadPipelineOutputInstances() {
        List<PipelineInstanceItemInfo> loadPipelineInstances = configuration.loadPipelineInstanceItems(PipelineOutput.PIPELINE_OUTPUT_KEY_PREFIX);
        loadPipelineInstances.stream().forEach((proc) -> {
            createPipelineOutput(proc.name, proc.className, proc.parameters);
        });
    }

    private void loadPipelineTaskInstances() {
        List<PipelineInstanceItemInfo> loadPipelineInstances = configuration.loadPipelineInstanceItems(PipelineTask.PIPELINE_TASK_KEY_PREFIX);
        loadPipelineInstances.stream().forEach((proc) -> {
            createPipelineTask(proc.name, proc.className, proc.parameters);
        });
    }

    private void loadPipelineInputClasses() {
        Map<String, Class<PipelineInput>> loadedInstances = ServiceLoader.loadClass(NLPInput.class);
        pipelineInputClasses.putAll(loadedInstances);
    }

    private void loadPipelineOutputClasses() {
        Map<String, Class<PipelineOutput>> loadedInstances = ServiceLoader.loadClass(NLPOutput.class);
        pipelineOutputClasses.putAll(loadedInstances);
    }

    private void loadPipelineTaskClasses() {
        Map<String, Class<PipelineTask>> loadedInstances = ServiceLoader.loadClass(NLPTask.class);
        pipelineTaskClasses.putAll(loadedInstances);
    }

    private void loadPipelineProcessorClasses() {
        Map<String, Class<PipelineProcessor>> loadedInstances = ServiceLoader.loadClass(NLPProcessor.class);
        pipelineProcessorClasses.putAll(loadedInstances);
    }

    private void storePipelineProcessorItem(PipelineItem processorItemInstance) {
        configuration.storePipelineItem(processorItemInstance);
    }

}
