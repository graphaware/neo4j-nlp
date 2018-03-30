package com.graphaware.nlp.dsl.workflow;

import com.graphaware.nlp.NLPIntegrationTest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.stub.StubTextProcessor;
import static org.junit.Assert.assertTrue;

import com.graphaware.nlp.workflow.task.TaskStatus;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Result;

import static org.junit.Assert.*;

public class WorkflowTaskTest extends NLPIntegrationTest {

    private static final List<String> SHORT_TEXTS
            = Arrays.asList("You knew China's cities were growing. But the real numbers are stunning http://wef.ch/29IxY7w  #China",
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


    @Before
    public void setUp() throws Exception {
        super.setUp();
        createPipeline(StubTextProcessor.class.getName(), TextProcessor.DEFAULT_PIPELINE);
        executeInTransaction("CALL ga.nlp.processor.pipeline.default({p0})", buildSeqParameters("tokenizer"), emptyConsumer());
    }

    @Test
    public void testClassList() {
        clearDb();
        executeInTransaction("CALL ga.nlp.workflow.task.class.list()",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    Map<String, Object> next = result.next();
                    assertEquals("com.graphaware.nlp.workflow.task.WorkflowTask", next.get("name"));
                    assertEquals("com.graphaware.nlp.workflow.task.WorkflowTask", next.get("className"));
                }));
    }

    @Test
    public void testCreation() {
        clearDb();
        // Ingest some texts
        executeInTransaction("UNWIND {texts} AS text CREATE (n:Lesson) SET n.text = text", Collections.singletonMap("texts", SHORT_TEXTS), emptyConsumer());

        // Create a workflow input for the text as query
        executeInTransaction("CALL ga.nlp.workflow.input.create('testInput', "
                + "'com.graphaware.nlp.workflow.input.QueryBasedWorkflowInput', "
                + "{query: 'MATCH (n:Lesson) where not exists((n)-->(:AnnotatedText)) return n.text as text, toString(id(n)) as id'})",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    Map<String, Object> next = result.next();
                    assertEquals("testInput", (String) next.get("name"));
                    assertEquals("com.graphaware.nlp.workflow.input.QueryBasedWorkflowInput", (String) next.get("className"));
                }));

        // Create a workflow text processor
        executeInTransaction("CALL ga.nlp.workflow.processor.create('testProcess', "
                + "'com.graphaware.nlp.workflow.processor.WorkflowTextProcessor', "
                + "{"
                + "pipeline: 'tokenizer'})",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    Map<String, Object> next = result.next();
                    assertEquals("testProcess", (String) next.get("name"));
                    assertEquals("com.graphaware.nlp.workflow.processor.WorkflowTextProcessor", (String) next.get("className"));
                }));

        // Create a store output
        executeInTransaction("CALL ga.nlp.workflow.output.create('testOutput', "
                + "'com.graphaware.nlp.workflow.output.StoreAnnotatedTextWorkflowOutput', "
                + "{query: 'MATCH (n:Lesson), (result) "
                + "where id(n) = toInteger({entryId}) AND id(result) = toInteger({annotatedTextId}) "
                + "WITH n, result "
                + "MERGE (n)-[r:HAS_ANNOTATED_TEXT]->(result) '})",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    Map<String, Object> next = result.next();
                    assertEquals("testOutput", (String) next.get("name"));
                    assertEquals("com.graphaware.nlp.workflow.output.StoreAnnotatedTextWorkflowOutput", (String) next.get("className"));
                }));

        // Create the Task
        executeInTransaction("CALL ga.nlp.workflow.task.create('testTask', "
                + "'com.graphaware.nlp.workflow.task.WorkflowTask', "
                + "{"
                + "input: 'testInput', "
                + "output: 'testOutput', "
                + "processor: 'testProcess', "
                + "sync: true"
                + "})",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    Map<String, Object> next = result.next();
                    assertEquals("testTask", (String) next.get("name"));
                    assertEquals("com.graphaware.nlp.workflow.task.WorkflowTask", (String) next.get("className"));
                }));


        executeInTransaction("CALL ga.nlp.workflow.task.instance.list()",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    Map<String, Object> next = result.next();
                    assertEquals("testTask", (String) next.get("name"));
                    assertEquals("com.graphaware.nlp.workflow.task.WorkflowTask", (String) next.get("className"));
                }));

        // Start the task
        executeInTransaction("CALL ga.nlp.workflow.task.start('testTask')",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                }));

        // Verify the result
        executeInTransaction("MATCH (n)-[r:HAS_ANNOTATED_TEXT]->(p) return n,p",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    int c = 0;
                    while (result.hasNext()) {
                        result.next();
                        c++;
                    }
                    assertEquals(10, c);
                }));
    }

    @Test
    public void testWorkflowTaskWithShortcutsDSL() {
        clearDb();
        executeInTransaction("UNWIND {texts} AS text CREATE (n:Lesson) SET n.text = text", Collections.singletonMap("texts", SHORT_TEXTS), emptyConsumer());

        // query workflow input
        executeInTransaction("CALL ga.nlp.workflow.createQueryInput('myInput', { query: 'MATCH (n:Lesson) RETURN n.text AS text, toString(id(n)) AS id'})", emptyConsumer());
        // text processor
        executeInTransaction("CALL ga.nlp.workflow.createTextProcessor('myProcessor', { pipeline: 'tokenizer'})", emptyConsumer());
        // store output
        executeInTransaction("CALL ga.nlp.workflow.createStoreAnnotationOutput('myOutput')", emptyConsumer());
        // create task
        executeInTransaction("CALL ga.nlp.workflow.task.create('myTask', 'com.graphaware.nlp.workflow.task.WorkflowTask', {input:'myInput', processor:'myProcessor', output:'myOutput'})", emptyConsumer());
        // run the task
        executeInTransaction("CALL ga.nlp.workflow.task.start('myTask')", (result -> {
            while (result.hasNext()) {
                System.out.println(result.next());
            }
        }));
        
        // verify the results
        executeInTransaction("MATCH (n)-[r:HAS_ANNOTATED_TEXT]->() RETURN count(r) AS c", (result -> {
            assertTrue(result.hasNext());
            assertEquals(10, (long) result.next().get("c"), 0L);
        }));
    }

    @Test
    public void testWorkflowTaskWithShortcutsDSLAndNoDocuments() {
        clearDb();

        // query workflow input
        executeInTransaction("CALL ga.nlp.workflow.createQueryInput('myInput', { query: 'MATCH (n:Lesson) RETURN n.text AS text, toString(id(n)) AS id'})", emptyConsumer());
        // text processor
        executeInTransaction("CALL ga.nlp.workflow.createTextProcessor('myProcessor', { pipeline: 'tokenizer'})", emptyConsumer());
        // store output
        executeInTransaction("CALL ga.nlp.workflow.createStoreAnnotationOutput('myOutput')", emptyConsumer());
        // create task
        executeInTransaction("CALL ga.nlp.workflow.task.create('myTask', 'com.graphaware.nlp.workflow.task.WorkflowTask', {input:'myInput', processor:'myProcessor', output:'myOutput'})", emptyConsumer());
        // run the task
        executeInTransaction("CALL ga.nlp.workflow.task.start('myTask')", (result -> {
            while (result.hasNext()) {
                assertEquals(TaskStatus.SUCCEEDED, result.next().get("status"));
            }
        }));

        // verify the results
        executeInTransaction("MATCH (n)-[r:HAS_ANNOTATED_TEXT]->() RETURN count(r) AS c", (result -> {
            assertTrue(result.hasNext());
            assertEquals(10, (long) result.next().get("c"), 0L);
        }));
    }

    @Test
    public void testInstanceList() {

    }
}
