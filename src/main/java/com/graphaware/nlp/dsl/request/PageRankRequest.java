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

public class PageRankRequest {

    private static final long DEFAULT_ITERATIONS = 30;
    private static final double DEFAULT_DUMPING_FACTOR = 0.85;
    private static final double DEFAULT_THRESHOLD = 0.0001;
    private static final boolean DEFAULT_RESPECT_DIRECTIONS = true;
    private static final String DEFAULT_NODE_TYPE = "Tag";
    private static final String DEFAULT_CO_OCCURRENCE_RELATIONTHIP = "CO_OCCURRENCE";
    private static final String DEFAULT_WEIGHT_PROPERTY = "weight";
    
    private String nodeType = DEFAULT_NODE_TYPE;
    private String relationshipType = DEFAULT_CO_OCCURRENCE_RELATIONTHIP;
    private String relationshipWeight = DEFAULT_WEIGHT_PROPERTY;
    private Long iteration = DEFAULT_ITERATIONS;
    private Double damp = DEFAULT_DUMPING_FACTOR;
    private Double threshold = DEFAULT_THRESHOLD;
    private Boolean respectDirections = DEFAULT_RESPECT_DIRECTIONS;
    
    public PageRankRequest() {
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }

    public String getRelationshipWeight() {
        return relationshipWeight;
    }

    public void setRelationshipWeight(String relationshipWeight) {
        this.relationshipWeight = relationshipWeight;
    }

    public Long getIteration() {
        return iteration;
    }

    public void setIteration(Long iteration) {
        this.iteration = iteration;
    }

    public Double getDamp() {
        return damp;
    }

    public void setDamp(Double damp) {
        this.damp = damp;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public Boolean getRespectDirections() {
        return respectDirections;
    }

    public void setRespectDirections(Boolean dirs) {
        this.respectDirections = dirs;
    }
}
