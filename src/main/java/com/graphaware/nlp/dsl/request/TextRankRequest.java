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

import java.util.Map;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextRankRequest {

    private static final Logger LOG = LoggerFactory.getLogger(TextRankRequest.class);

    //private final static String PARAMETER_NAME_QUERY = "query";
    private final static String PARAMETER_ANNOTATED_TEXT = "annotatedText";
    private final static String PARAMETER_ITERATIONS = "iterations";
    private final static String PARAMETER_DAMPING_FACTOR = "damp";
    private final static String PARAMETER_DAMPING_THRESHOLD = "threshold";
    private final static String PARAMETER_STOPWORDS = "stopwords";
    private final static String PARAMETER_DO_STOPWORDS = "removeStopwords";
    private final static String PARAMETER_RESPECT_DIRECTIONS = "respectDirections";
    private final static String PARAMETER_RESPECT_SENTENCES = "respectSentences";
    private final static String PARAMETER_USE_TFIDF_WEIGHTS = "useTfIdfWeights";
    private final static String PARAMETER_USE_DEPENDENCIES = "useDependencies";
    private final static String PARAMETER_COOCCURRENCE_WINDOW = "cooccurrenceWindow";
    private final static String PARAMETER_MAX_SINGLE_KEYWORDS = "maxSinglewordKeywords";
    private final static String PARAMETER_PHRASES_TOPX = "topXWordsForPhrases";
    private final static String PARAMETER_SINGLES_TOPX = "topXSinglewordKeywords";
    private final static String PARAMETER_KEYWORD_LABEL = "keywordLabel";

    private Node node;
    private int iterations;
    private double damp;
    private double threshold;
    private boolean doStopwords;
    private boolean respectDirections;
    private boolean respectSentences;
    private boolean useTfIdfWeights;
    private boolean useDependencies;
    private int cooccurrenceWindow;
    private int maxSingles;
    private double phrases_topx;
    private double singles_topx;
    private String keywordLabel;
    private String stopWords;    

    private static final int DEFAULT_ITERATIONS = 30;
    private static final double DEFAULT_DUMPING_FACTOR = 0.85;
    private static final double DEFAULT_THRESHOLD = 0.0001;
    private static final boolean DEFAULT_STOPWORDS_ENABLING = false;
    private static final boolean DEFAULT_RESPECT_DIRECTIONS = false;
    private static final boolean DEFAULT_RESPECT_SENTENCES = false;
    private static final boolean DEFAULT_USE_TFIDF_WEIGHTS = false;
    private static final boolean DEFAULT_USE_DEPENDENCIES = false;
    private static final int DEFAULT_COOCCURRENCE_WINDOW = 2;
    private static final int DEFAULT_MAX_SINGLE_KEYWORDS = 50;
    private static final double DEFAULT_PHRASES_TOPX = 1.0f/3;
    private static final double DEFAULT_SINGLES_TOPX = 1.0f/5;
    private static final String DEFAULT_KEYWORD_LABEL = "Keyword";

    public static TextRankRequest fromMap(Map<String, Object> textRankRequest) {
        if (!textRankRequest.containsKey(PARAMETER_ANNOTATED_TEXT)) {
            throw new RuntimeException("Missing parameter annotatedText");
        }
        TextRankRequest result = new TextRankRequest();
        result.setNode((Node) textRankRequest.get(PARAMETER_ANNOTATED_TEXT));
        result.setIterations(((Number)textRankRequest.getOrDefault(PARAMETER_ITERATIONS, DEFAULT_ITERATIONS)).intValue());
        result.setDamp(((Number) textRankRequest.getOrDefault(PARAMETER_DAMPING_FACTOR, DEFAULT_DUMPING_FACTOR)).doubleValue());
        result.setThreshold(((Number) textRankRequest.getOrDefault(PARAMETER_DAMPING_THRESHOLD, DEFAULT_THRESHOLD)).doubleValue());
        result.setDoStopwords((boolean) textRankRequest.getOrDefault(PARAMETER_DO_STOPWORDS, DEFAULT_STOPWORDS_ENABLING));
        result.setRespectDirections((boolean) textRankRequest.getOrDefault(PARAMETER_RESPECT_DIRECTIONS, DEFAULT_RESPECT_DIRECTIONS));
        result.setRespectSentences((boolean) textRankRequest.getOrDefault(PARAMETER_RESPECT_SENTENCES, DEFAULT_RESPECT_SENTENCES));
        result.setUseTfIdfWeights((boolean) textRankRequest.getOrDefault(PARAMETER_USE_TFIDF_WEIGHTS, DEFAULT_USE_TFIDF_WEIGHTS));
        result.setUseDependencies((boolean) textRankRequest.getOrDefault(PARAMETER_USE_DEPENDENCIES, DEFAULT_USE_DEPENDENCIES));
        result.setCooccurrenceWindow(((Number) textRankRequest.getOrDefault(PARAMETER_COOCCURRENCE_WINDOW, DEFAULT_COOCCURRENCE_WINDOW)).intValue());
        result.setMaxSingleKeywords(((Number) textRankRequest.getOrDefault(PARAMETER_MAX_SINGLE_KEYWORDS, DEFAULT_MAX_SINGLE_KEYWORDS)).intValue());
        result.setTopXWordsForPhrases(((Number) textRankRequest.getOrDefault(PARAMETER_PHRASES_TOPX, DEFAULT_PHRASES_TOPX)).doubleValue());
        result.setTopXSinglewordKeywords(((Number) textRankRequest.getOrDefault(PARAMETER_SINGLES_TOPX, DEFAULT_SINGLES_TOPX)).doubleValue());
        result.setKeywordLabel((String) textRankRequest.getOrDefault(PARAMETER_KEYWORD_LABEL, DEFAULT_KEYWORD_LABEL));

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

    public boolean isUseDependencies() {
        return useDependencies;
    }

    public void setUseDependencies(boolean useDependencies) {
        this.useDependencies = useDependencies;
    }

    public int getCooccurrenceWindow() {
        return cooccurrenceWindow;
    }

    public void setCooccurrenceWindow(int cooccurrenceWindow) {
        this.cooccurrenceWindow = cooccurrenceWindow;
    }

    public int getMaxSingleKeywords() {
        return maxSingles;
    }

    public void setMaxSingleKeywords(int n) {
        this.maxSingles = n;
    }

    public double getTopXWordsForPhrases() {
        return this.phrases_topx;
    }

    public void setTopXWordsForPhrases(double n) {
        this.phrases_topx = n;
    }

    public double getTopXSinglewordKeywords() {
        return this.singles_topx;
    }

    public void setTopXSinglewordKeywords(double n) {
        this.singles_topx = n;
    }

    public String getKeywordLabel() {
        return keywordLabel;
    }

    public void setKeywordLabel(String label) {
        this.keywordLabel = label;
    }

    public String getStopWords() {
        return stopWords;
    }

    public void setStopWords(String stopWords) {
        this.stopWords = stopWords;
    }
}
