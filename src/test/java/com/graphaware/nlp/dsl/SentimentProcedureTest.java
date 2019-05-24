package com.graphaware.nlp.dsl;

import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.persistence.constants.Labels;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.stub.StubTextProcessor;
import com.graphaware.nlp.util.TestNLPGraph;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SentimentProcedureTest extends NLPIntegrationTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        createPipeline(StubTextProcessor.class.getName(), TextProcessor.DEFAULT_PIPELINE);
        executeInTransaction("CALL ga.nlp.processor.pipeline.default({p0})", buildSeqParameters("tokenizer"), emptyConsumer());
    }

    @Test
    public void testSentimentViaProcedure() {
        clearDb();
        executeInTransaction("CALL ga.nlp.annotate({pipeline:'tokenizer', text: 'hello my name is Frank', id: 'test-proc', checkLanguage: false})", emptyConsumer());
        executeInTransaction("MATCH (n:AnnotatedText) CALL ga.nlp.sentiment(n) YIELD result RETURN result", (result -> {
            assertTrue(result.hasNext());
        }));

        TestNLPGraph tester = new TestNLPGraph(getDatabase());
        tester.assertSentenceWithIdHasSentimentLabel("test-proc_0", Labels.VeryPositive.name());
    }

}
