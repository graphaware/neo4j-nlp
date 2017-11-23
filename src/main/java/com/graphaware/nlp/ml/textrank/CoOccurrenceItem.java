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

import com.graphaware.common.util.Pair;
import java.util.ArrayList;
import java.util.List;

public class CoOccurrenceItem {
    private final long source;
    private final long destination;
    private double count;
    private final List<Pair<Integer, Integer>> occurrence;
    
    public CoOccurrenceItem(long source, long destination) {
        this(source, 0, destination, 0);
    }

    public CoOccurrenceItem(long source, int sourceStartPosition, long destination, int destinationStartingPosition) {
        this.source = source;
        this.destination = destination;
        this.count = 1;
        this.occurrence = new ArrayList<>();
        this.occurrence.add(new Pair<>(sourceStartPosition, destinationStartingPosition));
    }
    
    public long getSource() {
        return source;
    }

    public long getDestination() {
        return destination;
    }

    public double getCount() {
        return count;
    }
    
    public void incCount() {
        this.count++;
    }

    public void incCountBy(double val) {
        this.count += val;
    }

    public void setCount(double val) {
        this.count = val;
    }
    
    public void addPositions(int sourceStartPosition, int destinationStartingPosition) {
        this.occurrence.add(new Pair<>(sourceStartPosition, destinationStartingPosition));
    }

    public List<Pair<Integer, Integer>> getStartingPositions() {
        return occurrence;
    }    
}
