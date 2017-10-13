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

import com.graphaware.nlp.annotation.NLPTextProcessor;
import com.graphaware.nlp.dsl.request.PipelineSpecification;
import com.graphaware.nlp.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TextProcessorsManager {

    private static final Logger LOG = LoggerFactory.getLogger(TextProcessorsManager.class);
    private static final String DEFAULT_TEXT_PROCESSOR = "com.graphaware.nlp.processor.stanford.StanfordTextProcessor";

    private final Map<String, TextProcessor> textProcessors = new HashMap<>();

    public TextProcessorsManager() {
        loadTextProcessors();
        initiateTextProcessors();
    }

    private void loadTextProcessors() {
        Map<String, TextProcessor> loadedInstances = ServiceLoader.loadInstances(NLPTextProcessor.class);
        textProcessors.putAll(loadedInstances);

//        loadedInstances.keySet().forEach(k -> {
//            TextProcessor ins = loadedInstances.get(k);
//            if (ins.override() != null && textProcessors.containsKey(ins.override())) {
//                textProcessors.remove(ins.override());
//            }
//        });
//
//        loadedInstances.keySet().forEach(k -> {
//            TextProcessor textProcessor = loadedInstances.get(k);
//            if (textProcessor.getAlias() != null && textProcessors.containsKey(k)) {
//                textProcessors.put(textProcessor.getAlias(), textProcessor);
//            }
//        });
    }

    private void initiateTextProcessors() {
        textProcessors.values().forEach(textProcessor -> {
            textProcessor.init();
        });
    }

    public TextProcessor getTextProcessor(String name) {
        if (!textProcessors.containsKey(name)) {
            throw new RuntimeException("Processor with name '" + name + "' does not exist");
        }
        return textProcessors.get(name);
    }

    public TextProcessor retrieveTextProcessor(String processor, String pipeline) {
        TextProcessor newTP;
        if (processor != null && processor.length() > 0) {
            newTP = getTextProcessor(processor);
            if (newTP == null) {
                throw new RuntimeException("Text processor " + processor + " doesn't exist");
            }
        } else {
            newTP = getDefaultProcessor();
        }
        if (pipeline != null && pipeline.length() > 0) {
            if (!newTP.checkPipeline(pipeline)) {
                throw new RuntimeException("Pipeline with name " + pipeline
                        + " doesn't exist for processor " + newTP.getClass().getName());
            }
        } else {
            throw new RuntimeException("Pipeline not specified");
        }
        LOG.info("Using text processor: " + newTP.getClass().getName());

        return newTP;
    }

    public Map<String, TextProcessor> getTextProcessors() {
        return textProcessors;
    }

    public TextProcessor getDefaultProcessor() {
        return textProcessors.get(getDefaultProcessorName());
    }

    public Set<String> getTextProcessorNames() {
        return textProcessors.keySet();
    }

    public PipelineCreationResult createPipeline(PipelineSpecification pipelineSpecification) {
        String processorName = pipelineSpecification.getTextProcessor();
        if (processorName == null || !textProcessors.containsKey(processorName)) {
            throw new RuntimeException("Processor " + processorName + " does not exist");
        }
        TextProcessor processor = textProcessors.get(processorName);
        processor.createPipeline(pipelineSpecification);

        LOG.info("Created pipeline " + pipelineSpecification.getName() + " for processor " + processorName);

        return new PipelineCreationResult(0, "");
    }

    public void removePipeline(String processor, String pipeline) {
        if (!textProcessors.containsKey(processor)) {
            throw new RuntimeException("No text processor with name " + processor + " available");
        }

        // @todo extract to its own method
        TextProcessor textProcessor = textProcessors.get(processor);
        textProcessor.removePipeline(pipeline);
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

    private String getDefaultProcessorName() {
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
}
