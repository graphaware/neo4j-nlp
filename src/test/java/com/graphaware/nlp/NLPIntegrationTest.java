package com.graphaware.nlp;

import com.graphaware.common.kv.GraphKeyValueStore;
import com.graphaware.nlp.module.NLPConfiguration;
import com.graphaware.nlp.module.NLPModule;
import com.graphaware.runtime.GraphAwareRuntime;
import com.graphaware.runtime.GraphAwareRuntimeFactory;
import com.graphaware.test.integration.GraphAwareIntegrationTest;
import org.junit.Before;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import static com.graphaware.runtime.RuntimeRegistry.getStartedRuntime;
import java.lang.reflect.Field;

public abstract class NLPIntegrationTest extends GraphAwareIntegrationTest {

    protected GraphKeyValueStore keyValueStore;

    protected static final String STORE_KEY = "GA__NLP__";

    @Before
    @Override
    public void setUp() throws Exception {
        resetSingleton();
        super.setUp();
        GraphAwareRuntime runtime = GraphAwareRuntimeFactory.createRuntime(getDatabase());
        runtime.registerModule(new NLPModule("NLP", NLPConfiguration.defaultConfiguration(), getDatabase()));
        runtime.start();
        runtime.waitUntilStarted();
        keyValueStore = new GraphKeyValueStore(getDatabase());
    }
    
    private void resetSingleton() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
       Field instance = NLPManager.class.getDeclaredField("instance");
       instance.setAccessible(true);
       instance.set(null, null);
    }

    protected NLPManager getNLPManager() {
        return getStartedRuntime(getDatabase()).getModule(NLPModule.class).getNlpManager();
    }

    protected void executeInTransaction(String query, Consumer<Result> resultConsumer) {
        executeInTransaction(query, Collections.emptyMap(), resultConsumer);
    }


    protected void executeInTransaction(String query, Map<String, Object> parameters, Consumer<Result> resultConsumer) {
        try (Transaction tx = getDatabase().beginTx()) {
            Map<String, Object> p = (parameters == null) ? Collections.emptyMap() : parameters;
            resultConsumer.accept(getDatabase().execute(query, p));
            tx.success();
        }
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

}
