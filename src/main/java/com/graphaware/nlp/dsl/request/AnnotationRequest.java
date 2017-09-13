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

public class AnnotationRequest {

    private String text;

    private Object id;

    private String textProcessor;

    private String pipeline;

    private boolean force;

    private boolean checkLanguage = true;

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
}
