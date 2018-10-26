package com.graphaware.nlp.ml.textrank;

import java.util.List;

class KeywordExtractedItem {

    private final long tagId;
    private int startPosition;
    private int endPosition;
    private String value;
    private double relevance;
    private List<Long> relatedTags;
    private List<Number> relTagStartingPoints;
    private List<Number> relTagEndingPoints;
    private List<String> pos;

    public KeywordExtractedItem(long tagId) {
        this.tagId = tagId;
    }

    public long getTagId() {
        return tagId;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
    }

    public int getEndPosition() {
        return endPosition;
    }

    public void setEndPosition(int endPosition) {
        this.endPosition = endPosition;
    }

    public List<Long> getRelatedTags() {
        return relatedTags;
    }

    public void setRelatedTags(List<Long> relatedTags) {
        this.relatedTags = relatedTags;
    }

    public List<Number> getRelTagStartingPoints() {
        return relTagStartingPoints;
    }

    public void setRelTagStartingPoints(List<Number> relTagStartingPoints) {
        this.relTagStartingPoints = relTagStartingPoints;
    }

    public List<Number> getRelTagEndingPoints() {
        return relTagEndingPoints;
    }

    public void setRelTagEndingPoints(List<Number> relTagEndingPoints) {
        this.relTagEndingPoints = relTagEndingPoints;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public double getRelevance() {
        return relevance;
    }

    public void setRelevance(double relevance) {
        this.relevance = relevance;
    }

    public List<String> getPos() {
        return pos;
    }

    public void setPos(List<String> pos) {
        this.pos = pos;
    }

    public void update(KeywordExtractedItem item) {
        this.relatedTags.addAll(item.getRelatedTags());
        this.relTagStartingPoints.addAll(item.getRelTagStartingPoints());
        this.relTagEndingPoints.addAll(item.getRelTagEndingPoints());
    }
}
