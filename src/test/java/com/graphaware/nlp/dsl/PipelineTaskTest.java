package com.graphaware.nlp.dsl;

import com.graphaware.nlp.NLPIntegrationTest;
import java.util.Map;
import org.junit.Assert;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.neo4j.graphdb.Result;



public class PipelineTaskTest extends NLPIntegrationTest {
    
    @Test
    public void testClassList() {
        clearDb();
        executeInTransaction("CALL ga.nlp.pipeline.task.class.list()",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    Map<String, Object> next = result.next();
                    Assert.assertEquals("com.graphaware.nlp.pipeline.task.PipelineTask", next.get("name"));
                    Assert.assertEquals("com.graphaware.nlp.pipeline.task.PipelineTask", next.get("className"));
                }));
    }
    
    @Test
    public void testCreation() {
        clearDb();
        executeInTransaction("CALL ga.nlp.pipeline.input.create('testInput', "
                + "'com.graphaware.nlp.pipeline.input.QueryBasedPipelineIput', "
                + "{query: 'MATCH (n:Lesson) where not exists((n)-->(:AnnotatedText)) return n'})",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    Map<String, Object> next = result.next();
                    Assert.assertEquals("testInput", (String) next.get("name"));
                    Assert.assertEquals("com.graphaware.nlp.pipeline.input.QueryBasedPipelineIput", (String) next.get("className"));
                }));
        executeInTransaction("CALL ga.nlp.pipeline.processor.create('testProcess', "
                + "'com.graphaware.nlp.pipeline.processor.PipelineTextProcessor', "
                + "{"
                + "textProcessor: 'com.graphaware.nlp.processor.stanford.StanfordTextProcessor', "
                + "name: 'customStopWords', "
                + "processingSteps: {tokenize: true, dependency: true}, "
                + "stopWords: '+,have, use, can, should, from, may, result, all, during, must, when, time, could, require, work, need, provide, nasa, support, perform, include, which, would, other, level, more, make, between, you, do, about, above, after, again, against, am, any, because, been, before, being, below, both, did, do, does, doing, down, each, few, further, had, has, having, he, her, here, hers, herself, him, himself, his, how, i, its, itself, just, me, most, my, myself, nor, now, off, once, only, our, ours, ourselves, out, over, own, same, she, so, some, than, theirs, them, themselves, those, through, too, under, until, up, very, we, were, what, where, while, who, whom, why, you, your, yours, yourself, yourselves, small, big, little, much, more, some, several, also, any, both, rdquo, ldquo, raquo', "
                + "threadNumber: 20})",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    Map<String, Object> next = result.next();
                    Assert.assertEquals("testProcess", (String) next.get("name"));
                    Assert.assertEquals("com.graphaware.nlp.pipeline.processor.PipelineTextProcessor", (String) next.get("className"));
                }));
        executeInTransaction("CALL ga.nlp.pipeline.output.create('testOutput', "
                + "'com.graphaware.nlp.pipeline.output.StoreAnnotatedTextPipelineOutput', "
                + "{query: 'MATCH (n:Lesson) where not exists((n)-->(:AnnotatedText)) return n'})",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    Map<String, Object> next = result.next();
                    Assert.assertEquals("testOutput", (String) next.get("name"));
                    Assert.assertEquals("com.graphaware.nlp.pipeline.output.StoreAnnotatedTextPipelineOutput", (String) next.get("className"));
                }));
        executeInTransaction("CALL ga.nlp.pipeline.task.create('testTask', "
                + "'com.graphaware.nlp.pipeline.task.PipelineTask', "
                + "{"
                + "input: 'testInput', "
                + "output: 'testOutput', "
                + "processor: 'testProcess'"
                + "})",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    Map<String, Object> next = result.next();
                    Assert.assertEquals("testTask", (String) next.get("name"));
                    Assert.assertEquals("com.graphaware.nlp.pipeline.task.PipelineTask", (String) next.get("className"));
                }));
    }
}
