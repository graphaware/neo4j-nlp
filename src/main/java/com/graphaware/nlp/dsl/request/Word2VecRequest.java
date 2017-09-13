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

import org.neo4j.graphdb.Node;

import java.util.Map;

public class Word2VecRequest {

    private static final String PARAMETER_NAME_ANNOTATED_TEXT = "node";
    private static final String PARAMETER_NAME_SPLIT_TAG = "splitTag";
    private static final String PARAMETER_NAME_FILTER_LANG = "filterLang";
    private static final String PARAMETER_NAME_LANG = "lang";
    private static final String PARAMETER_MODEL_NAME = "modelName";
    private static final String PARAMETER_PROPERTY_NAME = "propertyName";
    private static final String PARAMETER_PROPERTY_QUERY = "query";
    private static final String PARAMETER_NAME_TAG = "tag";
    private static final String PARAMETER_NAME_TEXT_PROCESSOR = "textProcessor";

    private static final String DEFAULT_PROPERTY_NAME = "word2vec";
    private final static String DEFAULT_LANGUAGE = "en";
    private static final boolean DEFAULT_SPLIT_TAG = false;
    private static final boolean DEFAULT_FILTER_LANG = true;

    private Node annotatedNode;
    private Node tagNode;
    private Boolean splitTags;
    private Boolean filterByLang;
    private String lang;
    private String query;
    private String modelName;
    private String propertyName;
    private String processor;

    public static Word2VecRequest fromMap(Map<String, Object> word2VecRankRequest) {
        Word2VecRequest result = new Word2VecRequest();
        result.setAnnotatedNode((Node) word2VecRankRequest.get(PARAMETER_NAME_ANNOTATED_TEXT));
        result.setTagNode((Node) word2VecRankRequest.get(PARAMETER_NAME_TAG));
        result.setSplitTags((Boolean) word2VecRankRequest.getOrDefault(PARAMETER_NAME_SPLIT_TAG, DEFAULT_SPLIT_TAG));
        result.setFilterByLang((Boolean) word2VecRankRequest.getOrDefault(PARAMETER_NAME_FILTER_LANG, DEFAULT_FILTER_LANG));
        result.setLang((String) word2VecRankRequest.getOrDefault(PARAMETER_NAME_LANG, DEFAULT_LANGUAGE));
        result.setQuery((String) word2VecRankRequest.get(PARAMETER_PROPERTY_QUERY));
        result.setModelName((String) word2VecRankRequest.get(PARAMETER_MODEL_NAME));
        result.setPropertyName((String) word2VecRankRequest.getOrDefault(PARAMETER_PROPERTY_NAME, DEFAULT_PROPERTY_NAME));
        result.setProcessor((String) word2VecRankRequest.getOrDefault(PARAMETER_NAME_TEXT_PROCESSOR, ""));
        return result;
    }

    public Node getAnnotatedNode() {
        return annotatedNode;
    }

    public void setAnnotatedNode(Node annotatedNode) {
        this.annotatedNode = annotatedNode;
    }

    public Boolean getSplitTags() {
        return splitTags;
    }

    public void setSplitTags(Boolean splitTags) {
        this.splitTags = splitTags;
    }

    public Boolean getFilterByLang() {
        return filterByLang;
    }

    public void setFilterByLang(Boolean filterByLang) {
        this.filterByLang = filterByLang;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public Node getTagNode() {
        return tagNode;
    }

    public void setTagNode(Node tagNode) {
        this.tagNode = tagNode;
    }

    public String getProcessor() {
        return processor;
    }

    public void setProcessor(String processor) {
        this.processor = processor;
    }
    
}
