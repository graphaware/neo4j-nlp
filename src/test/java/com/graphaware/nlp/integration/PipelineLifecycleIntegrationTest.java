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

    @Test
    public void testDeletingPipelineNodeDeRegisterItFromProcessor() {
        Map<String, Object> customPipeline = new HashMap<>();
        customPipeline.put("name", "custom");
        customPipeline.put("textProcessor", "com.graphaware.nlp.stub.StubTextProcessor");
        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().execute("CALL ga.nlp.processor.addPipeline({params})", Collections.singletonMap("params", customPipeline));
            Map<String, Object> params = Collections.singletonMap("textProcessor", "com.graphaware.nlp.stub.StubTextProcessor");
            Result result = getDatabase().execute("CALL ga.nlp.processor.getPipelineInfos()");
            assertTrue(result.hasNext());
            boolean foundCustom = false;
            while (result.hasNext() && !foundCustom) {
                Map<String, Object> record = result.next();
                if (record.get("name").equals("custom")) {
                    foundCustom = true;
                }
            }
            assertTrue(foundCustom);
            tx.success();
        }

        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().execute("MATCH (n:Pipeline) DETACH DELETE n");
            tx.success();
        }

        try (Transaction tx = getDatabase().beginTx()) {
            Map<String, Object> params = Collections.singletonMap("textProcessor", "com.graphaware.nlp.stub.StubTextProcessor");
            Result result = getDatabase().execute("CALL ga.nlp.processor.getPipelineInfos()");
            boolean foundCustom = false;
            while (result.hasNext() && !foundCustom) {
                Map<String, Object> record = result.next();
                if (record.get("name").equals("custom")) {
                    foundCustom = true;
                }
            }
            assertFalse(foundCustom);
            tx.success();
        }
    }

}
