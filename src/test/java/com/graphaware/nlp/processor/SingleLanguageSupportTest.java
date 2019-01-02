package com.graphaware.nlp.processor;

import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.dsl.request.PipelineSpecification;
import com.graphaware.nlp.stub.StubTextProcessor;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import scala.language;

import java.lang.reflect.Field;

import static org.junit.Assert.assertTrue;

public class SingleLanguageSupportTest extends NLPIntegrationTest {

    @Test
    public void testErrorOnMultipleLanguage() throws Exception {
        PipelineSpecification pipelineSpecificationEn = new PipelineSpecification(
                "english",
                StubTextProcessor.class.getName());
        pipelineSpecificationEn.setLanguage("en");
        createPipeline(pipelineSpecificationEn.getTextProcessor(),
                pipelineSpecificationEn.getName(),
                pipelineSpecificationEn.getLanguage());

        PipelineSpecification pipelineSpecificationDe = new PipelineSpecification(
                "german",
                StubTextProcessor.class.getName());
        pipelineSpecificationDe.setLanguage("de");
        boolean raiseAnError = false;
        try {
            createPipeline(pipelineSpecificationDe.getTextProcessor(),
                    pipelineSpecificationDe.getName(),
                    pipelineSpecificationDe.getLanguage());
        } catch (Exception ex) {
            if (ex.getMessage().contains("Multiple languages not supported in this version")) {
                raiseAnError = true;
            }
        }
        assertTrue(raiseAnError);
    }

    private void resetSingleton() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field instance = NLPManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    protected void createPipeline(String textProcessor, String pipelineName,  String language) {
        executeInTransaction("CALL ga.nlp.processor.addPipeline({name:{p0}, textProcessor:{p1}, language: {p2}, processingSteps:{tokenizer:true, ner:true, phrase:true}})", buildSeqParameters(pipelineName, textProcessor, language), emptyConsumer());
    }
}
