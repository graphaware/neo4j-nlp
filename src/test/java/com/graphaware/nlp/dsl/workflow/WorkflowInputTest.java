package com.graphaware.nlp.dsl.workflow;

import com.graphaware.nlp.NLPIntegrationTest;
import java.util.Map;
import org.junit.Assert;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.neo4j.graphdb.Result;

public class WorkflowInputTest extends NLPIntegrationTest {

    @Test
    public void testClassList() {
        clearDb();
        executeInTransaction("CALL ga.nlp.workflow.input.class.list()",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    Map<String, Object> next = result.next();
                    Assert.assertEquals("com.graphaware.nlp.workflow.input.QueryBasedWorkflowInput", next.get("name"));
                    Assert.assertEquals("com.graphaware.nlp.workflow.input.QueryBasedWorkflowInput", next.get("className"));
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
                    Assert.assertEquals("testInput", (String) next.get("name"));
                    Assert.assertEquals("com.graphaware.nlp.workflow.input.QueryBasedWorkflowInput", (String) next.get("className"));
                }));
    }

    @Test
    public void testInstanceList() {
        clearDb();
        executeInTransaction("CALL ga.nlp.workflow.input.instance.list()",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    Map<String, Object> next = result.next();
                    Assert.assertEquals("testInput", (String) next.get("name"));
                    Assert.assertEquals("com.graphaware.nlp.workflow.input.QueryBasedWorkflowInput", (String) next.get("className"));
                }));
    }

}
