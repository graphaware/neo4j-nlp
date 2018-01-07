package com.graphaware.nlp.dsl.function;

import com.graphaware.nlp.NLPIntegrationTest;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class CosineFunctionsTest extends NLPIntegrationTest {

    @Test
    public void testCosineSimilarityViaProcedure() {
        List<Double> vector1 = Arrays.asList(1.0d,1.0d,1.0d,1.0d,1.0d);
        List<Double> vector2 = Arrays.asList(1.0d,1.0d,1.0d,1.0d,1.0d);
        Map<String, Object> params = buildSeqParameters(vector1, vector2);
        executeInTransaction("RETURN ga.nlp.ml.similarity.cosine({p0},{p1}) AS simil", params, (result -> {
            assertTrue(result.hasNext());
            Map<String, Object> record = (Map<String, Object>) result.next();
            assertEquals(1.0d, (double) record.get("simil"), 1.0d);
        }));
    }
}
