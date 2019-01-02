/*
 * Copyright (c) 2013-2018 GraphAware
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
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Tag {


    private int multiplicity = 1;
    private String lemma;
    private List<String> pos;
    private List<String> ne;
    private Collection<TagParentRelation> parents;
    private String language;
    private String originalValue;
    
    private Map<String, Object> properties = new HashMap<>();

    public Tag() {}

    public Tag(String lemma, String language, String originalValue) {
        this.lemma = lemma;
        this.language = language;
        this.originalValue = originalValue;
        this.parents = new CopyOnWriteArraySet<>();
    }

    public Tag(String lemma, String language) {
        this.lemma = lemma.trim();
        this.language = language;
        this.parents = new CopyOnWriteArraySet<>();
    }

    public String getLemma() {
        if ((ne != null && ne.contains("O")) || ne == null) {
            return lemma.toLowerCase();
        } else {
            return lemma;
        }
            
            
    }

    public void setPos(List<String> pos) {
        this.pos = pos;
    }

    public void setNe(List<String> ne) {
        this.ne = ne;
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

    public List<String> getPos() {
        List<String> values =  pos != null ? pos : new ArrayList<>();

        return values.stream().filter(pos -> { return pos != null; }).collect(Collectors.toList());
    }

    public List<String> getNe() {
        List<String> values =  ne != null ? ne : new ArrayList<>();

        return values.stream().filter(ne -> { return ne != null; }).collect(Collectors.toList());
    }

    @JsonIgnore
    public String[]  getPosAsArray() {
        return TypeConverter.convertStringListToArray(getPos());
    }

    @JsonIgnore
    public String[] getNeAsArray() {
        return TypeConverter.convertStringListToArray(getNe());
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

    public boolean hasNamedEntity() {
        return getNe().size() != 0 && !getNe().contains("O");
    }

    public String getOriginalValue() {
        return originalValue;
    }

    public void setOriginalValue(String originalValue) {
        this.originalValue = originalValue;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }
}
