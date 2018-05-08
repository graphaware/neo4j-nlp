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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SparseVector implements GenericVector {

    private Integer cardinality;
    private List<Long> index;
    private List<Float> values;
    
    public SparseVector() {        
    }

    public SparseVector(int cardinality, List<Long> index, List<Float> values) {
        this.cardinality = cardinality;
        this.index = index;
        this.values = values;
    }

    public static GenericVector fromMap(Map<Long, Float> map) {
        int cardinality = map.size();
        List<Long> index = map.keySet().stream()
                .sorted()
                .collect(Collectors.toList());
        List<Float> values = IntStream.range(0, cardinality)
                .mapToObj(i -> map.get(index.get(i)))
                .collect(Collectors.toList());
        return new SparseVector(cardinality, index, values);
    }

    public static GenericVector fromList(List<Float> vector) {
        int cardinality = vector.get(0).intValue();
        List<Long> index = vector.subList(1, cardinality + 1).stream().map((x) -> x.longValue()).collect(Collectors.toList());
        List<Float> values = vector.subList(cardinality + 1, 2 * cardinality + 1).stream().collect(Collectors.toList());
        return new SparseVector(cardinality, index, values);
    }
    
    @Override
    public void setArray(float[] vector) {
        this.cardinality = Float.valueOf(vector[0]).intValue();
        this.index = new ArrayList<>();
        this.values = new ArrayList<>();
        for (int i = 1; i < vector.length; i++) {
            if (i >= 1 && i < cardinality + 1) {
               index.add(Float.valueOf(vector[i]).longValue());
            } else {
               values.add(vector[i]);
            }
        }
    }

    public List<Float> getList() {
        List<Float> vectorAsList = new ArrayList<>(Collections.nCopies(cardinality * 2 + 1, 0.0f));
        vectorAsList.set(0, cardinality.floatValue());
        final int offset = cardinality;
        IntStream.range(0, cardinality).forEach((k) -> {
            float pos = index.get(k).floatValue();
            Float value = values.get(k);
            vectorAsList.set(k + 1, pos);
            vectorAsList.set(offset + 1 + k, value);
        });
        return vectorAsList;
    }
    
    @Override
    public float[] getArray() {
        float[] vector = new float[cardinality * 2 + 1];
        vector[0] = cardinality.floatValue();
        final int offset = cardinality;
        IntStream.range(0, cardinality).forEach((k) -> {
            float pos = index.get(k).floatValue();
            Float value = values.get(k);
            vector[k + 1] = pos;
            vector[offset + 1 + k] = value;
        });
        return vector;
    }

    public Integer getCardinality() {
        return cardinality;
    }

    public List<Long> getIndex() {
        return index;
    }

    public List<Float> getValues() {
        return values;
    }

    @Override
    public float dot(GenericVector other) {
        if (!(other instanceof SparseVector)) {
            throw new RuntimeException("other is not an instance of SparseVector");
        }
        
        SparseVector otherSparseVector = (SparseVector)other;

        if (this.values == null || 
                this.cardinality == 0 ||
                otherSparseVector.values == null || 
                otherSparseVector.cardinality == 0) {
            return 0f;
        }
        
        final AtomicReference<Float> sum = new AtomicReference<>(0f);
        int xIndex = 0;
        int yIndex = 0;

        while (true) {
            if (index.get(xIndex).longValue() == otherSparseVector.getIndex().get(yIndex).longValue()) {
                float curValue = sum.get();
                curValue += values.get(xIndex) * otherSparseVector.getValues().get(yIndex);
                sum.set(curValue);
                xIndex++;
                yIndex++;
            } else if (index.get(xIndex) > otherSparseVector.getIndex().get(yIndex)) {
                yIndex++;
            } else {
                xIndex++;
            }
            if (xIndex == cardinality
                    || yIndex == otherSparseVector.getCardinality()) {
                break;
            }
        }
        return sum.get();
    }

    @Override
    public float norm() {
        return Double.valueOf(Math.sqrt(this.dot(this))).floatValue();
    }

    @Override
    public String toString() {
        return getList().toString(); //To change body of generated methods, choose Tools | Templates.
    }
}
