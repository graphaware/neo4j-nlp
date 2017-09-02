package com.graphaware.nlp.extension;

import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.event.EventDispatcher;

public interface NLPExtension {

    void registerEventListeners(EventDispatcher eventDispatcher);

    void setNLPManager(NLPManager nlpManager);
}
