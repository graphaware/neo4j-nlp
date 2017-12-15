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

import java.util.List;

public class TagOccurrence extends PartOfTextOccurrence<Tag> {
    private final String value;
    
    public TagOccurrence(Tag element, int begin, int end, String value) {
        this(element, begin, end, value, null);
    }

    public TagOccurrence(Tag element, int begin, int end, String value, List<String> partIds) {
        super(element, begin, end, partIds);
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
