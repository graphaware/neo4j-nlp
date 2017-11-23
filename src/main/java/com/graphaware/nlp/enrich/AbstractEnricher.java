/*
 * Copyright (c) 2013-2017 GraphAware
 *
 * This file is part of the GraphAware Framework.
 *
 * GraphAware Framework is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package com.graphaware.nlp.enrich;

import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.language.LanguageManager;
import com.graphaware.nlp.persistence.PersistenceRegistry;
import com.graphaware.nlp.persistence.persisters.Persister;
import com.graphaware.nlp.processor.TextProcessor;
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

    protected Tag tryToAnnotate(String parentConcept, String language, TextProcessor nlpProcessor) {
        Tag annotateTag = null;
        if (LanguageManager.getInstance().isLanguageSupported(language)) {
            annotateTag = nlpProcessor.annotateTag(parentConcept, language);
        }
        if (annotateTag == null) {
            annotateTag = new Tag(parentConcept, language);
        }
        return annotateTag;
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
