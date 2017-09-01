package com.graphaware.nlp.stub;

import com.graphaware.nlp.Events;
import com.graphaware.nlp.annotation.NLPModuleExtension;
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
        eventDispatcher.registerListener(Events.POST_TEXT_ANNOTATION, (event) -> {
            someValue = ((TextAnnotationEvent) event).getId();
        });

        eventDispatcher.registerListener(Events.POST_TEXT_ANNOTATION, (event) -> {
            addEventLabel(((TextAnnotationEvent) event).getAnnotatedNode());
        });
    }

    public String getSomeValue() {
        return someValue;
    }

    private void addEventLabel(Node node) {
        node.addLabel(Label.label("Event"));
    }
}
