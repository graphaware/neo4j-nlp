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
package com.graphaware.nlp.dsl;

import static com.graphaware.nlp.enrich.conceptnet5.ConceptNet5Importer.DEFAULT_ADMITTED_RELATIONSHIP;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.neo4j.graphdb.Node;

public class ConceptRequest {

    private final static int DEFAULT_DEPTH = 2;
    private final static String DEFAULT_LANGUAGE = "en";
    private final static boolean DEFAULT_SPLIT_TAG = false;
    private final static boolean DEFAULT_FILTER_BY_LANGUAGE = true;
    public final static List<String> DEFAULT_ADMITTED_POS = Arrays.asList();
    private static final int DEFAULT_RESULTS_LIMIT = 100;

    private int depth = DEFAULT_DEPTH;
    private String language = DEFAULT_LANGUAGE;
    private String processor;
    private boolean splitTag = DEFAULT_SPLIT_TAG;
    private boolean filterByLanguage = DEFAULT_FILTER_BY_LANGUAGE;
    private List<String> admittedRelationships = Arrays.asList(DEFAULT_ADMITTED_RELATIONSHIP);
    private List<String> admittedPos = DEFAULT_ADMITTED_POS;
    private int resultsLimit = DEFAULT_RESULTS_LIMIT;
    
    private Node annotatedNode;
    private Node tag;

    public ConceptRequest() {
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public int getDepth() {
        return depth;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isSplitTag() {
        return splitTag;
    }

    public void setSplitTag(boolean splitTag) {
        this.splitTag = splitTag;
    }

    public boolean isFilterByLanguage() {
        return filterByLanguage;
    }

    public void setFilterByLanguage(boolean filterByLanguage) {
        this.filterByLanguage = filterByLanguage;
    }

    public List<String> getAdmittedRelationships() {
        return admittedRelationships;
    }

    public void setAdmittedRelationships(List<String> admittedRelationships) {
        this.admittedRelationships = admittedRelationships;
    }

    public List<String> getAdmittedPos() {
        return admittedPos;
    }

    public void setAdmittedPos(List<String> admittedPos) {
        this.admittedPos = admittedPos;
    }

    public Node getAnnotatedNode() {
        return annotatedNode;
    }

    public void setAnnotatedNode(Node annotatedNode) {
        this.annotatedNode = annotatedNode;
    }

    public Node getTag() {
        return tag;
    }

    public void setTag(Node tag) {
        this.tag = tag;
    }

    public String getProcessor() {
        return processor;
    }

    public void setProcessor(String processor) {
        this.processor = processor;
    }

    public int getResultsLimit() {
        return resultsLimit;
    }

    public void setResultsLimit(int resultsLimit) {
        this.resultsLimit = resultsLimit;
    }

    public static ConceptRequest fromMap(Map<String, Object> conceptRequest) {
        ConceptRequest request = new ConceptRequest();
        request.setAnnotatedNode((Node)conceptRequest.get("node"));
        request.setTag((Node)conceptRequest.get("tag"));
        if (conceptRequest.containsKey("admittedPos")) {
            request.setAdmittedPos((List<String>)conceptRequest.get("admittedPos"));
        }
        if (conceptRequest.containsKey("admittedPos")) {
            request.setAdmittedRelationships((List<String>)conceptRequest.get("admittedRelationships"));
        }
        if (conceptRequest.containsKey("depth")) {
            request.setDepth(((Long)conceptRequest.get("depth")).intValue());
        }
        if (conceptRequest.containsKey("language")) {
            request.setLanguage((String)conceptRequest.get("language"));
        }
        if (conceptRequest.containsKey("splitTag")) {
            request.setSplitTag((Boolean)conceptRequest.get("splitTag"));
        }
        if (conceptRequest.containsKey("filterByLanguage")) {
            request.setFilterByLanguage((Boolean)conceptRequest.get("filterByLanguage"));
        }
        if (conceptRequest.containsKey("limit")) {
            request.setResultsLimit((int) conceptRequest.get("limit"));
        }
        request.setProcessor((String)conceptRequest.get("processor"));
        return request;
    }
}
