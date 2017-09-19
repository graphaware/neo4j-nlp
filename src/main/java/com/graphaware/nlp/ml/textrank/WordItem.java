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

public class WordItem {
    private final int startPosition;
    private final int endPosition;
    private final String word;

    public WordItem(int start, int end, String word) {
        this.startPosition = start;
        this.endPosition = end;
        this.word = word;
    }
    
    public int getStart() {
        return this.startPosition;
    }

    public int getEnd() {
        return this.endPosition;
    }

    public String getWord() {
        return this.word;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!WordItem.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        final WordItem other = (WordItem) obj;
        if (!(this.word.equals(other.getWord()) && this.startPosition == other.getStart()))
            return false;
        return true;
    }
}
