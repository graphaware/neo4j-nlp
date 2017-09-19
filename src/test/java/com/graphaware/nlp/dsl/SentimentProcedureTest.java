package com.graphaware.nlp.dsl;

import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.persistence.constants.Labels;
import com.graphaware.nlp.util.TestNLPGraph;
import org.junit.Test;

import static org.junit.Assert.*;

public class SentimentProcedureTest extends NLPIntegrationTest {

    @Test
    public void testSentimentViaProcedure() {
        clearDb();
        executeInTransaction("CALL ga.nlp.annotate({text: 'hello my name is Frank', id: 'test-proc', checkLanguage: false})", emptyConsumer());
        executeInTransaction("MATCH (n:AnnotatedText) CALL ga.nlp.sentiment(n) YIELD result RETURN result", (result -> {
            assertTrue(result.hasNext());
        }));

        TestNLPGraph tester = new TestNLPGraph(getDatabase());
        tester.assertSentenceWithIdHasSentimentLabel("test-proc_0", Labels.VeryPositive.name());
    }

}
