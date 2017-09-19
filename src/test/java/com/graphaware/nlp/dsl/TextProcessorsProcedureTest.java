package com.graphaware.nlp.dsl;

import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.stub.StubTextProcessor;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class TextProcessorsProcedureTest extends NLPIntegrationTest {

    @Test
    public void testGetPipelineInformationsProcedure() {
        executeInTransaction("CALL ga.nlp.processor.getPipelines", (result -> {
            assertTrue(result.hasNext());
        }));
    }

    @Test
    public void testAddPipeline() {
        clearDb();
        executeInTransaction("CALL ga.nlp.processor.addPipeline({name:'custom-1', textProcessor:'" + StubTextProcessor.class.getName() +"'})", emptyConsumer());
        assertTrue(
                getNLPManager()
                .getTextProcessorsManager()
                .getTextProcessor(StubTextProcessor.class.getName())
                .getPipelines()
                .contains("custom-1")
        );
        assertTrue(checkConfigurationContainsKey(STORE_KEY + "PIPELINE_custom-1"));
    }

    @Test(expected = RuntimeException.class)
    public void testAddPipelineOnNonExistentProcessorShouldFail() {
        clearDb();
        executeInTransaction("CALL ga.nlp.processor.addPipeline({name:'non-exist', textProcessor:'non-processor'})", (result) -> {
            //
        });
    }

    @Test
    public void removePipelineTest() {
        clearDb();
        executeInTransaction("CALL ga.nlp.processor.addPipeline({name:'custom-1', textProcessor:'" + StubTextProcessor.class.getName() +"'})", emptyConsumer());
        assertTrue(getNLPManager().getTextProcessorsManager().getTextProcessor(StubTextProcessor.class.getName()).getPipelines().contains("custom-1"));
        executeInTransaction("CALL ga.nlp.processor.removePipeline('custom-1', '"+StubTextProcessor.class.getName()+"')", emptyConsumer());
        assertFalse(getNLPManager().getTextProcessorsManager().getTextProcessor(StubTextProcessor.class.getName()).getPipelines().contains("custom-1"));
        assertFalse(checkConfigurationContainsKey(STORE_KEY + "PIPELINE_custom-1"));
    }

    @Test
    public void testGetPipelineInfosWorksWithAndWithoutAPipelineNameParameter() {
        clearDb();
        executeInTransaction("CALL ga.nlp.processor.addPipeline({name:'custom-1', textProcessor:'" + StubTextProcessor.class.getName() +"'})", emptyConsumer());
        executeInTransaction("CALL ga.nlp.processor.getPipelines", (result -> {
            assertTrue(result.hasNext());
            assertEquals(4, result.stream().count());
        }));

        executeInTransaction("CALL ga.nlp.processor.getPipelines('custom-1')", (result -> {
            assertTrue(result.hasNext());
            assertEquals(2, result.stream().count());
        }));

        executeInTransaction("CALL ga.nlp.processor.getPipelines('not-exist')", (result -> {
            assertFalse(result.hasNext());
        }));
    }

}
