package com.graphaware.nlp.processor;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class PipelineSpecification {

    private String name;

    private String textProcessor;

    public static PipelineSpecification fromMap(Map<String, Object> map) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(map, PipelineSpecification.class);
    }

    public PipelineSpecification() {

    }

    public String getName() {
        return name;
    }

    public String getTextProcessor() {
        return textProcessor;
    }
}
