/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.domain;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

/**
 *
 * @author ale
 */
public class Tag {
    
    private int multiplicity;
    private final String lemma;
    private String pos;
    private String ne;
    private Collection<TagParentRelation> parents;
    
    public Tag(String lemma) {
        this.lemma = lemma;
    }

    public String getLemma() {
        return lemma;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }

    public void setNe(String ne) {
        this.ne = ne;
    }

    public int getMultiplicity() {
        return multiplicity;
    }

    public void incMultiplicity() {
        multiplicity++;
    }

    public String getPos() {
        return pos;
    }

    public String getNe() {
        return ne;
    }

    public void addParent(String rel, Tag storedTag) {
        if (parents == null)
            parents = new TreeSet<>();
        parents.add(new TagParentRelation(storedTag, rel));
    }
}
