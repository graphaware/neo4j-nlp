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

import com.graphaware.common.log.LoggerFactory;
import com.graphaware.nlp.annotation.PipelineProcessor;
import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.dsl.result.ProcessorInstanceItem;
import com.graphaware.nlp.dsl.result.ProcessorItem;
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

public class PipelineManager {

    private static final Log LOG = LoggerFactory.getLogger(PipelineManager.class);
    private final Map<String, Class<AbstractPipelineProcessor>> pipelineProcessorClasses = new HashMap<>();
    private final Map<String, AbstractPipelineProcessor> pipelineProcessorInstances = new HashMap<>();

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
        loadPipelineProcessorClasses();
        loadPipelineProcessorInstances();        
    }

    public AbstractPipelineProcessor createPipelineProcessor(String id, String className, Map<String, Object> parameters) {
        if (!pipelineProcessorClasses.containsKey(className)) {
            throw new RuntimeException("Processor class with name " + className + " not found!");
        }
        //Check if it exist
        if (pipelineProcessorInstances.containsKey(id)) {
            throw new RuntimeException("Processor instance with id " + id + " already exist!");
        }
        Class<AbstractPipelineProcessor> pipelineProcessorClass = pipelineProcessorClasses.get(className);
        Constructor<AbstractPipelineProcessor> constructor;
        try {
            constructor = pipelineProcessorClass.getConstructor(String.class);
            AbstractPipelineProcessor newProcessorInstance = constructor.newInstance(id);
            newProcessorInstance.init(parameters);
            pipelineProcessorInstances.put(id, newProcessorInstance);
            storePipelineProcessorInstance(newProcessorInstance);
            
        } catch (NoSuchMethodException | SecurityException 
                | InstantiationException | IllegalAccessException 
                | IllegalArgumentException | InvocationTargetException ex) {
            String errorDescription = "Error while initializing the processor " + id + " of class " + className;
            LOG.error(errorDescription, ex);
            throw new RuntimeException(errorDescription);
        }
        return pipelineProcessorInstances.get(id);
    }

    public Set<ProcessorItem> getPipelineProcessors() {
        Set<ProcessorItem> result = new HashSet<>();
        pipelineProcessorClasses.entrySet().stream().forEach(row -> {
            ProcessorItem processor = new ProcessorItem(row.getKey(), row.getValue().getName());
            result.add(processor);
        });
        return result;
    }
    
    public Set<ProcessorInstanceItem> getPipelineProcessorItems() {
        Set<ProcessorInstanceItem> result = new HashSet<>();
        pipelineProcessorInstances.values().stream().forEach(row -> {
            result.add(row.getInfo());
        });
        return result;
    }

    private void loadPipelineProcessorInstances() {
        List<ProcessorInstanceItem> loadPipelineProcessor = configuration.loadPipelineProcessor();
        loadPipelineProcessor.stream().forEach((proc) -> {
            createPipelineProcessor(proc.name, proc.className, proc.parameters);
        });
    }
    
    private void storePipelineProcessorInstance(AbstractPipelineProcessor processorInstance) {
        configuration.storePipelineProcessor(processorInstance);
    }
    
    private void loadPipelineProcessorClasses() {
        Map<String, Class<AbstractPipelineProcessor>> loadedInstances = ServiceLoader.loadClass(PipelineProcessor.class);
        pipelineProcessorClasses.putAll(loadedInstances);
        
    }
}
