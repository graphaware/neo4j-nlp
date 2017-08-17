package com.graphaware.nlp.dsl;

import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.util.TestNLPGraph;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import static org.junit.Assert.*;

public class AnnotateTextProcedureTest extends NLPIntegrationTest {

    @Test
    public void testTextAnnotationViaProcedure() {
        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().execute("CALL ga.nlp.annotate({text: 'hello my name is Frank', id: 'test-proc', checkLanguage: false})");
            tx.success();
        }

        TestNLPGraph tester = new TestNLPGraph(getDatabase());
        tester.assertAnnotatedTextNodesCount(1);
        tester.assertSentenceNodesCount(1);
    }

    @Test
    public void testExceptionIsThrownWhenLanguageCannotBeDetected() {
        try (Transaction tx = getDatabase().beginTx()) {
            try {
                getDatabase().execute("CALL ga.nlp.annotate({text: 'hello my name is Frank', id: 'test-proc'})");
                tx.success();
                assertTrue(false);
            } catch (RuntimeException e) {
                assertTrue(true);
            }
        }
    }



}
