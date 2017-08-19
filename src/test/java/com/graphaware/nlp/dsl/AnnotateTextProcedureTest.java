package com.graphaware.nlp.dsl;

import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.util.TestNLPGraph;
import java.util.Map;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import static org.junit.Assert.*;
import org.neo4j.graphdb.Result;

public class AnnotateTextProcedureTest extends NLPIntegrationTest {

    @Test
    public void testTextAnnotationViaProcedure() {
        clearDb();
        executeInTransaction("CALL ga.nlp.annotate({text: 'hello my name is Frank', id: 'test-proc', checkLanguage: false})", emptyConsumer());

        TestNLPGraph tester = new TestNLPGraph(getDatabase());
        tester.assertAnnotatedTextNodesCount(1);
        tester.assertSentenceNodesCount(1);
    }

    @Test
    public void testExceptionIsThrownWhenLanguageCannotBeDetected() {
        try {
            executeInTransaction("CALL ga.nlp.annotate({text: 'hello my name is Frank', id: 'test-proc'})", emptyConsumer());
            assertFalse(true);
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    public void testFilter() {
        executeInTransaction("CALL ga.nlp.filter({text: 'This is the operations manual for Neo4j version 3.2, authored by the Neo4j Team.', filter: 'Neo4j'})",
                (result -> {
                    assertTrue(result.hasNext());
                    assertTrue((Boolean)result.next().get("result"));
                }));
    }

}
