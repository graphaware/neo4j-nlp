package com.graphaware.nlp.dsl.workflow;

import com.graphaware.nlp.NLPIntegrationTest;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Result;

import static org.junit.Assert.*;

@Ignore
public class WorkflowInputTest extends NLPIntegrationTest {

    @Test
    public void testClassList() {
        clearDb();
        executeInTransaction("CALL ga.nlp.workflow.input.class.list()",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    Map<String, Object> next = result.next();
                    assertEquals("com.graphaware.nlp.workflow.input.QueryBasedWorkflowInput", next.get("name"));
                    assertEquals("com.graphaware.nlp.workflow.input.QueryBasedWorkflowInput", next.get("className"));
                }));
    }

    @Test
    public void testCreation() {
        clearDb();
        executeInTransaction("CALL ga.nlp.workflow.input.create('testInput', "
                + "'com.graphaware.nlp.workflow.input.QueryBasedWorkflowInput', "
                + "{query: 'MATCH (n:Lesson) where not exists((n)-->(:AnnotatedText)) return n'})",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    Map<String, Object> next = result.next();
                    assertEquals("testInput", (String) next.get("name"));
                    assertEquals("com.graphaware.nlp.workflow.input.QueryBasedWorkflowInput", (String) next.get("className"));
                }));
        executeInTransaction("CALL ga.nlp.workflow.input.instance.list()",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    Map<String, Object> next = result.next();
                    assertEquals("testInput", (String) next.get("name"));
                    assertEquals("com.graphaware.nlp.workflow.input.QueryBasedWorkflowInput", (String) next.get("className"));
                }));
    }

    @Test
    public void testCreateQueryInputProcedure() {
        clearDb();
        executeInTransaction("CALL ga.nlp.workflow.createQueryInput('myInput',{query:'MATCH (n:Document) RETURN n.text AS text, toString(id(n)) AS id'})", (result -> {
            assertTrue(result.hasNext());
            while(result.hasNext()) {
                Map<String, Object> next = result.next();
                assertEquals("com.graphaware.nlp.workflow.input.QueryBasedWorkflowInput", next.get("className"));
                assertEquals("myInput", next.get("name"));
                Map<String, Object> params = (Map<String, Object>) next.get("parameters");
                assertEquals("MATCH (n:Document) RETURN n.text AS text, toString(id(n)) AS id", params.get("query"));
            }
        }));
    }

}
