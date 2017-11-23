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

public class TagParentRelation {
    private Tag parent;
    private String relation;
    private float weight;
    private String source;

    public TagParentRelation(Tag parent, String relation) {
        this.parent = parent;
        this.relation = relation;
    }
    
    public TagParentRelation(Tag parent, String relation, float weight) {
        this.parent = parent;
        this.relation = relation;
        this.weight = weight;
    }

    public TagParentRelation(Tag parent, String relation, float weight, String source) {
        this.parent = parent;
        this.relation = relation;
        this.weight = weight;
        this.source = source;
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

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
