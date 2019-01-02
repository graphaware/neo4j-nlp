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
package com.graphaware.nlp.dsl.request;

import com.graphaware.nlp.ml.textrank.TextRankSummarizer;
import org.neo4j.graphdb.Node;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.graphaware.nlp.dsl.request.RequestConstants.*;

public class SummaryRequest extends AbstractProcedureRequest {

    private static String DEFAULT_TYPE = TextRankSummarizer.TEXT_RANK_SUMMARIZER;
    private static Map<String, Object> DEFAULT_PARAMETERS = new HashMap<>();

    private Node input;
    private String type;
    private Map<String, Object> parameters;

    public SummaryRequest() {
    }

    public SummaryRequest(Node input, String type, Map<String, Object> parameters) {
        this.input = input;
        this.type = type;
        this.parameters = parameters;
    }

    public Node getInput() {
        return input;
    }

    public void setInput(Node input) {
        this.input = input;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    @Override
    public List<String> validMapKeys() {
        return Arrays.asList(
                NODE_KEY,
                TYPE_KEY,
                PARAMETERS_KEY
        );
    }

    @Override
    public List<String> mandatoryKeys() {
        return Arrays.asList(
                NODE_KEY
        );
    }

    public static SummaryRequest fromMap(Map<String, Object> map) {
        SummaryRequest request = new SummaryRequest();
        request.setInput((Node)map.get(NODE_KEY));
        String type = (String)map.get(TYPE_KEY);
        if (type == null) {
            type = DEFAULT_TYPE;
        }
        request.setType(type);
        Map parameters = (Map)map.get(PARAMETERS_KEY);
        if (parameters == null) {
            parameters = DEFAULT_PARAMETERS;
        }
        request.setParameters(parameters);
        request.validateMap(map);
        return request;
    }
}
