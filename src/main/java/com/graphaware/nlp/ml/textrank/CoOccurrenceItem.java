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
package com.graphaware.nlp.ml.textrank;

public class CoOccurrenceItem {
    private final long source;
    private final long destination;
    private int count;

    public CoOccurrenceItem(long source, long destination) {
        this.source = source;
        this.destination = destination;
        this.count = 1;
    }
    
    public long getSource() {
        return source;
    }

    public long getDestination() {
        return destination;
    }

    public int getCount() {
        return count;
    }
    
    public void incCount() {
        this.count++;
    }
}
