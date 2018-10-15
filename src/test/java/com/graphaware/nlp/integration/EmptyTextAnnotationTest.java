package com.graphaware.nlp.integration;

import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.exception.InvalidTextException;
import com.graphaware.nlp.stub.StubTextProcessor;
import org.junit.Test;

public class EmptyTextAnnotationTest extends NLPIntegrationTest {

    @Test
    public void testTryingToAnnotateAnEmptyTextFails() {
        createPipeline(StubTextProcessor.class.getName(), "empty");
        executeInTransaction("CALL ga.nlp.annotate({id: 'hello', text: '   ', pipeline: 'empty', checkLanguage: false})", emptyConsumer(), InvalidTextException.class);
    }

}
