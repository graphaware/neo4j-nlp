package com.graphaware.nlp.dsl;

import com.graphaware.nlp.NLPIntegrationTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class TextProcessorsProcedureTest extends NLPIntegrationTest {

    @Test
    public void testGetPipelineInformationsProcedure() {
        executeInTransaction("CALL ga.nlp.processor.getPipelineInfos", (result -> {
            assertTrue(result.hasNext());
        }));
    }
}
