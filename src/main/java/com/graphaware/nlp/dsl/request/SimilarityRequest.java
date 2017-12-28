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

public class SimilarityRequest extends AbstractProcedureRequest {

    private final static int DEFAULT_K_SIZE = 500;
    private final static String SIMILARITY_TYPE = "SIMILARITY_COSINE";

    private List<Node> input;
    private Long depth;
    private String query;
    private String relationshipType;
    private String propertyName;
    private int kSize;

    public SimilarityRequest() {
    }

    public List<Node> getInput() {
        return input;
    }

    public void setInput(List<Node> input) {
        this.input = input;
    }

    public Long getDepth() {
        return depth;
    }

    public void setDepth(Long depth) {
        this.depth = depth;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
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
                INPUT_KEY,
                QUERY_KEY,
                PROPERTY_KEY,
                DEPTH_KEY,
                RELATIONSHIP_TYPE_KEY,
                K_SIZE_KEY
        );
    }

    @Override
    public List<String> mandatoryKeys() {
        return Arrays.asList(
                INPUT_KEY
        );
    }

    public static SimilarityRequest fromMap(Map<String, Object> map) {
        SimilarityRequest request = new SimilarityRequest();
        request.setInput((List) map.get(INPUT_KEY));
        request.setQuery((String) map.get(QUERY_KEY));
        request.setPropertyName((String) map.get(PROPERTY_KEY));
        request.setDepth((Long) map.get(DEPTH_KEY));
        if (map.containsKey(RELATIONSHIP_TYPE_KEY)) {
            request.setRelationshipType((String) map.get(RELATIONSHIP_TYPE_KEY));
        } else {
            request.setRelationshipType(SIMILARITY_TYPE);
        }
        if (map.containsKey(K_SIZE_KEY)) {
            Long value = (Long) map.get(K_SIZE_KEY);
            request.setkSize(value.intValue());
        } else {
            request.setkSize(DEFAULT_K_SIZE);
        }
        request.validateMap(map);
        return request;
    }

    public int getkSize() {
        return kSize;
    }

    public void setkSize(int kSize) {
        this.kSize = kSize;
    }

}
