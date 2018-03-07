package com.graphaware.nlp.integration.dsl.function;

import com.graphaware.nlp.NLPIntegrationTest;
import org.junit.Test;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.util.HashMap;
import java.util.Map;

public class AnnotateFunctionTest extends NLPIntegrationTest {

    @Test
    public void testAnnotateFunction() {
        String text = "John Smith works at GraphAware. He is an experienced consultant.";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "tokenizer");
        parameters.put("textProcessor", "com.graphaware.nlp.stub.StubTextProcessor");

        Map<String, Object> p = new HashMap<>();
        p.put("text", text);
        p.put("params", parameters);
        executeInTransaction("RETURN ga.nlp.processor.annotate({text}, {params}) AS annotated", p, (result -> {
            Map<String, Object> record = result.next();
            System.out.println(record);
        }));
    }
}
