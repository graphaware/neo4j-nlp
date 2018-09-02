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

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TagOccurrence extends PartOfTextOccurrence<Tag> {

    private String value;

    private OptimizedCoreference coreference;

    private double confidence;
    
    public TagOccurrence(Tag element, int begin, int end, String value) {
        this(element, begin, end, value, null);
    }

    @JsonCreator
    public TagOccurrence(
            @JsonProperty("element") Tag element,
            @JsonProperty("begin") int begin,
            @JsonProperty("end") int end,
            @JsonProperty("value") String value,
            @JsonProperty("partIds") List<String> partIds) {
        super(element, begin, end, partIds);
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean hasNamedEntity() {
        return getElement().hasNamedEntity();
    }

    public String getNamedEntity() {
        return getElement().getNe().get(0);
    }

    public boolean hasReference() {
        return coreference != null;
    }

    public OptimizedCoreference getCoreference() {
        return coreference;
    }

    public void setCoreference(OptimizedCoreference coreference) {
        this.coreference = coreference;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
}
