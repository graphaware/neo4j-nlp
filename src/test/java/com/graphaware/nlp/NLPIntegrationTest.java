package com.graphaware.nlp;

import com.graphaware.common.kv.GraphKeyValueStore;
import com.graphaware.nlp.dsl.request.PipelineSpecification;
import com.graphaware.nlp.ml.word2vec.Word2VecProcessor;
import com.graphaware.nlp.module.NLPConfiguration;
import com.graphaware.nlp.module.NLPModule;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.stub.StubTextProcessor;
import com.graphaware.nlp.workflow.WorkflowManager;
import com.graphaware.runtime.GraphAwareRuntime;
import com.graphaware.runtime.GraphAwareRuntimeFactory;
import com.graphaware.test.integration.GraphAwareIntegrationTest;
import org.junit.Before;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static com.graphaware.runtime.RuntimeRegistry.getStartedRuntime;
import static org.junit.Assert.*;
import java.lang.reflect.Field;

public abstract class NLPIntegrationTest extends GraphAwareIntegrationTest {

    protected static PipelineSpecification pipelineSpecification = new PipelineSpecification(
            TextProcessor.DEFAULT_PIPELINE,
            StubTextProcessor.class.getName());

    protected GraphKeyValueStore keyValueStore;

    protected static final String STORE_KEY = "GA__NLP__";

    @Before
    @Override
    public void setUp() throws Exception {
        resetSingleton();
        super.setUp();
        registerRuntime();
        keyValueStore = new GraphKeyValueStore(getDatabase());
    }

    @Override
    protected Map<String, String> additionalServerConfiguration() {
        return Collections.singletonMap("dbms.directories.import", "import");
    }

    protected void registerRuntime() {
        GraphAwareRuntime runtime = GraphAwareRuntimeFactory.createRuntime(getDatabase());
        runtime.registerModule(new NLPModule("NLP", NLPConfiguration.defaultConfiguration(), getDatabase()));
        runtime.start();
        runtime.waitUntilStarted();
    }
    
    private void resetSingleton() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
       Field nlpManagerinstance = NLPManager.class.getDeclaredField("instance");
       nlpManagerinstance.setAccessible(true);
       nlpManagerinstance.set(null, null);
       
       Field instance = WorkflowManager.class.getDeclaredField("instance");
       instance.setAccessible(true);
       instance.set(null, null);
    }

    protected NLPManager getNLPManager() {
        return getStartedRuntime(getDatabase()).getModule(NLPModule.class).getNlpManager();
    }

    protected void executeInTransaction(String query, Consumer<Result> resultConsumer, Class expectedException) {
        executeInTransaction(query, Collections.emptyMap(), resultConsumer, expectedException);
    }

    protected void executeInTransaction(String query, Consumer<Result> resultConsumer) {
        executeInTransaction(query, Collections.emptyMap(), resultConsumer, null);
    }

    protected void executeInTransaction(String query, Map<String, Object> parameters, Consumer<Result> resultConsumer) {
        executeInTransaction(query, parameters, resultConsumer, null);
    }


    protected void executeInTransaction(String query, Map<String, Object> parameters, Consumer<Result> resultConsumer, Class expectedExceptionClass) {
        try (Transaction tx = getDatabase().beginTx()) {
            Map<String, Object> p = (parameters == null) ? Collections.emptyMap() : parameters;
            resultConsumer.accept(getDatabase().execute(query, p));
            tx.success();
        } catch (Exception e) {
            if (null == expectedExceptionClass) {
                throw e;
            }

            validateExpectedException(e, expectedExceptionClass);
        }
    }

    private void validateExpectedException(Exception e, Class clazz) {
        assertTrue(checkInnerException(e, clazz));
    }

    private boolean checkInnerException(Throwable e, Class clazz) {
        if (e.getClass().equals(clazz)) {
            return true;
        }

        if (e.getCause() != null) {
            return checkInnerException(e.getCause(), clazz);
        }

        return false;
    }

    protected Word2VecProcessor getWord2VecProcessor() {
        return (Word2VecProcessor) getNLPManager().getExtension(Word2VecProcessor.class);
    }

    protected void clearDb() {
        executeInTransaction("MATCH (n) DETACH DELETE n", emptyConsumer());
    }

    protected static Consumer<Result> emptyConsumer() {
        return new Consumer<Result>() {
            @Override
            public void accept(Result result) {
                //
            }
        };
    }

    protected boolean checkConfigurationContainsKey(String key) {
        boolean result;
        try (Transaction tx = getDatabase().beginTx()) {
            result = keyValueStore.hasKey(key);
            tx.success();
        }

        return result;
    }

    protected Map<String, Object> buildSeqParameters(Object ...parameters) {
        int i = 0;
        Map<String, Object> map = new HashMap<>();
        for (Object o : parameters) {
            String k = "p" + i;
            map.put(k, o);
            ++i;
        }

        return map;
    }

    protected void createPipeline(String textProcessor, String pipelineName, String ...annotators) {
        final Map<String, Boolean> steps = new HashMap<>();
        Arrays.asList(annotators).forEach(a -> {
            steps.put(a, true);
        });
        String query = "CALL ga.nlp.processor.addPipeline({name:{p0}, textProcessor:{p1}, language: 'en', processingSteps:$p2})";
        executeInTransaction(query, buildSeqParameters(pipelineName, textProcessor, steps), emptyConsumer());

    }

    protected void createPipeline(String textProcessor, String pipelineName) {
        executeInTransaction("CALL ga.nlp.processor.addPipeline({name:{p0}, textProcessor:{p1}, language: 'en', processingSteps:{tokenizer:true, ner:true, phrase:true}})", buildSeqParameters(pipelineName, textProcessor), emptyConsumer());
    }

    protected void createStubPipelineAndSetDefault(String name) {
        createPipeline(StubTextProcessor.class.getName(), name);
        executeInTransaction("CALL ga.nlp.processor.pipeline.default({n})", Collections.singletonMap("n", name), emptyConsumer());
    }

}
