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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Tag.class, name = "tag"),
        @JsonSubTypes.Type(value = Phrase.class, name = "phrase")
})
public class PartOfTextOccurrence<T> {

    private T element;
    @JsonSerialize(using = SpanSerializer.class)
    private Span span;
    private final List<String> partIds = new ArrayList<>();

    public PartOfTextOccurrence() {
    }

    public PartOfTextOccurrence(T element, int begin, int end) {
        this.element = element;
        this.span = new Span(begin, end);
    }

    public PartOfTextOccurrence(T element, int begin, int end, List<String> partIds) {
        this(element, begin, end);
        if (partIds != null
                && !partIds.isEmpty()) {
            this.partIds.addAll(partIds);
        }
    }

    public T getElement() {
        return element;
    }

    @JsonProperty("span")
    public Span getSpan() {
        return span;
    }

    public List<String> getPartIds() {
        return partIds;
    }
}
