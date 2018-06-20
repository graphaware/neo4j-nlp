/*
 * Copyright (c) 2013-2018 GraphAware
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
package com.graphaware.nlp.vector;

public class DenseVector implements GenericVector {

    private float[] vector;

    public DenseVector() {
    }

    public DenseVector(float[] vector) {
        this.vector = vector;
    }
    
    @Override
    public void setArray(float[] vector) {
        this.vector = vector;
    }

    @Override
    public float[] getArray() {
        return vector;
    }

    @Override
    public float dot(GenericVector other) {
        if (!(other instanceof DenseVector)) {
            throw new RuntimeException("Other vector is not an instance of DenseVector");
        } 
        if (other.getArray() == null || other.getArray().length == 0
                || vector == null || vector.length == 0) {
            return 0f;
        }
        
        if (other.getArray().length != this.vector.length) {
            throw new RuntimeException("The two vectors cannot be multiplied");
        }        
        
        float[] otherVector = other.getArray();
        int n = vector.length;
        float sum = 0.0f;
        for (int i = 0; i < n; i++) {
            sum += otherVector[i] * vector[i];
        }
        return sum;
    }

    @Override
    public float norm() {
        return Double.valueOf(Math.sqrt(this.dot(this))).floatValue();
    }

}
