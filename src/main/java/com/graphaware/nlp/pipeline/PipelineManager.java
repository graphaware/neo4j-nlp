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
import com.graphaware.nlp.module.NLPConfiguration;
import com.graphaware.nlp.util.ServiceLoader;
import java.util.HashMap;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.logging.Log;

public class PipelineManager {
    
    private static final Log LOG = LoggerFactory.getLogger(PipelineManager.class);
    private final Map<String, Class<AbstractPipelineProcessor>> pipelineProcessors = new HashMap<>();
    
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
    
    public void init(GraphDatabaseService database, NLPConfiguration nlpConfiguration) {
        if (initialized) {
            return;
        }
        this.nlpConfiguration = nlpConfiguration;
        this.configuration = new DynamicConfiguration(database);
        this.database = database;
        initialized = true;
        loadPipelineProcessors();
    }
    
    public <P extends AbstractPipelineProcessor> P createPipeline(String name, String className, Map<String, Object> parameters) {
        return null;
    } 
    
    private void loadPipelineProcessors() {
        Map<String, Class<AbstractPipelineProcessor>> loadedInstances = ServiceLoader.loadClass(PipelineProcessor.class);
        pipelineProcessors.putAll(loadedInstances);
    }
}
