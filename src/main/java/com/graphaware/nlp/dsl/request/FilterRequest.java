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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.graphaware.nlp.dsl.request.RequestConstants.*;

public class FilterRequest extends AbstractProcedureRequest {
    
    private String text;
    private String filter;
    private String processor;
    private String pipeline;

    public FilterRequest() {
    }

    
    public FilterRequest(String text, String filter, String processor, String pipeline) {
        this.text = text;
        this.filter = filter;
        this.processor = processor;
        this.pipeline = pipeline;
    }

    @Override
    public List<String> validMapKeys() {
        return Arrays.asList(
                TEXT_KEY,
                FILTER_KEY,
                TEXT_PROCESSOR_KEY,
                PIPELINE_KEY
        );
    }

    @Override
    public List<String> mandatoryKeys() {
        return Arrays.asList(
                TEXT_KEY,
                FILTER_KEY
        );
    }

    public static FilterRequest fromMap(Map<String, Object> map) {
        FilterRequest request = mapper.convertValue(map, FilterRequest.class);
        request.validateMap(map);

        return request;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getProcessor() {
        return processor;
    }

    public void setProcessor(String processor) {
        this.processor = processor;
    }

    public String getPipeline() {
        return pipeline;
    }

    public void setPipeline(String pipeline) {
        this.pipeline = pipeline;
    }
    
    
    
}
