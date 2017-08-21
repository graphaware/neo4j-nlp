package com.graphaware.nlp.enrich;

import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.persistence.PersistenceRegistry;
import com.graphaware.nlp.persistence.persisters.Persister;
import org.neo4j.graphdb.GraphDatabaseService;

public class AbstractEnricher {

    private final GraphDatabaseService database;

    private final PersistenceRegistry persistenceRegistry;

    private final DynamicConfiguration configuration;

    public AbstractEnricher(GraphDatabaseService database, PersistenceRegistry persistenceRegistry, DynamicConfiguration configuration) {
        this.database = database;
        this.persistenceRegistry = persistenceRegistry;
        this.configuration = configuration;
    }

    public GraphDatabaseService getDatabase() {
        return database;
    }

    public PersistenceRegistry getPersistenceRegistry() {
        return persistenceRegistry;
    }

    public DynamicConfiguration getConfiguration() {
        return configuration;
    }

    protected Persister getPersister(Class clazz) {
        return persistenceRegistry.getPersister(clazz);
    }
}
