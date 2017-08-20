/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.dsl;

import static com.graphaware.nlp.conceptnet5.ConceptNet5Importer.DEFAULT_ADMITTED_RELATIONSHIP;
import java.util.Arrays;
import java.util.List;
import org.neo4j.graphdb.Node;

/**
 *
 * @author ale
 */
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
