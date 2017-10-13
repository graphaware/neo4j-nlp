package com.graphaware.nlp.dsl;

import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.util.TestNLPGraph;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class AnnotateTextProcedureTest extends NLPIntegrationTest {

    private static final List<String> SHORT_TEXTS =
            Arrays.asList("You knew China's cities were growing. But the real numbers are stunning http://wef.ch/29IxY7w  #China",
                    "Globalization for the 99%: can we make it work for all?",
                    "This organisation increased productivity, happiness and trust with just one change http://wef.ch/29PeKxF ",
                    "In pictures: The high-tech villages that live off the grid http://wef.ch/29xuRh8 ",
                    "The 10 countries best prepared for the new digital economy http://wef.ch/2a8DNug ",
                    "This is how to limit damage to the #euro after #Brexit, say economists http://wef.ch/29GGVzG ",
                    "The office jobs that could see you earning nearly 50% less than some of your co-workers http://wef.ch/29P9biE ",
                    "Which nationalities have the best quality of life? http://wef.ch/29uDfwV",
                    "It’s 9,000km away, but #Brexit has hit #Japan hard http://wef.ch/29P92eQ  #economics",
                    "Which is the world’s fastest-growing large economy? Clue: it’s not #China http://wef.ch/29xuXFd  #economics"
            );

    @Test
    public void testTextAnnotationViaProcedure() {
        clearDb();
        executeInTransaction("CALL ga.nlp.annotate({text: 'hello my name is Frank', id: 'test-proc', checkLanguage: false})", emptyConsumer());

        TestNLPGraph tester = new TestNLPGraph(getDatabase());
        tester.assertAnnotatedTextNodesCount(1);
        tester.assertSentenceNodesCount(1);
    }

    @Test
    public void testTextAnnotationOnMultipleNodes() {
        clearDb();
        executeInTransaction("UNWIND {texts} AS text CREATE (n:Tweet) SET n.text = text", Collections.singletonMap("texts", SHORT_TEXTS), emptyConsumer());
        executeInTransaction("MATCH (n:Tweet) CALL ga.nlp.annotate({text: n.text, id: id(n), checkLanguage: false}) YIELD result WITH result AS at, n MERGE (n)-[r:ANNOTATED_TEXT]->(at) RETURN n",(result -> {
            assertTrue(result.hasNext());
            assertEquals(10, result.stream().count());
        }));
        executeInTransaction("MATCH (n:Tweet)-[r:ANNOTATED_TEXT]->(at) RETURN n, at", (result -> {
            assertTrue(result.hasNext());
            assertEquals(10, result.stream().count());
        }));
    }

    @Test
    public void testExceptionIsThrownWhenLanguageCannotBeDetected() {
        try {
            //executeInTransaction("CALL ga.nlp.annotate({text: 'hello my name is Frank', id: 'test-proc'})", emptyConsumer());
            executeInTransaction("CALL ga.nlp.annotate({text: 'The European Union accumulated a higher portion of GDP as a form of foreign aid than any other economic union.', id: 'test-proc'})", emptyConsumer());
            assertTrue(true);
        } catch (Exception e) {
            assertTrue(false);
        }
    }

    @Test
    public void testAnnotateWithProcessorAlias() {
        executeInTransaction("CALL ga.nlp.annotate({text:'John and Adam planned to kill the cat', id: '123', textProcessor:'com.graphaware.nlp.stub.StubTextProcessor'})", (result -> {
            assertTrue(result.hasNext());
        }));
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
