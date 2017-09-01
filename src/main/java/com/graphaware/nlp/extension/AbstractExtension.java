package com.graphaware.nlp.extension;

import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.persistence.persisters.Persister;
import com.graphaware.nlp.processor.TextProcessorsManager;

public abstract class AbstractExtension implements NLPExtension {

    protected NLPManager nlpManager;

    @Override
    public void setNLPManager(NLPManager nlpManager) {
        this.nlpManager = nlpManager;
    }

    protected DynamicConfiguration configuration() {
        return nlpManager.getConfiguration();
    }

    protected Persister getPersister(Class clazz) {
        return nlpManager.getPersister(clazz);
    }

    protected TextProcessorsManager getTextProcessorsManager() {
        return nlpManager.getTextProcessorsManager();
    }
}
