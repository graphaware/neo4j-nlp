package com.graphaware.nlp.dsl;

import com.graphaware.nlp.NLPIntegrationTest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.neo4j.graphdb.Result;

public class PipelineTaskTest extends NLPIntegrationTest {
    
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
        executeInTransaction("UNWIND {texts} AS text CREATE (n:Lesson) SET n.text = text", Collections.singletonMap("texts", SHORT_TEXTS), emptyConsumer());
        executeInTransaction("CALL ga.nlp.pipeline.input.create('testInput', "
                + "'com.graphaware.nlp.pipeline.input.QueryBasedPipelineIput', "
                + "{query: 'MATCH (n:Lesson) where not exists((n)-->(:AnnotatedText)) return n.text as text, id(n) as id'})",
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
        
        executeInTransaction("CALL ga.nlp.pipeline.task.start('testTask')",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                }));
    }

    @Test
    public void testInstanceList() {
        executeInTransaction("CALL ga.nlp.pipeline.task.instance.list()",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    Map<String, Object> next = result.next();
                    Assert.assertEquals("testTask", (String) next.get("name"));
                    Assert.assertEquals("com.graphaware.nlp.pipeline.task.PipelineTask", (String) next.get("className"));
                }));
    }
}
