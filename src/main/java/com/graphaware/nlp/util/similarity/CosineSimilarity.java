package com.graphaware.nlp.util.similarity;

import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author alessandro@graphaware.com
 */
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

    private float getNorm(Map<Long, Float> xVector) {
        final AtomicReference<Float> sum = new AtomicReference<>(0f);
        xVector.values().stream().forEach((value) -> {
            float curValue = sum.get();
            curValue += value * value;
            sum.set(curValue);
        });
        return Double.valueOf(Math.sqrt(sum.get().doubleValue())).floatValue();
    }

}
