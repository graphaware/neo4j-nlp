/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.dsl;

/**
 *
 * @author ale
 */
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
