package com.graphaware.nlp.event;

import com.graphaware.nlp.NLPEvents;
import org.junit.Test;
import java.util.*;

import static org.junit.Assert.*;

public class EventDispatcherTest {

    private static final String TEXT1 = "HELLO WORLD";

    @Test
    public void testEventsAreReceivedFromDispatcher() {
        EventDispatcher eventDispatcher = new EventDispatcher();
        eventDispatcher.registerListener(NLPEvents.POST_TEXT_ANNOTATION, (event) -> {
            String text = ((GenericEvent) event).getText();
            assertEquals(TEXT1, text);
        });

        eventDispatcher.notify(NLPEvents.POST_TEXT_ANNOTATION, new GenericEvent(TEXT1, "hello"));
    }

    @Test
    public void testEventListenersAreNotifiedInOrder() {
        EventDispatcher eventDispatcher = new EventDispatcher();
        final List<String> out = new ArrayList<>();
        eventDispatcher.registerListener(NLPEvents.POST_TEXT_ANNOTATION, (event) -> {
            out.add("hello100");
        }, 100 );
        eventDispatcher.registerListener(NLPEvents.POST_TEXT_ANNOTATION, (event) ->{
            out.add("hello50");
        }, 50);
        eventDispatcher.notify(NLPEvents.POST_TEXT_ANNOTATION, new GenericEvent("hello", "1"));
        assertEquals("hello50", out.get(0));
        assertEquals("hello100", out.get(1));
    }

    private class GenericEvent implements Event {

        private final String text;

        private final String id;

        public GenericEvent(String text, String id) {
            this.text = text;
            this.id = id;
        }

        public String getText() {
            return text;
        }

        public String getId() {
            return id;
        }
    }
}
