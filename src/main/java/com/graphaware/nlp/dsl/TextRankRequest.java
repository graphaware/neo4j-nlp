/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.dsl;

import java.util.Map;
import org.neo4j.graphdb.Node;

/**
 *
 * @author ale
 */
public class TextRankRequest {

    //private final static String PARAMETER_NAME_QUERY = "query";
    private final static String PARAMETER_ANNOTATED_TEXT = "annotatedText";
    private final static String PARAMETER_ITERATIONS = "iter";
    private final static String PARAMETER_DAMPING_FACTOR = "damp";
    private final static String PARAMETER_DAMPING_THRESHOLD = "threshold";
    private final static String PARAMETER_STOPWORDS = "stopwords";
    private final static String PARAMETER_DO_STOPWORDS = "removeStopWords";
    private final static String PARAMETER_RESPECT_DIRECTIONS = "respectDirections";
    private final static String PARAMETER_RESPECT_SENTENCES = "respectSentences";
    private final static String PARAMETER_USE_TFIDF_WEIGHTS = "useTfIdfWeights";
    private final static String PARAMETER_COOCCURRENCE_WINDOW = "cooccurrenceWindow";

    private Node node;
    private int iterations;
    private double damp;
    private double threshold;
    private boolean doStopwords;
    private boolean respectDirections;
    private boolean respectSentences;
    private boolean useTfIdfWeights;
    private int cooccurrenceWindow;    
    private String stopWords;    

    private static final long DEFAULT_ITERATIONS = 30;
    private static final double DEFAULT_DUMPING_FACTOR = 0.85;
    private static final double DEFAULT_THRESHOLD = 0.0001;
    private static final boolean DEFAULT_STOPWORDS_ENABLING = false;
    private static final boolean DEFAULT_RESPECT_DIRECTIONS = false;
    private static final boolean DEFAULT_RESPECT_SENTENCES = false;
    private static final boolean DEFAULT_USE_TFIDF_WEIGHTS = false;
    private static final long DEFAULT_COOCCURRENCE_WINDOW = 2;

    public static TextRankRequest fromMap(Map<String, Object> textRankRequest) {
        if (!textRankRequest.containsKey(PARAMETER_ANNOTATED_TEXT)) {
            throw new RuntimeException("Missing parameter annotatedText");
        }
        TextRankRequest result = new TextRankRequest();
        result.setNode((Node) textRankRequest.get(PARAMETER_ANNOTATED_TEXT));
        result.setIterations((Integer)textRankRequest.getOrDefault(PARAMETER_ITERATIONS, DEFAULT_ITERATIONS));
        result.setDamp((double) textRankRequest.getOrDefault(PARAMETER_DAMPING_FACTOR, DEFAULT_DUMPING_FACTOR));
        result.setThreshold((double) textRankRequest.getOrDefault(PARAMETER_DAMPING_THRESHOLD, DEFAULT_THRESHOLD));
        result.setDoStopwords((boolean) textRankRequest.getOrDefault(PARAMETER_DO_STOPWORDS, DEFAULT_STOPWORDS_ENABLING));
        result.setRespectDirections((boolean) textRankRequest.getOrDefault(PARAMETER_RESPECT_DIRECTIONS, DEFAULT_RESPECT_DIRECTIONS));
        result.setRespectSentences((boolean) textRankRequest.getOrDefault(PARAMETER_RESPECT_SENTENCES, DEFAULT_RESPECT_SENTENCES));
        result.setUseTfIdfWeights((boolean) textRankRequest.getOrDefault(PARAMETER_USE_TFIDF_WEIGHTS, DEFAULT_USE_TFIDF_WEIGHTS));
        result.setCooccurrenceWindow(((Integer) textRankRequest.getOrDefault(PARAMETER_COOCCURRENCE_WINDOW, DEFAULT_COOCCURRENCE_WINDOW)));

        if (textRankRequest.containsKey(PARAMETER_STOPWORDS)) {
            result.setStopWords((String) textRankRequest.get(PARAMETER_STOPWORDS));
        }
        return result;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public double getDamp() {
        return damp;
    }

    public void setDamp(double damp) {
        this.damp = damp;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public boolean isDoStopwords() {
        return doStopwords;
    }

    public void setDoStopwords(boolean doStopwords) {
        this.doStopwords = doStopwords;
    }

    public boolean isRespectDirections() {
        return respectDirections;
    }

    public void setRespectDirections(boolean respectDirections) {
        this.respectDirections = respectDirections;
    }

    public boolean isRespectSentences() {
        return respectSentences;
    }

    public void setRespectSentences(boolean respectSentences) {
        this.respectSentences = respectSentences;
    }

    public boolean isUseTfIdfWeights() {
        return useTfIdfWeights;
    }

    public void setUseTfIdfWeights(boolean useTfIdfWeights) {
        this.useTfIdfWeights = useTfIdfWeights;
    }

    public int getCooccurrenceWindow() {
        return cooccurrenceWindow;
    }

    public void setCooccurrenceWindow(int cooccurrenceWindow) {
        this.cooccurrenceWindow = cooccurrenceWindow;
    }

    public String getStopWords() {
        return stopWords;
    }

    public void setStopWords(String stopWords) {
        this.stopWords = stopWords;
    }
}
