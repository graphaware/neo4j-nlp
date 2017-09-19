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

public class TypedDependency {

    private final String source;

    private final String target;

    private final String name;

    private final String specific;

    public TypedDependency(String source, String target, String name, String specific) {
        this.source = source;
        this.target = target;
        this.name = name;
        this.specific = specific;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public String getName() {
        return name;
    }

    public String getSpecific() {
        return specific;
    }
}
