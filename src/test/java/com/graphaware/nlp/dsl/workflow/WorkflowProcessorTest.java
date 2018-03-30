package com.graphaware.nlp.dsl.workflow;

import com.graphaware.nlp.NLPIntegrationTest;
import java.util.Map;

import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.stub.StubTextProcessor;
import org.junit.Assert;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Result;

public class WorkflowProcessorTest extends NLPIntegrationTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        createPipeline(StubTextProcessor.class.getName(), TextProcessor.DEFAULT_PIPELINE);
        executeInTransaction("CALL ga.nlp.processor.pipeline.default({p0})", buildSeqParameters("tokenizer"), emptyConsumer());
    }

    @Test
    public void testClassList() {
        clearDb();
        executeInTransaction("CALL ga.nlp.workflow.processor.class.list()",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    while (result.hasNext()) {
                        Map<String, Object> next = result.next();
                        Assert.assertTrue(((String)next.get("name")).contains("com.graphaware.nlp.workflow.processor"));
                        Assert.assertTrue(((String)next.get("className")).contains("com.graphaware.nlp.workflow.processor"));
                    }
                }));
    }

    @Test
    public void testCreation() {
        clearDb();
        executeInTransaction("CALL ga.nlp.workflow.processor.create('testProcess', "
                + "'com.graphaware.nlp.workflow.processor.WorkflowTextProcessor', "
                + "{"
                + "textProcessor: 'com.graphaware.nlp.stub.StubTextProcessor', "
                + "name: 'customStopWords', "
                + "processingSteps: {tokenize: true, dependency: true}, "
                + "stopWords: '+,have, use, can, should, from, may, result, all, during, must, when, time, could, require, work, need, provide, nasa, support, perform, include, which, would, other, level, more, make, between, you, do, about, above, after, again, against, am, any, because, been, before, being, below, both, did, do, does, doing, down, each, few, further, had, has, having, he, her, here, hers, herself, him, himself, his, how, i, its, itself, just, me, most, my, myself, nor, now, off, once, only, our, ours, ourselves, out, over, own, same, she, so, some, than, theirs, them, themselves, those, through, too, under, until, up, very, we, were, what, where, while, who, whom, why, you, your, yours, yourself, yourselves, small, big, little, much, more, some, several, also, any, both, rdquo, ldquo, raquo', "
                + "threadNumber: 20})",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    Map<String, Object> next = result.next();
                    Assert.assertEquals("testProcess", (String) next.get("name"));
                    Assert.assertEquals("com.graphaware.nlp.workflow.processor.WorkflowTextProcessor", (String) next.get("className"));
                }));
        executeInTransaction("CALL ga.nlp.workflow.processor.instance.list()",
                ((Result result) -> {
                    assertTrue(result.hasNext());
                    Map<String, Object> next = result.next();
                    Assert.assertEquals("testProcess", (String) next.get("name"));
                    Assert.assertEquals("com.graphaware.nlp.workflow.processor.WorkflowTextProcessor", (String) next.get("className"));
                }));
    }

    @Test
    public void testCreationWithShortcutDSL() {
        executeInTransaction("CALL ga.nlp.workflow.createTextProcessor('myProcessor', {pipeline:'tokenizer'})", (result -> {
            assertTrue(result.hasNext());
            while (result.hasNext()) {
                Map<String, Object> next = result.next();
                Assert.assertEquals("myProcessor", (String) next.get("name"));
                Assert.assertEquals("com.graphaware.nlp.workflow.processor.WorkflowTextProcessor", (String) next.get("className"));
            }
        }));
    }

}
