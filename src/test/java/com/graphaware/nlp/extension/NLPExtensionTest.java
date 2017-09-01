package com.graphaware.nlp.extension;

import com.graphaware.nlp.Events;
import com.graphaware.nlp.annotation.NLPModuleExtension;
import com.graphaware.nlp.event.DefaultEvent;
import com.graphaware.nlp.event.EventDispatcher;
import com.graphaware.nlp.stub.StubExtension;
import com.graphaware.nlp.util.ServiceLoader;
import org.junit.Test;

import java.util.Map;
import static org.junit.Assert.*;

public class NLPExtensionTest {

    @Test
    public void testServiceLoaderCanLoadExtensions() {
        Map<String, NLPExtension> loadedExtensions = ServiceLoader.loadInstances(NLPModuleExtension.class);
        assertEquals(1, loadedExtensions.size());
        loadedExtensions.keySet().forEach(k -> {
            assertEquals(StubExtension.class.getName(), k);
        });
    }

    @Test
    public void testExtensionCanRegisterEventListeners() {
        Map<String, NLPExtension> loadedExtensions = ServiceLoader.loadInstances(NLPModuleExtension.class);
        EventDispatcher dispatcher = new EventDispatcher();
        loadedExtensions.values().forEach(nlpExtension -> {
            nlpExtension.registerEventListeners(dispatcher);
        });
        dispatcher.notify(Events.POST_TEXT_ANNOTATION, new DefaultEvent("hello you"));
        loadedExtensions.values().forEach(nlpExtension -> {
            assertEquals("hello you", ((StubExtension) nlpExtension).getSomeValue());
        });
    }

}
