package com.graphaware.nlp.stub;

import com.graphaware.nlp.NLPEvents;
import com.graphaware.nlp.annotation.NLPModuleExtension;
import com.graphaware.nlp.event.DefaultEvent;
import com.graphaware.nlp.event.EventDispatcher;
import com.graphaware.nlp.event.TextAnnotationEvent;
import com.graphaware.nlp.extension.AbstractExtension;
import com.graphaware.nlp.extension.NLPExtension;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

@NLPModuleExtension(name = "AWESOME-EXTENSION")
public class StubExtension extends AbstractExtension implements NLPExtension {

    private String someValue;

    @Override
    public void registerEventListeners(EventDispatcher eventDispatcher) {
        eventDispatcher.registerListener(StubEvents.HELLO, (event) -> {
            someValue = ((DefaultEvent) event).getValue();
        });

        eventDispatcher.registerListener(NLPEvents.POST_TEXT_ANNOTATION, (event) -> {
            addEventLabel(((TextAnnotationEvent) event).getAnnotatedNode());
            someValue = ((TextAnnotationEvent) event).getId();
        });
    }

    public String getSomeValue() {
        return someValue;
    }

    private void addEventLabel(Node node) {
        node.addLabel(Label.label("STUB_Event"));
    }

}
