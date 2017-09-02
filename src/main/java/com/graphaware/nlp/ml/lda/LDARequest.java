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
package com.graphaware.nlp.ml.lda;

public class LDARequest {

    private String query;
    private String topicLabel;
    private int clusters;
    private int itarations;
    private int topicWords;
    private boolean storeModel = false;

    public LDARequest() {
    }
    
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getClusters() {
        return clusters;
    }

    public void setClusters(int clusters) {
        this.clusters = clusters;
    }

    public int getItarations() {
        return itarations;
    }

    public void setItarations(int itarations) {
        this.itarations = itarations;
    }

    public int getTopicWords() {
        return topicWords;
    }

    public void setTopicWords(int topicWords) {
        this.topicWords = topicWords;
    }

    public boolean isStoreModel() {
        return storeModel;
    }

    public void setStoreModel(boolean storeModel) {
        this.storeModel = storeModel;
    }

    public String getTopicLabel() {
        return topicLabel;
    }

    public void setTopicLabel(String topicLabel) {
        this.topicLabel = topicLabel;
    }
}
