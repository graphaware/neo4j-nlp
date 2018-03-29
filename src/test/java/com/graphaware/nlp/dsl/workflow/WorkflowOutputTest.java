package com.graphaware.nlp.dsl.workflow;

import com.graphaware.nlp.NLPIntegrationTest;
import java.util.Map;
import org.junit.Assert;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.neo4j.graphdb.Result;

public class WorkflowOutputTest extends NLPIntegrationTest {

    @Test
    public void testClassList() {
        clearDb();
        executeInTransaction("CALL ga.nlp.workflow.output.class.list()",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    while (result.hasNext()) {
                        Map<String, Object> next = result.next();
                        Assert.assertTrue(((String)next.get("name")).contains("com.graphaware.nlp.workflow.output"));
                        Assert.assertTrue(((String)next.get("className")).contains("com.graphaware.nlp.workflow.output"));
                    }

                }));
    }

    @Test
    public void testCreation() {
        clearDb();
        executeInTransaction("CALL ga.nlp.workflow.output.create('testOutput', "
                + "'com.graphaware.nlp.workflow.output.StoreAnnotatedTextWorkflowOutput', "
                + "{query: 'MATCH (n:Lesson) where not exists((n)-->(:AnnotatedText)) return n'})",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    Map<String, Object> next = result.next();
                    Assert.assertEquals("testOutput", (String) next.get("name"));
                    Assert.assertEquals("com.graphaware.nlp.workflow.output.StoreAnnotatedTextWorkflowOutput", (String) next.get("className"));
                }));
        executeInTransaction("CALL ga.nlp.workflow.output.instance.list()",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    Map<String, Object> next = result.next();
                    Assert.assertEquals("testOutput", (String) next.get("name"));
                    Assert.assertEquals("com.graphaware.nlp.workflow.output.StoreAnnotatedTextWorkflowOutput", (String) next.get("className"));
                }));
    }

}
