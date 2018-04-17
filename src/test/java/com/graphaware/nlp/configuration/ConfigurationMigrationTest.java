package com.graphaware.nlp.configuration;

import com.graphaware.nlp.AbstractEmbeddedTest;
import com.graphaware.nlp.dsl.request.PipelineSpecification;
import com.graphaware.nlp.module.NLPConfiguration;
import com.graphaware.nlp.module.NLPModule;
import com.graphaware.nlp.stub.StubTextProcessor;
import com.graphaware.runtime.GraphAwareRuntime;
import com.graphaware.runtime.GraphAwareRuntimeFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import static org.junit.Assert.*;

public class ConfigurationMigrationTest extends AbstractEmbeddedTest {

    @Test
    public void testPipelinesCanBeMigrated() throws Exception {
        PipelineSpecification specification = new PipelineSpecification("custom", StubTextProcessor.class.getName());
        specification.setStopWords("hello,hihi");
        ObjectMapper mapper = new ObjectMapper();
        String spec = mapper.writeValueAsString(specification);
        try (Transaction tx = getDatabase().beginTx()) {
            keyValueStore.set(DynamicConfiguration.STORE_KEY + "PIPELINE_" + specification.getName(), spec);
            assertFalse(spec.contains("\"@class\""));
            tx.success();
        }

        GraphAwareRuntime runtime = GraphAwareRuntimeFactory.createRuntime(getDatabase());
        runtime.registerModule(new NLPModule("NLP", NLPConfiguration.defaultConfiguration(), getDatabase()));
        runtime.start();
        runtime.waitUntilStarted();

        try (Transaction tx = getDatabase().beginTx()) {
            String s = keyValueStore.get(DynamicConfiguration.STORE_KEY + "PIPELINE_" + specification.getName()).toString();
            assertTrue(s.contains("\"@class\""));
            tx.success();
        }
    }

}
