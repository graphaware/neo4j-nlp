/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
