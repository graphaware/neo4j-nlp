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
package com.graphaware.nlp.util;

import java.util.ArrayList;
import java.util.Iterator;

public class FixedSizeOrderedList<T extends Comparable> extends ArrayList<T> {
    
    private final int maxSize;

    public FixedSizeOrderedList(int maxSize) {
        super();
        this.maxSize = maxSize;
    }
    
    @Override
    public boolean add(T value) {
        int index = 0;
        Iterator<T> iter = this.iterator();
        while (iter.hasNext() 
                && iter.next().compareTo(value) > 0) {
            index++;
            if (index > maxSize) {
                return false;
            }
        }
        super.add(index, value);
        if (this.size() > maxSize) {
            this.remove(maxSize);
        }
        return true;
    }
}
