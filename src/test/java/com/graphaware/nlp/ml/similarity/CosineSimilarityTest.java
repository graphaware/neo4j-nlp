package com.graphaware.nlp.ml.similarity;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class CosineSimilarityTest {

    @Test
    public void testCosineSimilarity() {
        List<Double> vector1 = Arrays.asList(1.0d,1.0d,1.0d,1.0d,1.0d);
        List<Double> vector2 = Arrays.asList(1.0d,1.0d,1.0d,1.0d,1.0d);
        double similarity1 = new CosineSimilarity().getSimilarity(vector1, vector2);
        assertEquals(1.0d, similarity1, 1.0d);

        List<Double> vector3 = Arrays.asList(0.0d,0.0d,0.0d,0.0d,0.0d);
        double similarity2 = new CosineSimilarity().getSimilarity(vector1, vector3);
        assertEquals(0.0d, similarity2, 1.0d);
    }

}
