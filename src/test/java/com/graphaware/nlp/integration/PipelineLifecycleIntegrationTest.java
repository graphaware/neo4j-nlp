package com.graphaware.nlp.integration;

import com.graphaware.nlp.module.NLPConfiguration;
import com.graphaware.nlp.module.NLPModule;
import com.graphaware.runtime.GraphAwareRuntime;
import com.graphaware.runtime.GraphAwareRuntimeFactory;
import com.graphaware.test.integration.GraphAwareIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.util.*;

import static org.junit.Assert.*;

public class PipelineLifecycleIntegrationTest extends GraphAwareIntegrationTest {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        GraphAwareRuntime runtime = GraphAwareRuntimeFactory.createRuntime(getDatabase());
        runtime.registerModule(new NLPModule("NLP", NLPConfiguration.defaultConfiguration(), getDatabase()));
        runtime.start();
        runtime.waitUntilStarted();
    }

    @Test
    public void testStubProcessorIsRegistered() {
        List<String> registeredProcessors = new ArrayList<>();
        try (Transaction tx = getDatabase().beginTx()) {
            Result result = getDatabase().execute("CALL ga.nlp.processor.getProcessors()");
            while (result.hasNext()) {
                Map<String, Object> processorInfo = result.next();
                registeredProcessors.add(processorInfo.get("className").toString());
            }
            tx.success();
        }

        assertTrue(registeredProcessors.contains("com.graphaware.nlp.stub.StubTextProcessor"));
    }

}
