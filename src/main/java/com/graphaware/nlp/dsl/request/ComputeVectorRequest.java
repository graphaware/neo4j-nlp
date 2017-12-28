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
import org.neo4j.graphdb.Node;

public class ComputeVectorRequest extends AbstractProcedureRequest {
    
    private Node input;
    private String query;
    private String propertyName;
    
    public ComputeVectorRequest() {
    }

    public ComputeVectorRequest(Node input, String query, String propertyName) {
        this.input = input;
        this.query = query;
        this.propertyName = propertyName;
    }

    public Node getInput() {
        return input;
    }

    public void setInput(Node input) {
        this.input = input;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }
    
    

    
    @Override
    public List<String> validMapKeys() {
        return Arrays.asList(
                NODE_KEY,
                QUERY_KEY,
                PROPERTY_KEY
        );
    }

    @Override
    public List<String> mandatoryKeys() {
        return Arrays.asList(
                NODE_KEY
        );
    }

    public static ComputeVectorRequest fromMap(Map<String, Object> map) {
        ComputeVectorRequest request = new ComputeVectorRequest();
        request.setInput((Node)map.get(NODE_KEY));
        request.setQuery((String)map.get(QUERY_KEY));
        request.setPropertyName((String)map.get(PROPERTY_KEY));        
        request.validateMap(map);

        return request;
    }
}
