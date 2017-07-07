/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.ml.similarity;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

/**
 *
 * @author ale
 */
public class CosineFunctions {

    @UserFunction
    @Description("com.graphaware.nlp.ml.similarity.cosine([1.2, 2.3, 3.1], [1.3,0,2.4]) - compute cosine similarity between them")
    public double cosine(
            @Name("vector1") List<Double> vector1,
            @Name("vector2") List<Double> vector2) {
        if (vector1 != null && vector2 != null) {
            double similarity = new CosineSimilarity().getSimilarity(vector1, vector2);
            return similarity;
        } else {
            return 0.0d;
        }
    }

    protected Map<Long, Float> getVectorMap(List<Double> vector1) {
        Map<Long, Float> map
                = IntStream.range(0, vector1.size())
                .boxed()
                .collect(Collectors.toMap(i -> i.longValue(), i -> vector1.get(i).floatValue()));
        return map;
    }
}
