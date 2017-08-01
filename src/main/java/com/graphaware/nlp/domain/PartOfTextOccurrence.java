/*
 * Copyright (c) 2013-2016 GraphAware
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

import com.graphaware.common.util.Pair;

import java.util.ArrayList;
import java.util.List;

class PartOfTextOccurrence<T> {

    private final T element;
    private final Pair<Integer, Integer> span;
    private final List<String> partIds = new ArrayList<>();

    public PartOfTextOccurrence(T element, int begin, int end) {
        this.element = element;
        this.span = new Pair<>(begin, end);
    }

    public PartOfTextOccurrence(T element, int begin, int end, List<String> partIds) {
        this.element = element;
        this.span = new Pair<>(begin, end);
        this.partIds.addAll(partIds);
    }

    public T getElement() {
        return element;
    }

    public Pair<Integer, Integer> getSpan() {
        return span;
    }

    public List<String> getPartIds() {
        return partIds;
    }
}
