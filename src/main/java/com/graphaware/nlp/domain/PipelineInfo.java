package com.graphaware.nlp.domain;

import java.util.List;
import java.util.Map;

public class PipelineInfo {

    private final String name;

    private final String textProcessorClass;

    private final Map<String, Object> options;

    private final Map<String, Boolean> specifications;

    private final int numberOfThreads;

    private final List<String> stopwords;

    public PipelineInfo(String name, String textProcessorClass, Map<String, Object> options, Map<String, Boolean> specifications, int numberOfThreads, List<String> stopwords) {
        this.name = name;
        this.textProcessorClass = textProcessorClass;
        this.options = options;
        this.specifications = specifications;
        this.numberOfThreads = numberOfThreads;
        this.stopwords = stopwords;
    }

    public String getName() {
        return name;
    }

    public String getTextProcessorClass() {
        return textProcessorClass;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public Map<String, Boolean> getSpecifications() {
        return specifications;
    }

    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    public List<String> getStopwords() {
        return stopwords;
    }
}
