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
package com.graphaware.nlp.ml.queue;

import java.util.HashMap;
import java.util.Map;

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
