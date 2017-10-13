package com.graphaware.nlp.processor;

import com.graphaware.common.kv.GraphKeyValueStore;
import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.module.NLPConfiguration;
import com.graphaware.nlp.module.NLPModule;
import com.graphaware.nlp.stub.StubTextProcessor;
import com.graphaware.runtime.GraphAwareRuntime;
import com.graphaware.runtime.GraphAwareRuntimeFactory;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

public class TextProcessorManagerTest {

    @Test
    public void testDefaultTextProcessorIsReturned() {
        TextProcessorsManager textProcessorsManager = new TextProcessorsManager();
        assertTrue(textProcessorsManager.getDefaultProcessor() instanceof StubTextProcessor);
    }

    @Test(expected = RuntimeException.class)
    public void testExceptionIsThrownWhenRetrievingProcessorThatDoNotExist() {
        TextProcessorsManager textProcessorsManager = new TextProcessorsManager();
        textProcessorsManager.getTextProcessor("not exist");
    }

    @Test
    public void testProcessorsAreRegisteredWithAliases() {
        TextProcessorsManager textProcessorsManager = new TextProcessorsManager();
        assertTrue(textProcessorsManager.getTextProcessor("com.graphaware.nlp.stub.StubTextProcessor") instanceof StubTextProcessor);
    }

    @Test
    public void testRetrievingProcessorWithNameAndPipeline() {
        TextProcessorsManager textProcessorsManager = new TextProcessorsManager();
        assertTrue(textProcessorsManager.retrieveTextProcessor("com.graphaware.nlp.stub.StubTextProcessor", "tokenizer") instanceof StubTextProcessor);
    }

    @Test(expected = RuntimeException.class)
    public void testRetrieveProcessorWithUnknownPipelineThrowException() {
        TextProcessorsManager textProcessorsManager = new TextProcessorsManager();
        textProcessorsManager.retrieveTextProcessor("com.graphaware.nlp.stub.StubTextProcessor", "unk");
    }

    @Test(expected = RuntimeException.class)
    public void testRetrieveProcessorThrowExceptionWhenNullPipelineGiven() {
        TextProcessorsManager textProcessorsManager = new TextProcessorsManager();
        textProcessorsManager.retrieveTextProcessor(null, null);
    }

    @Test
    public void testRetrieveProcessorWithNullAndValidPipelineReturnsProcessor() {
        TextProcessorsManager textProcessorsManager = new TextProcessorsManager();
        assertTrue(textProcessorsManager.retrieveTextProcessor(null, "tokenizer") instanceof StubTextProcessor);
    }

}
