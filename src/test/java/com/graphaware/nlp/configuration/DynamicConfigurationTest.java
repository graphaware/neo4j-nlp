package com.graphaware.nlp.configuration;

import com.graphaware.nlp.AbstractEmbeddedTest;
import com.graphaware.nlp.workflow.input.QueryBasedWorkflowInput;
import com.graphaware.nlp.workflow.input.WorkflowInputQueryConfiguration;
import org.codehaus.jackson.map.ObjectMapper;
import com.graphaware.common.kv.GraphKeyValueStore;
import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.dsl.request.PipelineSpecification;
import com.graphaware.nlp.dsl.result.WorkflowInstanceItemInfo;
import com.graphaware.nlp.module.NLPConfiguration;
import com.graphaware.nlp.module.NLPModule;
import com.graphaware.nlp.stub.StubTextProcessor;
import com.graphaware.nlp.workflow.task.WorkflowTask;
import com.graphaware.nlp.workflow.task.WorkflowTaskConfiguration;
import com.graphaware.nlp.workflow.task.WorkflowTaskInstanceItemInfo;
import com.graphaware.runtime.GraphAwareRuntime;
import com.graphaware.runtime.GraphAwareRuntimeFactory;
import com.graphaware.test.integration.EmbeddedDatabaseIntegrationTest;
import org.codehaus.jackson.map.SerializationConfig;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;

import java.lang.reflect.Field;
import java.util.HashMap;

import static com.graphaware.runtime.RuntimeRegistry.getStartedRuntime;
import java.util.List;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import static org.junit.Assert.*;

public class DynamicConfigurationTest extends AbstractEmbeddedTest {

    @Test
    public void testConfigurationCanStoreAndRetrievePipelines() {
        DynamicConfiguration configuration = new DynamicConfiguration(getDatabase());
        PipelineSpecification specification = new PipelineSpecification("custom", StubTextProcessor.class.getName());
        specification.setStopWords("hello,hihi");
        specification.setThreadNumber(4);
        configuration.storeCustomPipeline(specification);

        try (Transaction tx = getDatabase().beginTx()) {
            assertTrue(keyValueStore.hasKey("GA__NLP__PIPELINE_custom"));
            tx.success();
        }
    }

    @Test
    public void testConfigurationCanLoadCustomPipelineAsObject() {
        DynamicConfiguration configuration = new DynamicConfiguration(getDatabase());
        PipelineSpecification specification = new PipelineSpecification("custom", StubTextProcessor.class.getName());
        specification.setStopWords("hello,hihi");
        specification.setThreadNumber(4);
        configuration.storeCustomPipeline(specification);

        PipelineSpecification pipelineSpecification = configuration.loadCustomPipelines().get(0);
        assertEquals(specification.getName(), pipelineSpecification.getName());
        assertEquals(specification.getStopWords(), pipelineSpecification.getStopWords());
        assertEquals(specification.getTextProcessor(), pipelineSpecification.getTextProcessor());
        assertEquals(specification.getThreadNumber(), pipelineSpecification.getThreadNumber());
    }

    @Test
    public void testConfigurationCanRemovePipeline() {
        DynamicConfiguration configuration = new DynamicConfiguration(getDatabase());
        PipelineSpecification specification = new PipelineSpecification("custom", StubTextProcessor.class.getName());
        specification.setStopWords("hello,hihi");
        specification.setThreadNumber(4);
        configuration.storeCustomPipeline(specification);

        try (Transaction tx = getDatabase().beginTx()) {
            assertTrue(keyValueStore.hasKey("GA__NLP__PIPELINE_custom"));
            configuration.removePipeline("custom", StubTextProcessor.class.getName());
            assertFalse(keyValueStore.hasKey("GA__NLP__PIPELINE_custom"));
            tx.success();
        }
    }

    @Test
    public void testConfigurationValuesShouldBeLoadedFromPreviousState() throws Exception {
        resetSingleton();
        ObjectMapper mapper = new ObjectMapper();
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        keyValueStore = new GraphKeyValueStore(getDatabase());
        PipelineSpecification specification = new PipelineSpecification("custom", StubTextProcessor.class.getName());
        specification.setStopWords("hello,hihi");
        specification.setThreadNumber(4);
        try (Transaction tx = getDatabase().beginTx()) {
            String writeValueAsString = mapper.writeValueAsString(specification);
            keyValueStore.set("GA__NLP__PIPELINE_custom", writeValueAsString);
            keyValueStore.set("GA__NLP__SETTING_fallbackLanguage", "en");
            tx.success();
        }

        GraphAwareRuntime runtime = GraphAwareRuntimeFactory.createRuntime(getDatabase());
        runtime.registerModule(new NLPModule("NLP", NLPConfiguration.defaultConfiguration(), getDatabase()));
        runtime.start();
        runtime.waitUntilStarted();
        assertTrue(getStartedRuntime(getDatabase()).getModule(NLPModule.class).getNlpManager().getTextProcessorsManager()
        .getTextProcessor(StubTextProcessor.class.getName()).getPipelines().contains("custom"));
        assertTrue(getStartedRuntime(getDatabase()).getModule(NLPModule.class).getNlpManager().getConfiguration()
        .hasSettingValue(SettingsConstants.FALLBACK_LANGUAGE));
    }

    @Test
    public void testWorfklowItemsInfosShouldBeLoadedFromPreviousState() throws Exception {
        resetSingleton();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
        keyValueStore = new GraphKeyValueStore(getDatabase());
        QueryBasedWorkflowInput workflowInput = new QueryBasedWorkflowInput("test", getDatabase());
        DynamicConfiguration configuration = new DynamicConfiguration(getDatabase());
        workflowInput.setConfiguration(new WorkflowInputQueryConfiguration(new HashMap<>()));
        try (Transaction tx = getDatabase().beginTx()) {
            configuration.storeWorkflowInstanceItem(workflowInput);
            tx.success();
        }

        GraphAwareRuntime runtime = GraphAwareRuntimeFactory.createRuntime(getDatabase());
        runtime.registerModule(new NLPModule("NLP", NLPConfiguration.defaultConfiguration(), getDatabase()));
        runtime.start();
        runtime.waitUntilStarted();

    }

    private void resetSingleton() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field instance = NLPManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }
    
    @Test
    public void testLoadingInstances() throws Exception {
        resetSingleton();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
        keyValueStore = new GraphKeyValueStore(getDatabase());
        WorkflowTask workflowInput = new WorkflowTask("test", getDatabase());
        DynamicConfiguration configuration = new DynamicConfiguration(getDatabase());
        HashMap<String, Object> properties = new HashMap<>();
        properties.put(WorkflowTaskConfiguration.WORFKLOW_INPUT_NAME, "");
        properties.put(WorkflowTaskConfiguration.WORFKLOW_OUTPUT_NAME, "");
        properties.put(WorkflowTaskConfiguration.WORFKLOW_PROCESSOR_NAME, "");
        workflowInput.setConfiguration(new WorkflowTaskConfiguration(properties));
        try (Transaction tx = getDatabase().beginTx()) {
            configuration.storeWorkflowInstanceItem(workflowInput);
            tx.success();
        }
        List<WorkflowInstanceItemInfo> pipelineInstanceItems = configuration.loadPipelineInstanceItems(WorkflowTask.WORFKLOW_TASK_KEY_PREFIX);
        assertTrue(pipelineInstanceItems.size() == 1);
        WorkflowInstanceItemInfo info = pipelineInstanceItems.get(0);
        assertTrue(info instanceof WorkflowTaskInstanceItemInfo);
        assertEquals("test", info.getName());
        assertEquals("com.graphaware.nlp.workflow.task.WorkflowTask", info.getClassName());
    }
}
