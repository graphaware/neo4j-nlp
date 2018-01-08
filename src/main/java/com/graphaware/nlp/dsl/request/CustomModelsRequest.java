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
package com.graphaware.nlp.dsl.request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CustomModelsRequest {

    private static final Logger LOG = LoggerFactory.getLogger(TextRankRequest.class);

    private String textProcessor;
    private String alg;
    private String modelId;
    private String inputFile;
    private String lang;

    private Map<String, Object> trainingParams;

    private final static String PARAMETER_TEXT_PROCESSOR = "textProcessor";
    private final static String PARAMETER_ALG = "alg";
    private final static String PARAMETER_MODEL_ID = "modelIdentifier";
    private final static String PARAMETER_INPUT_FILE = "inputFile";
    private final static String PARAMETER_LANG = "lang";
    private final static String PARAMETER_TRAINING_PARAMS = "trainingParameters";

    private static final String DEFAULT_LANG = "en";

    public static CustomModelsRequest fromMap(Map<String, Object> request) {
        if (!request.containsKey(PARAMETER_TEXT_PROCESSOR))
            throw new RuntimeException("Missing parameter " + PARAMETER_TEXT_PROCESSOR);
        if (!request.containsKey(PARAMETER_ALG))
            throw new RuntimeException("Missing parameter " + PARAMETER_ALG);
        if (!request.containsKey(PARAMETER_MODEL_ID))
            throw new RuntimeException("Missing parameter " + PARAMETER_MODEL_ID);
        if (!request.containsKey(PARAMETER_INPUT_FILE))
            throw new RuntimeException("Missing parameter " + PARAMETER_INPUT_FILE);

        CustomModelsRequest result = new CustomModelsRequest();
        result.setTextProcessor((String) request.get(PARAMETER_TEXT_PROCESSOR));
        result.setAlg((String) request.get(PARAMETER_ALG));
        result.setModelID((String) request.get(PARAMETER_MODEL_ID));
        result.setInputFile((String) request.get(PARAMETER_INPUT_FILE));
        result.setLanguage((String) request.getOrDefault(PARAMETER_LANG, DEFAULT_LANG));

        if (request.containsKey(PARAMETER_TRAINING_PARAMS)) {
            result.setTrainingParameters((Map) request.get(PARAMETER_TRAINING_PARAMS));
        }

        return result;
    }

    public String getTextProcessor() {
        return this.textProcessor;
    }
    
    private void setTextProcessor(String processor) {
        this.textProcessor = processor;
    }

    public String getAlg() {
        return this.alg;
    }

    private void setAlg(String alg) {
        this.alg = alg;
    }

    public String getModelID() {
        return this.modelId;
    }

    private void setModelID(String id) {
        this.modelId = id;
    }

    public String getInputFile() {
        return this.inputFile;
    }

    private void setInputFile(String file) {
        this.inputFile = file;
    }

    public String getLanguage() {
        return this.lang;
    }

    private void setLanguage(String lang) {
        this.lang = lang;
    }

    public Map<String, Object> getTrainingParameters() {
        return this.trainingParams;
    }

    private void setTrainingParameters(Map<String, Object> params) {
        this.trainingParams = params;
    }

}
