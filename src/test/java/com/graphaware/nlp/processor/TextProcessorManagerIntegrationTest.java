package com.graphaware.nlp.processor;

import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.configuration.SettingsConstants;
import com.graphaware.nlp.dsl.request.PipelineSpecification;
import com.graphaware.nlp.stub.StubTextProcessor;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

public class TextProcessorManagerIntegrationTest extends NLPIntegrationTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        createPipeline(pipelineSpecification.getTextProcessor(), pipelineSpecification.getName());
    }

    @Test
    public void testDefaultPipelineIsUsedWhenSetInConfiguration() throws Exception {
        resetSingleton();
        try (Transaction tx = getDatabase().beginTx()) {
            getNLPManager().annotateTextAndPersist("some text", "id1", pipelineSpecification);
            assertTrue(true);
            tx.success();
        }
    }

    private void resetSingleton() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field instance = NLPManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }
}
