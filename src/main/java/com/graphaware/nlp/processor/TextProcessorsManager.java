/*
 * Copyright (c) 2013-2018 GraphAware
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
import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.configuration.SettingsConstants;
import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.domain.Constants;
import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.dsl.request.PipelineSpecification;
import com.graphaware.nlp.exception.InvalidPipelineException;
import com.graphaware.nlp.exception.InvalidTextException;
import com.graphaware.nlp.exception.InvalidTextProcessorException;
import com.graphaware.nlp.exception.TextAnalysisException;
import com.graphaware.nlp.util.ServiceLoader;
import org.neo4j.logging.Log;
import com.graphaware.common.log.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class TextProcessorsManager {

    private static final Log LOG = LoggerFactory.getLogger(TextProcessorsManager.class);
    private static final String DEFAULT_TEXT_PROCESSOR = "com.graphaware.nlp.processor.stanford.StanfordTextProcessor";

    private final Map<String, TextProcessor> textProcessors = new HashMap<>();
    private final DynamicConfiguration configuration;

    private final Map<String, PipelineSpecification> defaultPipelineByLanguage = new HashMap<>();

    private String supportedLanguage;

    public TextProcessorsManager(DynamicConfiguration configuration) {
        this.configuration = configuration;
        loadTextProcessors();
        initiateTextProcessors();
    }

    private void loadTextProcessors() {
        Map<String, TextProcessor> loadedInstances = ServiceLoader.loadInstances(NLPTextProcessor.class);
        textProcessors.putAll(loadedInstances);
    }

    private void initiateTextProcessors() {
        textProcessors.values().forEach(textProcessor -> {
            textProcessor.init();
        });
    }

    public void registerPipelinesFromConfig() {
        configuration.loadCustomPipelines().forEach(pipelineSpecification -> {
            // Check that the text processor exist, it can happen that the configuration
            // hold a reference to a processor that is not more registered, in order to avoid
            // this method to fail completely for valid pipelines, we just do not register
            // possible legacy pipelines
            if (getTextProcessorNames().contains(pipelineSpecification.getTextProcessor())) {
                createPipeline(pipelineSpecification);
            }
        });
    }

    public PipelineSpecification getPipelineSpecification(String pipelineName) {
        PipelineSpecification pipelineSpecification = configuration.loadPipeline(pipelineName);
        if (null == pipelineSpecification) {
            throw new RuntimeException("No pipeline " + pipelineName);
        }
        return pipelineSpecification;
    }

    public void addPipeline(PipelineSpecification pipelineSpecification) {
        // Check that the textProcessor exist !
        if (null == pipelineSpecification.getTextProcessor()
                || getTextProcessor(pipelineSpecification.getTextProcessor()) == null) {
            throw new RuntimeException(String.format("Invalid text processor %s", pipelineSpecification.getTextProcessor()));
        }
        if (null != configuration.loadPipeline(pipelineSpecification.getName())) {
            throw new RuntimeException("Pipeline with name " + pipelineSpecification.getName() + " already exist");
        }
        createPipeline(pipelineSpecification);
        configuration.storeCustomPipeline(pipelineSpecification);
    }

    public void removePipeline(String pipeline, String processor) {
        PipelineSpecification pipelineSpecification = configuration.loadPipeline(pipeline);
        String language = pipelineSpecification.getLanguage();
        configuration.removePipeline(pipeline, processor);
        getTextProcessor(processor).removePipeline(pipeline);
        if (getPipelineSpecifications().stream().noneMatch(item -> item.getLanguage().equals(language))) {
            removeSupportedLanguage(language);
        } else {
            PipelineSpecification defaultForLanguage = defaultPipelineByLanguage.get(language);
            if (defaultForLanguage != null &&
                    defaultForLanguage.getName().equalsIgnoreCase(pipeline)) {
                PipelineSpecification newDefault = getPipelineSpecifications().stream().filter(item -> item.getLanguage().equals(language)).collect(Collectors.toList()).get(0);
                defaultPipelineByLanguage.put(language, newDefault);
            }
        }
    }

    public List<PipelineSpecification> getPipelineSpecifications(String name) {
        PipelineSpecification pipelineSpecification = configuration.loadPipeline(name);
        if (null != pipelineSpecification) {
            return Arrays.asList(pipelineSpecification);
        }

        return new ArrayList<>();
    }

    public List<PipelineSpecification> getPipelineSpecifications() {
        return configuration.loadCustomPipelines();
    }

    public boolean hasPipeline(String name) {
        List<PipelineSpecification> pipelines = getPipelineSpecifications(name);
        return pipelines.size() > 0;
    }

    public TextProcessor getTextProcessor(String name) {
        if (!textProcessors.containsKey(name)) {
            throw new InvalidTextProcessorException("Processor with name '" + name + "' does not exist");
        }
        return textProcessors.get(name);
    }

    public TextProcessor retrieveTextProcessor(String processor, String pipeline) {
        TextProcessor newTP;
        if (processor != null && processor.length() > 0) {
            newTP = getTextProcessor(processor);
            if (newTP == null) {
                throw new InvalidTextProcessorException("Text processor " + processor + " doesn't exist");
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
            throw new InvalidPipelineException("Pipeline not specified");
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

    public Tag annotateTag(String text, String language) {
        PipelineSpecification spec = getDefaultPipeline(language);
        TextProcessor processor = getTextProcessor(spec.getTextProcessor());
        return processor.annotateTag(text, spec);
    }

    public List<Tag> annotateTags(String text, String language) {
        PipelineSpecification spec = getDefaultPipeline(language);
        TextProcessor processor = getTextProcessor(spec.getTextProcessor());
        return processor.annotateTags(text, spec);
    }

    public AnnotatedText annotate(String text, String pipelineName) {
        return annotate(text, getPipelineSpecification(pipelineName));
    }

    public AnnotatedText annotate(String text, PipelineSpecification pipelineSpecification) {
        if (null == pipelineSpecification) {
            throw new RuntimeException("No pipeline " + pipelineSpecification.name + " found.");
        }

        if (text.trim().equalsIgnoreCase("")) {
            throw new InvalidTextException();
        }

        TextProcessor processor = getTextProcessor(pipelineSpecification.getTextProcessor());
        long startTime = -System.currentTimeMillis();
        AnnotatedText annotatedText;

        try {
            annotatedText = processor.annotateText(text, pipelineSpecification);
        } catch (Exception e) {
            throw new TextAnalysisException(e.getMessage(), e);
        }

        LOG.info("Time to annotate " + (System.currentTimeMillis() + startTime));
        return annotatedText;
    }

    private PipelineCreationResult createPipeline(PipelineSpecification pipelineSpecification) {
        addSupportedLanguage(pipelineSpecification);
        String processorName = pipelineSpecification.getTextProcessor();
        if (processorName == null || !textProcessors.containsKey(processorName)) {
            throw new RuntimeException("Processor " + processorName + " does not exist");
        }
        TextProcessor processor = textProcessors.get(processorName);
        processor.createPipeline(pipelineSpecification);
        return new PipelineCreationResult(0, "");
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

    protected void addSupportedLanguage(PipelineSpecification pipelineSpecification) {
        String language = pipelineSpecification.getLanguage();
        if (language == null) {
            pipelineSpecification.setLanguage(Constants.DEFAULT_LANGUAGE);
            language = Constants.DEFAULT_LANGUAGE;
        }
        if (supportedLanguage == null ||
                supportedLanguage.isEmpty()) {
            supportedLanguage = language;
        } else if (supportedLanguage != null &&
                !supportedLanguage.equalsIgnoreCase(language)) {
            throw new RuntimeException("Multiple languages not supported in this version");
        }
        checkAndSetDefaultPipeline(pipelineSpecification, language);
    }

    protected void checkAndSetDefaultPipeline(PipelineSpecification pipelineSpecification, String language) {
        String pipelineName = (String) configuration.getSettingValueFor(getDefaultPipelineKey(language));
        if (pipelineName == null) {
            setDefaultPipelineAux(language, pipelineSpecification);
        } else {
            PipelineSpecification defaultPipelineSpecification = configuration.loadPipeline(pipelineName);
            if (pipelineSpecification != null &&
                    !defaultPipelineByLanguage.containsKey(language)) {
                defaultPipelineByLanguage.put(language, defaultPipelineSpecification);
            } else {
                //Since a default is set but not exist let me set a default for the language anyway
                defaultPipelineByLanguage.put(language, pipelineSpecification);
            }
        }
    }

    public void setDefaultPipeline(String pipelineName, String language) {
        PipelineSpecification pipelineSpecification = configuration.loadPipeline(pipelineName);
        if (null == pipelineSpecification) {
            throw new RuntimeException("No pipeline " + pipelineName + " exist");
        }
        setDefaultPipelineAux(language, pipelineSpecification);
    }

    public PipelineSpecification getDefaultPipeline(String language) {
        if (defaultPipelineByLanguage.containsKey(language)) {
            return defaultPipelineByLanguage.get(language);
        }
        PipelineSpecification pipelineSpecification = getPipelineSpecificationFromConfig(language);
        if (pipelineSpecification == null) {
            return null;
        }
        defaultPipelineByLanguage.put(language, pipelineSpecification);
        return pipelineSpecification;
    }

    private PipelineSpecification getPipelineSpecificationFromConfig(String language) {
        String pipelineName = (String) configuration.getSettingValueFor(getDefaultPipelineKey(language));
        if (pipelineName == null) {
            LOG.warn("Something goes wrong (this shouldn't happen) default pipeline not available");
            return null;
        }
        PipelineSpecification pipelineSpecification = configuration.loadPipeline(pipelineName);
        if (pipelineSpecification == null) {
            LOG.warn("Default pipeline specification for " + pipelineName + " not available");
            return null;
        }
        return pipelineSpecification;
    }

    private void setDefaultPipelineAux(String language, PipelineSpecification pipelineSpecification) {
        configuration.updateInternalSetting(getDefaultPipelineKey(language), pipelineSpecification.getName());
        defaultPipelineByLanguage.put(language, pipelineSpecification);
    }

    private String getDefaultPipelineKey(String language) {
        return SettingsConstants.DEFAULT_PIPELINE + "_" + language;
    }

    private void removeSupportedLanguage(String language) {
        supportedLanguage = null;
        defaultPipelineByLanguage.remove(language);
    }
}
