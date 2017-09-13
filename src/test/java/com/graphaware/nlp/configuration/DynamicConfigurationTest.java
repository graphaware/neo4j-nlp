package com.graphaware.nlp.configuration;

import com.graphaware.common.kv.GraphKeyValueStore;
import com.graphaware.nlp.dsl.request.PipelineSpecification;
import com.graphaware.nlp.stub.StubTextProcessor;
import com.graphaware.test.integration.EmbeddedDatabaseIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import static org.junit.Assert.*;

public class DynamicConfigurationTest extends EmbeddedDatabaseIntegrationTest {

    private GraphKeyValueStore keyValueStore;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        clearDb();
        this.keyValueStore = new GraphKeyValueStore(getDatabase());
    }

    @Test
    public void testConfigurationCanStoreAndRetrievePipelines() {
        DynamicConfiguration configuration = new DynamicConfiguration(getDatabase());
        PipelineSpecification specification = new PipelineSpecification("custom", StubTextProcessor.class.getName());
        specification.setStopWords("hello,hihi");
        specification.setThreadNumber(4);
        configuration.storeCustomPipeline(specification);

        try (Transaction tx = getDatabase().beginTx()) {
            assertTrue(keyValueStore.hasKey("GA__NLP__PIPELINE_custom"));
            tx.success();
        }
    }

    @Test
    public void testConfigurationCanLoadCustomPipelineAsObject() {
        DynamicConfiguration configuration = new DynamicConfiguration(getDatabase());
        PipelineSpecification specification = new PipelineSpecification("custom", StubTextProcessor.class.getName());
        specification.setStopWords("hello,hihi");
        specification.setThreadNumber(4);
        configuration.storeCustomPipeline(specification);

        PipelineSpecification pipelineSpecification = configuration.loadCustomPipelines().get(0);
        assertEquals(specification.getName(), pipelineSpecification.getName());
        assertEquals(specification.getStopWords(), pipelineSpecification.getStopWords());
        assertEquals(specification.getTextProcessor(), pipelineSpecification.getTextProcessor());
        assertEquals(specification.getThreadNumber(), pipelineSpecification.getThreadNumber());
    }

    @Test
    public void testConfigurationCanRemovePipeline() {
        DynamicConfiguration configuration = new DynamicConfiguration(getDatabase());
        PipelineSpecification specification = new PipelineSpecification("custom", StubTextProcessor.class.getName());
        specification.setStopWords("hello,hihi");
        specification.setThreadNumber(4);
        configuration.storeCustomPipeline(specification);

        try (Transaction tx = getDatabase().beginTx()) {
            assertTrue(keyValueStore.hasKey("GA__NLP__PIPELINE_custom"));
            configuration.removePipeline("custom", StubTextProcessor.class.getName());
            assertFalse(keyValueStore.hasKey("GA__NLP__PIPELINE_custom"));
            tx.success();
        }
    }

    private void clearDb() {
        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().execute("MATCH (n) DETACH DELETE n");
            tx.success();
        }
    }

}
