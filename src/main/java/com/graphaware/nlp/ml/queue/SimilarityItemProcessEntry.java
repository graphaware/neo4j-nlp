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
package com.graphaware.nlp.ml.queue;

import java.util.ArrayList;

public class SimilarityItemProcessEntry {
    private final ArrayList<SimilarityItem> kNN;
    private final long node;

    public SimilarityItemProcessEntry(long node, ArrayList<SimilarityItem> kNN) {
        this.kNN = kNN;
        this.node = node;
    }

    public ArrayList<SimilarityItem> getkNN() {
        return kNN;
    }

    public long getNodeId() {
        return node;
    }
}
