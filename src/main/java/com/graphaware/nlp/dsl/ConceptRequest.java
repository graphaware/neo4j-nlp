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
import org.neo4j.graphdb.Node;

public class ConceptRequest {

    private final static int DEFAULT_DEPTH = 2;
    private final static String DEFAULT_LANGUAGE = "en";
    private final static boolean DEFAULT_SPLIT_TAG = false;
    private final static boolean DEFAULT_FILTER_BY_LANGUAGE = true;
    public  final static List<String> DEFAULT_ADMITTED_POS = Arrays.asList();

    private int depth = DEFAULT_DEPTH;
    private String language = DEFAULT_LANGUAGE;
    private boolean splitTag = DEFAULT_SPLIT_TAG;
    private boolean filterByLanguage = DEFAULT_FILTER_BY_LANGUAGE;
    private List<String> admittedRelationships = Arrays.asList(DEFAULT_ADMITTED_RELATIONSHIP);
    private List<String> admittedPos = DEFAULT_ADMITTED_POS;
    

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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public Node getTag() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public String getProcessor() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
}
