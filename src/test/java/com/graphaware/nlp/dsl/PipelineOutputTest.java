package com.graphaware.nlp.dsl;

import com.graphaware.nlp.NLPIntegrationTest;
import java.util.Map;
import org.junit.Assert;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.neo4j.graphdb.Result;

public class PipelineOutputTest extends NLPIntegrationTest {

    @Test
    public void testClassList() {
        clearDb();
        executeInTransaction("CALL ga.nlp.pipeline.output.class.list()",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    Map<String, Object> next = result.next();
                    Assert.assertEquals("com.graphaware.nlp.pipeline.output.StoreAnnotatedTextPipelineOutput", next.get("name"));
                    Assert.assertEquals("com.graphaware.nlp.pipeline.output.StoreAnnotatedTextPipelineOutput", next.get("className"));
                }));
    }

    @Test
    public void testCreation() {
        clearDb();
        executeInTransaction("CALL ga.nlp.pipeline.output.create('testOutput', "
                + "'com.graphaware.nlp.pipeline.output.StoreAnnotatedTextPipelineOutput', "
                + "{query: 'MATCH (n:Lesson) where not exists((n)-->(:AnnotatedText)) return n'})",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    Map<String, Object> next = result.next();
                    Assert.assertEquals("testOutput", (String) next.get("name"));
                    Assert.assertEquals("com.graphaware.nlp.pipeline.output.StoreAnnotatedTextPipelineOutput", (String) next.get("className"));
                }));
    }

    @Test
    public void testInstanceList() {
        clearDb();
        executeInTransaction("CALL ga.nlp.pipeline.output.instance.list()",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    Map<String, Object> next = result.next();
                    Assert.assertEquals("testOutput", (String) next.get("name"));
                    Assert.assertEquals("com.graphaware.nlp.pipeline.output.StoreAnnotatedTextPipelineOutput", (String) next.get("className"));
                }));
    }

}
