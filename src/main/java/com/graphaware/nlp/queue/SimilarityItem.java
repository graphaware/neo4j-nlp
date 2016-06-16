package com.graphaware.nlp.queue;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author alessandro@graphaware.com
 */
public class SimilarityItem implements Comparable<SimilarityItem> {

    private final long firstNode;
    private final long secondNode;
    private float similarity;
    private String similarityType;

    public SimilarityItem(long firstNode, long secondNode, float sim, String similarityType) {
        this.firstNode = firstNode;
        this.secondNode = secondNode;
        this.similarity = sim;
        this.similarityType = similarityType;
    }

    public long getFirstNode() {
        return firstNode;
    }

    public long getSecondNode() {
        return secondNode;
    }

    public float getSimilarity() {
        return similarity;
    }

    public void setSimilarity(float similarity) {
        this.similarity = similarity;
    }

    public String getSimilarityType() {
        return similarityType;
    }

    public void setSimilarityType(String similarityType) {
        this.similarityType = similarityType;
    }

    public Map<String, Object> getParam() {
        Map<String, Object> param = new HashMap<>();
        param.put("sourceId", firstNode);
        param.put("destId", secondNode);
        param.put("value", similarity);
        return param;
    }

    @Override
    public int compareTo(SimilarityItem o) {
        if (o == null) {
            return 1;
        }
        if (!this.getSimilarityType().equalsIgnoreCase(o.getSimilarityType())) {
            throw new RuntimeException("Similarities cannot be compared since they belong to different similarity types");
        }
        if (this.getSimilarity() > o.getSimilarity()) {
            return 1;
        }
        if (this.getSimilarity() == o.getSimilarity()) {
            return 0;
        }
        return -1;
    }
}
