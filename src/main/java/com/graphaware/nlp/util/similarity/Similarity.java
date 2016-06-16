package com.graphaware.nlp.util.similarity;

import java.util.Map;

/**
 *
 * @author alessandro@graphaware.com
 */
public interface Similarity {
    public float getSimilarity(Map<Long, Float> x, Map<Long, Float> y);
}
