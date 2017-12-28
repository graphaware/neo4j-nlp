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

import static com.graphaware.nlp.domain.Constants.VECTOR_PROPERTY;
import com.graphaware.nlp.vector.SparseVector;

public class VectorContainer {

    private final long nodeId;
    private final String propertyName;
    private final SparseVector vector;

    public VectorContainer(long nodeId, String propertyName, SparseVector vector) {
        if (propertyName == null) {
            this.propertyName = VECTOR_PROPERTY;
        } else {
            this.propertyName = propertyName;
        }
        this.vector = vector;
        this.nodeId = nodeId;
    }

    public VectorContainer(long nodeId, SparseVector vector) {
        this.propertyName = VECTOR_PROPERTY;
        this.vector = vector;
        this.nodeId = nodeId;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public SparseVector getVector() {
        return vector;
    }

    public long getNodeId() {
        return nodeId;
    }

}
