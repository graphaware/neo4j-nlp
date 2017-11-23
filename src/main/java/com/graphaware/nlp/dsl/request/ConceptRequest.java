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

import com.graphaware.nlp.enrich.conceptnet5.ConceptNet5Enricher;
import org.neo4j.graphdb.Node;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.graphaware.nlp.dsl.request.RequestConstants.*;
import static com.graphaware.nlp.enrich.conceptnet5.ConceptNet5Importer.DEFAULT_ADMITTED_RELATIONSHIP;

public class ConceptRequest extends AbstractProcedureRequest {

    private final static int DEFAULT_DEPTH = 2;
    private final static String DEFAULT_LANGUAGE = "en";
    private final static boolean DEFAULT_SPLIT_TAG = false;
    private final static boolean DEFAULT_FILTER_BY_LANGUAGE = true;
    public final static List<String> DEFAULT_ADMITTED_POS = Arrays.asList();
    private static final int DEFAULT_RESULTS_LIMIT = 100;
    private static final String DEFAULT_ENRICHER = ConceptNet5Enricher.ENRICHER_NAME;

    private int depth = DEFAULT_DEPTH;
    private String language = DEFAULT_LANGUAGE;
    private String processor;
    private boolean splitTag = DEFAULT_SPLIT_TAG;
    private boolean filterByLanguage = DEFAULT_FILTER_BY_LANGUAGE;
    private List<String> admittedRelationships = Arrays.asList(DEFAULT_ADMITTED_RELATIONSHIP);
    private List<String> admittedPos = DEFAULT_ADMITTED_POS;
    private int resultsLimit = DEFAULT_RESULTS_LIMIT;
    private String enricherName = DEFAULT_ENRICHER;
    
    private Node annotatedNode;
    private Node tag;

    public ConceptRequest() {
    }

    @Override
    public List<String> validMapKeys() {
        return Arrays.asList(
                NODE_KEY,
                TAG_KEY,
                DEPTH_KEY,
                SPLIT_TAGS_KEY,
                FILTER_BY_LANGUAGE_KEY,
                ADMITTED_PART_OF_SPEECH_KEY,
                ADMITTED_RELATIONSHIPS_KEY,
                LIMIT_KEY,
                TEXT_PROCESSOR_KEY,
                LANGUAGE_KEY,
                ENRICHER_KEY
        );
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

    public String getEnricherName() {
        return enricherName;
    }

    public void setEnricherName(String enricherName) {
        this.enricherName = enricherName;
    }

    public static ConceptRequest fromMap(Map<String, Object> conceptRequest) {

        ConceptRequest request = new ConceptRequest();
        request.validateMap(conceptRequest);
        request.validateRequestHasKeyOrOtherKey(NODE_KEY, TAG_KEY, conceptRequest);

        request.setAnnotatedNode((Node) conceptRequest.get(NODE_KEY));
        request.setTag((Node) conceptRequest.get(TAG_KEY));
        if (conceptRequest.containsKey(ADMITTED_PART_OF_SPEECH_KEY)) {
            request.setAdmittedPos((List<String>) conceptRequest.get(ADMITTED_PART_OF_SPEECH_KEY));
        }
        if (conceptRequest.containsKey(ADMITTED_RELATIONSHIPS_KEY)) {
            request.setAdmittedRelationships((List<String>) conceptRequest.get(ADMITTED_RELATIONSHIPS_KEY));
        }
        if (conceptRequest.containsKey(DEPTH_KEY)) {
            request.setDepth(((Long) conceptRequest.get(DEPTH_KEY)).intValue());
        }
        if (conceptRequest.containsKey(LANGUAGE_KEY)) {
            request.setLanguage((String) conceptRequest.get(LANGUAGE_KEY));
        }
        if (conceptRequest.containsKey(SPLIT_TAGS_KEY)) {
            request.setSplitTag((Boolean) conceptRequest.get(SPLIT_TAGS_KEY));
        }
        if (conceptRequest.containsKey(FILTER_BY_LANGUAGE_KEY)) {
            request.setFilterByLanguage((Boolean) conceptRequest.get(FILTER_BY_LANGUAGE_KEY));
        }
        if (conceptRequest.containsKey(LIMIT_KEY)) {
            request.setResultsLimit( (int) conceptRequest.get(LIMIT_KEY));
        }
        if (conceptRequest.containsKey(ENRICHER_KEY)) {
            request.setEnricherName(conceptRequest.get(ENRICHER_KEY).toString());
        }

        request.setProcessor((String) conceptRequest.get(TEXT_PROCESSOR_KEY));
        return request;
    }
}
