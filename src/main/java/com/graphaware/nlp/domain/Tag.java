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
package com.graphaware.nlp.domain;

import com.graphaware.nlp.util.TypeConverter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

public class Tag {


    private int multiplicity = 1;
    private final String lemma;
    private List<String> posL;
    private List<String> neL;
    private final Collection<TagParentRelation> parents;
    private final String language;
    
    private Map<String, Object> properties = new HashMap<>();

    public Tag(String lemma, String language) {
        this.lemma = lemma.trim();
        this.language = language;
        this.parents = new CopyOnWriteArraySet<>();
    }

    public String getLemma() {
        if ((neL != null && neL.contains("O")) || neL == null) {
            return lemma.toLowerCase();
        } else {
            return lemma;
        }
            
            
    }

    public void setPos(List<String> pos) {
        this.posL = pos;
    }

    public void setNe(List<String> ne) {
        this.neL = ne;
    }

    public int getMultiplicity() {
        return multiplicity;
    }

    public synchronized void incMultiplicity() {
        multiplicity++;
    }

    public void setMultiplicity(int multiplicity) {
        this.multiplicity = multiplicity;
    }

    public List<String> getPosAsList() {
        List<String> values =  posL != null ? posL : new ArrayList<>();

        return values.stream().filter(pos -> { return pos != null; }).collect(Collectors.toList());
    }

    public List<String> getNeAsList() {
        List<String> values =  neL != null ? neL : new ArrayList<>();

        return values.stream().filter(ne -> { return ne != null; }).collect(Collectors.toList());
    }
    
    public String[]  getPosAsArray() {
        return TypeConverter.convertStringListToArray(getPosAsList());
    }
    
    public String[] getNeAsArray() {
        return TypeConverter.convertStringListToArray(getNeAsList());
    }

    public String getId() {
        return getLemma() + "_" + language;
    }

    public String getLanguage() {
        return language;
    }

    public void addParent(String rel, Tag storedTag, float weight) {
        addParent(new TagParentRelation(storedTag, rel, weight));
    }

    public void addParent(String rel, Tag storedTag, float weight, String source) {
        addParent(new TagParentRelation(storedTag, rel, weight, source));
    }

    public void addParent(String rel, Tag storedTag) {
        addParent(new TagParentRelation(storedTag, rel));
    }

    public void addParent(TagParentRelation parentRelationship) {
        parents.add(parentRelationship);
    }

    public void addProperties(String key, Object value) {
        properties.put(key, value);
    }

    public Map<String, Object> getExtraProperties() {
        return properties;
    }

    public Collection<TagParentRelation> getParents() {
        return parents;
    }
}
