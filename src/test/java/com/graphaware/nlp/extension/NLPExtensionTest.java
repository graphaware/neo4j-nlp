package com.graphaware.nlp.extension;

import com.graphaware.nlp.NLPEvents;
import com.graphaware.nlp.annotation.NLPModuleExtension;
import com.graphaware.nlp.event.DefaultEvent;
import com.graphaware.nlp.event.EventDispatcher;
import com.graphaware.nlp.stub.StubEvents;
import com.graphaware.nlp.stub.StubExtension;
import com.graphaware.nlp.util.ServiceLoader;
import org.junit.Test;

import java.util.Map;
import static org.junit.Assert.*;

public class NLPExtensionTest {

    @Test
    public void testServiceLoaderCanLoadExtensions() {
        Map<String, NLPExtension> loadedExtensions = ServiceLoader.loadInstances(NLPModuleExtension.class);
        assertFalse(loadedExtensions.size() == 0);
    }

    @Test
    public void testExtensionCanRegisterEventListeners() {
        Map<String, NLPExtension> loadedExtensions = ServiceLoader.loadInstances(NLPModuleExtension.class);
        EventDispatcher dispatcher = new EventDispatcher();
        loadedExtensions.values().forEach(nlpExtension -> {
            nlpExtension.registerEventListeners(dispatcher);
        });
        dispatcher.notify(StubEvents.HELLO, new DefaultEvent("hello you"));
        NLPExtension nlpExtension = loadedExtensions.get(StubExtension.class.getName());
        assertEquals("hello you", ((StubExtension) nlpExtension).getSomeValue());
    }

}
