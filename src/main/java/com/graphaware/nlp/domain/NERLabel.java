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

import org.neo4j.graphdb.Label;

/**
 * All labels used in the project.
 */
public class NERLabel implements Label {

    private static final String PREFIX_NER_LABEL_NAME = "NER";
    private final String name;

    public NERLabel(String name) {
        if (name == null) {
            throw new IllegalArgumentException();
        }
        this.name = PREFIX_NER_LABEL_NAME + name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    public String name() {
        return this.name;
    }

    public static Label label(String name) {
        if (name == null) {
            throw new IllegalArgumentException();
        }
        return new NERLabel(name);
    }

}
