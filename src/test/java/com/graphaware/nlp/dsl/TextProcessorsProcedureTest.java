package com.graphaware.nlp.dsl;

import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.stub.StubTextProcessor;
import org.junit.Test;

import static org.junit.Assert.*;

public class TextProcessorsProcedureTest extends NLPIntegrationTest {

    @Test
    public void testGetPipelineInformationsProcedure() {
        executeInTransaction("CALL ga.nlp.processor.getPipelineInfos", (result -> {
            assertTrue(result.hasNext());
        }));
    }

    @Test
    public void removePipelineTest() {
        executeInTransaction("CALL ga.nlp.processor.addPipeline({name:'custom-1', textProcessor:'" + StubTextProcessor.class.getName() +"'})", emptyConsumer());
        assertTrue(getNLPManager().getTextProcessorsManager().getTextProcessor(StubTextProcessor.class.getName()).getPipelines().contains("custom-1"));
        executeInTransaction("CALL ga.nlp.processor.removePipeline('custom-1', '"+StubTextProcessor.class.getName()+"')", emptyConsumer());
        assertFalse(getNLPManager().getTextProcessorsManager().getTextProcessor(StubTextProcessor.class.getName()).getPipelines().contains("custom-1"));
    }
}
