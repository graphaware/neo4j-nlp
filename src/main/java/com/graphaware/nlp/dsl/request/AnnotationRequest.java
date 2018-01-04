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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.graphaware.nlp.dsl.request.RequestConstants.*;

public class AnnotationRequest extends AbstractProcedureRequest {

    private String text;

    private Object id;

    private String textProcessor;

    private String pipeline;

    private boolean force;

    private boolean checkLanguage = true;

    private List<String> annotators = new ArrayList<>();

    private List<String> excludedNER = new ArrayList<>();

    private List<String> excludedPOS = new ArrayList<>();

    public AnnotationRequest() {

    }

    public AnnotationRequest(String text, Object id, String textProcessor, String pipeline, boolean force, boolean checkLanguage) {
        this.text = text;
        this.id = id;
        this.textProcessor = textProcessor;
        this.pipeline = pipeline;
        this.force = force;
        this.checkLanguage = checkLanguage;
    }

    @Override
    public List<String> validMapKeys() {
        return Arrays.asList(
                ID_KEY,
                TEXT_PROCESSOR_KEY,
                TEXT_KEY,
                PIPELINE_KEY,
                FORCE_KEY,
                CHECK_LANGUAGE_KEY,
                ANNOTATORS,
                EXCLUDED_NER,
                EXCLUDED_POS
        );
    }

    @Override
    public List<String> mandatoryKeys() {
        return Arrays.asList(
                TEXT_KEY,
                ID_KEY
        );
    }

    public static AnnotationRequest fromMap(Map<String, Object> map) {
        AnnotationRequest request = mapper.convertValue(map, AnnotationRequest.class);
        request.validateMap(map);

        return request;
    }

    public String getText() {
        return text;
    }

    public String getId() {
        return String.valueOf(id);
    }

    public String getTextProcessor() {
        return textProcessor;
    }

    public String getPipeline() {
        return pipeline;
    }

    public boolean isForce() {
        return force;
    }

    public boolean isCheckLanguage() {
        return checkLanguage;
    }

    public boolean shouldCheckLanguage() {
        return checkLanguage;
    }

    public List<String> getAnnotators() {
        return annotators;
    }

    public List<String> getExcludedNER() {
        return excludedNER;
    }

    public List<String> getExcludedPOS() {
        return excludedPOS;
    }
}
