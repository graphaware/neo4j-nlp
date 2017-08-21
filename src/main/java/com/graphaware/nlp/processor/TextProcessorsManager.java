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
package com.graphaware.nlp.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphaware.nlp.annotation.NLPTextProcessor;
import com.graphaware.nlp.dsl.PipelineSpecification;
import com.graphaware.nlp.util.ServiceLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.graphaware.nlp.persistence.constants.Labels.Pipeline;

public class TextProcessorsManager {

    private static final Logger LOG = LoggerFactory.getLogger(TextProcessorsManager.class);
    private static final String DEFAULT_TEXT_PROCESSOR = "com.graphaware.nlp.processor.stanford.StanfordTextProcessor";

    private final GraphDatabaseService database;
    private final Map<String, TextProcessor> textProcessors = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public TextProcessorsManager(GraphDatabaseService database) {
        this.database = database;
        loadTextProcessors();
        loadPipelines();
    }

    private void loadTextProcessors() {
        textProcessors.putAll(ServiceLoader.loadInstances(NLPTextProcessor.class));
    }

    public TextProcessor getTextProcessor(String name) {
        return textProcessors.get(name);
    }

    public Set<String> getTextProcessorNames() {
        return textProcessors.keySet();
    }

    public PipelineCreationResult createPipeline(PipelineSpecification pipelineSpecification) {
        String processorName = pipelineSpecification.getTextProcessor();
        if (processorName == null || !textProcessors.containsKey(processorName)) {
            return new PipelineCreationResult(-1, "Processor class not specified or not existing");
        }
        TextProcessor processor = textProcessors.get(processorName);
        //TODO add catch
        processor.createPipeline(pipelineSpecification);

        return new PipelineCreationResult(0, "");
    }

    public Map<String, TextProcessor> getTextProcessors() {
        return textProcessors;
    }

    private void loadPipelines() {
        try (Transaction tx = database.beginTx()) {
            ResourceIterator<Node> pipelineNodes = database.findNodes(Pipeline);
            pipelineNodes.stream().forEach(pipeline -> {
                createPipeline(PipelineSpecification.fromMap(pipeline.getAllProperties()));
            });
            tx.success();
        }
    }

    public Node storePipeline(PipelineSpecification pipelineSpecification) {
        try (Transaction tx = database.beginTx()) {
            Node pipelineNode = database.createNode(Pipeline);
            Map<String, Object> inputParams = mapper.convertValue(pipelineSpecification, Map.class);
            inputParams.entrySet().stream().forEach(entry -> {
                if (entry.getValue() != null) {
                    pipelineNode.setProperty(entry.getKey(), entry.getValue());
                }
            });
            tx.success();
            return pipelineNode;
        }
    }

    public String getDefaultProcessorName() {
        if (textProcessors.isEmpty()) {
            return null;
        }

        if (textProcessors.containsKey(DEFAULT_TEXT_PROCESSOR)) {
            return DEFAULT_TEXT_PROCESSOR; // return the default text processor if it's available
        }

        if (textProcessors.keySet().size() > 0) {
            return textProcessors.keySet().iterator().next(); // return first processor (or null) in the list in case the default text processor doesn't exist
        }

        return null;
    }

    public TextProcessor getDefaultProcessor() {
        return textProcessors.get(getDefaultProcessorName());
    }

    public void removePipeline(String processor, String pipeline) {
        if (!textProcessors.containsKey(processor)) {
            throw new RuntimeException("No text processor with name " + processor + " available");
        }

        // @todo extract to its own method
        TextProcessor textProcessor = textProcessors.get(processor);
        textProcessor.removePipeline(pipeline);
        removePipelineNode(processor, pipeline);
    }

    private void removePipelineNode(String processor, String pipeline) throws QueryExecutionException {
        Map<String, Object> map = new HashMap<>();
        map.put("textProcessor", processor);
        map.put("name", pipeline);
        try (Transaction tx = database.beginTx()) {
            database.execute("MATCH (n:Pipeline {textProcessor: {textProcessor}, name: {name}}) DELETE n", map);
            tx.success();
        }
    }

    // @todo is it really needed ?
    public static class PipelineCreationResult {

        private final int result;
        private final String message;

        public PipelineCreationResult(int result, String message) {
            this.result = result;
            this.message = message;
        }

        public int getResult() {
            return result;
        }

        public String getMessage() {
            return message;
        }
    }
}
