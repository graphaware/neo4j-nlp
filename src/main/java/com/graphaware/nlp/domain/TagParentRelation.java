/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.domain;

/**
 *
 * @author ale
 */
public class TagParentRelation {
    private Tag parent;
    private String relation;

    public TagParentRelation(Tag parent, String relation) {
        this.parent = parent;
        this.relation = relation;
    }

    public Tag getParent() {
        return parent;
    }

    public void setParent(Tag parent) {
        this.parent = parent;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }
    
}
