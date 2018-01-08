package com.graphaware.nlp.dsl;

import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.dsl.request.PipelineSpecification;
import com.graphaware.nlp.stub.StubTextProcessor;
import org.apache.commons.lang3.builder.ToStringExclude;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;

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
        executeInTransaction("CALL ga.nlp.processor.addPipeline({name:'custom-1', textProcessor:'" + StubTextProcessor.class.getName() +"', processingSteps:{tokenize:true,ner:true,dependency:true},excludedNER:['MONEY','MISC']})", emptyConsumer());
        assertTrue(checkConfigurationContainsKey(STORE_KEY + "PIPELINE_custom-1"));
        PipelineSpecification pipelineSpecification = getNLPManager().getConfiguration()
                .loadPipeline("custom-1");
        assertNotNull(pipelineSpecification);
        assertTrue(pipelineSpecification.hasProcessingStep("tokenize"));
        assertTrue(pipelineSpecification.hasProcessingStep("dependency"));
        assertTrue(pipelineSpecification.getExcludedNER().contains("MONEY"));
        assertTrue(pipelineSpecification.getExcludedNER().contains("MISC"));
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
        assertTrue(checkConfigurationContainsKey(STORE_KEY + "PIPELINE_custom-1"));
//        assertTrue(getNLPManager().getTextProcessorsManager().getTextProcessor(StubTextProcessor.class.getName()).getPipelines().contains("custom-1"));
        executeInTransaction("CALL ga.nlp.processor.removePipeline('custom-1', '"+StubTextProcessor.class.getName()+"')", emptyConsumer());
//        assertFalse(getNLPManager().getTextProcessorsManager().getTextProcessor(StubTextProcessor.class.getName()).getPipelines().contains("custom-1"));
        assertFalse(checkConfigurationContainsKey(STORE_KEY + "PIPELINE_custom-1"));
    }

    @Test
    public void testGetPipelineInfosWorksWithAndWithoutAPipelineNameParameter() {
        clearDb();
        removeCustomPipelines();
        executeInTransaction("CALL ga.nlp.processor.addPipeline({name:'custom-1', textProcessor:'" + StubTextProcessor.class.getName() +"'})", emptyConsumer());
        executeInTransaction("CALL ga.nlp.processor.getPipelines", (result -> {
            assertTrue(result.hasNext());
            assertEquals(2, result.stream().count());
        }));

        executeInTransaction("CALL ga.nlp.processor.getPipelines('custom-1')", (result -> {
            assertTrue(result.hasNext());
            assertEquals(1, result.stream().count());
        }));
    }

    @Test
    public void testAddingPipelineWithCustomSentimentModel() {
        clearDb();
        removeCustomPipelines();
        executeInTransaction("CALL ga.nlp.processor.addPipeline({name:'custom-1', textProcessor:'" + StubTextProcessor.class.getName() +"', processingSteps:{customSentiment:'my-model'}})", emptyConsumer());
        PipelineSpecification pipelineSpecification = getNLPManager().getConfiguration().loadPipeline("custom-1");
        assertEquals("my-model", pipelineSpecification.getProcessingStepAsString("customSentiment"));
    }

    @Test
    public void testCustomPipelineWithSentimentModelShouldDisplayModelNameInPipelineInfo() {
        clearDb();
        removeCustomPipelines();
        executeInTransaction("CALL ga.nlp.processor.addPipeline({name:'custom-1', textProcessor:'" + StubTextProcessor.class.getName() +"', processingSteps:{customSentiment:'my-model'}})", emptyConsumer());
        PipelineSpecification pipelineSpecification = getNLPManager().getConfiguration().loadPipeline("custom-1");
        assertEquals("my-model", pipelineSpecification.getProcessingStepAsString("customSentiment"));
        executeInTransaction("CALL ga.nlp.processor.getPipelines('custom-1')", (result -> {
            assertTrue(result.hasNext());
            while (result.hasNext()) {
                Map<String, Object> record = (Map<String, Object>) result.next();
                Map<String, Object> specs = (Map<String, Object>) record.get("specifications");
                assertEquals("my-model", specs.get("customSentiment"));
            }
        }));
    }

    private void removeCustomPipelines() {
        try (Transaction tx = getDatabase().beginTx()) {
            getNLPManager().getConfiguration().loadCustomPipelines().forEach(pipelineSpecification -> {
                getNLPManager().getConfiguration().removePipeline(pipelineSpecification.getName(), pipelineSpecification.getTextProcessor());
            });
            tx.success();
        }
    }
}
