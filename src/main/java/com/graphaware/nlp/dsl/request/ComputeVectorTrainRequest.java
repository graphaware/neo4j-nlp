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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.graphaware.nlp.dsl.request.RequestConstants.*;
import java.util.HashMap;

public class ComputeVectorTrainRequest extends AbstractProcedureRequest {

    private static String DEFAULT_TYPE = "query";
    private static Map<String, Object> DEFAULT_PARAMETERS = new HashMap<>();
    
    private String type;
    private Map<String, Object> parameters;

    public ComputeVectorTrainRequest() {
    }
    
    public ComputeVectorTrainRequest(String type, Map<String, Object> parameters) {
        this.type = type;
        this.parameters = parameters;
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
                TYPE_KEY,
                PARAMETERS_KEY
        );
    }

    @Override
    public List<String> mandatoryKeys() {
        return Arrays.asList(
        );
    }

    public static ComputeVectorTrainRequest fromMap(Map<String, Object> map) {
        ComputeVectorTrainRequest request = new ComputeVectorTrainRequest();
        String type = (String)map.get(TYPE_KEY);
        if (type == null) {
            type = DEFAULT_TYPE;
        }
        
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
