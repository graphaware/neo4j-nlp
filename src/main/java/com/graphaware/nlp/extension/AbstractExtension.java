package com.graphaware.nlp.extension;

import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.event.EventDispatcher;
import com.graphaware.nlp.persistence.persisters.Persister;
import com.graphaware.nlp.processor.TextProcessorsManager;
import org.neo4j.graphdb.GraphDatabaseService;

public abstract class AbstractExtension implements NLPExtension {

    protected NLPManager getNLPManager() {
        return NLPManager.getInstance();
    }

    protected DynamicConfiguration configuration() {
        return getNLPManager().getConfiguration();
    }

    protected Persister getPersister(Class clazz) {
        return getNLPManager().getPersister(clazz);
    }

    protected TextProcessorsManager getTextProcessorsManager() {
        return getNLPManager().getTextProcessorsManager();
    }

    protected GraphDatabaseService getDatabase() {
        return getNLPManager().getDatabase();
    }
    
    protected DynamicConfiguration getConfiguration() {
        return getNLPManager().getConfiguration();
    }

    @Override
    public void registerEventListeners(EventDispatcher eventDispatcher) {

    }

    @Override
    public void postLoaded() {
        
    }
}
