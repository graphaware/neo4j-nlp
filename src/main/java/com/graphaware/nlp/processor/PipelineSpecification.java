package com.graphaware.nlp.processor;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PipelineSpecification {

    private String name;

    private String textProcessor;

    private Map<String, Boolean> processingSteps = new HashMap<>();

    private String stopwords;

    private long threadNumber;

    public static PipelineSpecification fromMap(Map<String, Object> map) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(map, PipelineSpecification.class);
    }

    public PipelineSpecification() {

    }

    public PipelineSpecification(String name, String textProcessor) {
        this.name = name;
        this.textProcessor = textProcessor;
    }

    public String getName() {
        return name;
    }

    public String getTextProcessor() {
        return textProcessor;
    }

    public boolean hasProcessingStep(String stepName) {
        return processingSteps.containsKey(stepName) && processingSteps.get(stepName);
    }

    public boolean hasProcessingStep(String stepName, boolean defaultValue) {
        if (processingSteps.containsKey(stepName)) {
            return processingSteps.get(stepName);
        }

        return defaultValue;
    }

    public void addProcessingStep(String step) {
        processingSteps.put(step, true);
    }

    public String getStopwords() {
        return stopwords;
    }

    public void setStopwords(String stopwords) {
        this.stopwords = stopwords;
    }

    public long getThreadNumber() {
        return threadNumber;
    }

    public void setThreadNumber(long threadNumber) {
        this.threadNumber = threadNumber;
    }
}
