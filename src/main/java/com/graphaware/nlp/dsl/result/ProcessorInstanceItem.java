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
package com.graphaware.nlp.dsl.result;

import java.util.Map;

public class ProcessorInstanceItem {
    public final String className;
    public final String name;
    public final Map<String, Object> parameters;
    public final boolean valid;

    public ProcessorInstanceItem(String className, String name, 
            Map<String, Object> parameters,
            boolean valid) {
        this.className = className;
        this.name = name;
        this.parameters = parameters;
        this.valid = valid;
    }

    @Override
    public String toString() {
        return name + " [" + className + " ]";
    }
    
    
}
