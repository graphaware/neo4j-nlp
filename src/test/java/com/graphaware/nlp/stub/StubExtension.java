package com.graphaware.nlp.stub;

import com.graphaware.nlp.Events;
import com.graphaware.nlp.annotation.NLPModuleExtension;
import com.graphaware.nlp.event.DefaultEvent;
import com.graphaware.nlp.event.EventDispatcher;
import com.graphaware.nlp.extension.AbstractExtension;
import com.graphaware.nlp.extension.NLPExtension;

@NLPModuleExtension(name = "AWESOME-EXTENSION")
public class StubExtension extends AbstractExtension implements NLPExtension {

    private String someValue;

    @Override
    public void registerEventListeners(EventDispatcher eventDispatcher) {
        eventDispatcher.registerListener(Events.POST_TEXT_ANNOTATION, (event) -> {
            someValue = ((DefaultEvent) event).getValue();
        });
    }

    public String getSomeValue() {
        return someValue;
    }
}
