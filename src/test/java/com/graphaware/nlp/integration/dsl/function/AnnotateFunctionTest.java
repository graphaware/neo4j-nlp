package com.graphaware.nlp.integration.dsl.function;

import com.graphaware.nlp.dsl.function.AnnotateFunction;
import com.graphaware.nlp.module.NLPConfiguration;
import com.graphaware.nlp.module.NLPModule;
import com.graphaware.runtime.GraphAwareRuntime;
import com.graphaware.runtime.GraphAwareRuntimeFactory;
import com.graphaware.test.integration.GraphAwareIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.impl.proc.Procedures;
import org.neo4j.kernel.internal.GraphDatabaseAPI;

import java.util.HashMap;
import java.util.Map;

public class AnnotateFunctionTest extends GraphAwareIntegrationTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        GraphAwareRuntime runtime = GraphAwareRuntimeFactory.createRuntime(getDatabase());
        runtime.registerModule(new NLPModule("NLP", NLPConfiguration.defaultConfiguration(), getDatabase()));
        runtime.start();
        runtime.waitUntilStarted();
    }

    @Test
    public void testAnnotateFunction() {
        String text = "John Smith works at GraphAware. He is an experienced consultant.";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", "tokenizer");
        parameters.put("textProcessor", "com.graphaware.nlp.stub.StubTextProcessor");

        Map<String, Object> p = new HashMap<>();
        p.put("text", text);
        p.put("params", parameters);

        try (Transaction tx = getDatabase().beginTx()) {
            Result result = getDatabase().execute("RETURN ga.nlp.processor.annotate({text}, {params}) AS annotated", p);
            while (result.hasNext()) {
                Map<String, Object> record = result.next();
                System.out.println(record);
            }
            tx.success();
        }
    }
}
