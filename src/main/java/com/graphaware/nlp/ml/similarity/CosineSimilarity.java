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
package com.graphaware.nlp.ml.similarity;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

public class CosineSimilarity implements Similarity {

    @Override
    public float getSimilarity(Map<Long, Float> xVector, Map<Long, Float> yVector) {
        float a = getDotProduct(xVector, yVector);
        float b = getNorm(xVector) * getNorm(yVector);

        if (b > 0) {
            return a / b;
        } else {
            return 0;
        }
    }
    //improved version for user functions
    public double getSimilarity(List<Double> xVector, List<Double> yVector) {
        double a = getDotProduct(xVector, yVector);
        double b = getNorm(xVector) * getNorm(yVector);

        if (b > 0) {
            return a / b;
        } else {
            return 0;
        }
    }

    private float getDotProduct(final Map<Long, Float> xVector, final Map<Long, Float> yVector) {

        final AtomicReference<Float> sum = new AtomicReference<>(0f);
        TreeSet<Long> keys = new TreeSet<>(xVector.keySet());
        keys.addAll(yVector.keySet());
        keys.stream().forEach((key) -> {
            if (xVector.containsKey(key) && yVector.containsKey(key)) {
                float curValue = sum.get();
                curValue += xVector.get(key) * yVector.get(key);
                sum.set(curValue);
            }
        });
        return sum.get();
    }

    private double getDotProduct(final List<Double> xVector, final List<Double> yVector) {

        final AtomicReference<Float> sum = new AtomicReference<>(0f);
        IntStream.range(0, xVector.size())
                .boxed().forEach((key) -> {
                    float curValue = sum.get();
                    curValue += xVector.get(key) * yVector.get(key);
                    sum.set(curValue);
                });
        return sum.get();
    }

    private float getNorm(Map<Long, Float> xVector) {
        final AtomicReference<Float> sum = new AtomicReference<>(0f);
        xVector.values().stream().forEach((value) -> {
            float curValue = sum.get();
            curValue += value * value;
            sum.set(curValue);
        });
        return Double.valueOf(Math.sqrt(sum.get().doubleValue())).floatValue();
    }
    
    private double getNorm(List<Double> xVector) {
        final AtomicReference<Float> sum = new AtomicReference<>(0f);
        xVector.stream().forEach((value) -> {
            float curValue = sum.get();
            curValue += value * value;
            sum.set(curValue);
        });
        return Math.sqrt(sum.get().doubleValue());
    }

}
