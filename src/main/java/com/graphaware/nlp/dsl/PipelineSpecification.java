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
package com.graphaware.nlp.dsl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

public class PipelineSpecification {
    private String name;

    private String textProcessor;

    private Map<String, Boolean> processingSteps = new HashMap<>();

    private String stopwords;

    private long threadNumber;

    public PipelineSpecification() {

    }
    
    public static PipelineSpecification fromMap(Map<String, Object> map) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(map, PipelineSpecification.class);
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
