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
package com.graphaware.nlp.persistence;

import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.domain.Keyword;
import com.graphaware.nlp.domain.Sentence;
import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.domain.VectorContainer;
import com.graphaware.nlp.persistence.persisters.*;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.HashMap;
import java.util.Map;

public class PersistenceRegistry {

    private final Map<Class, Persister> registeredPersisters = new HashMap<>();

    public PersistenceRegistry(GraphDatabaseService databaseService) {
        register(Tag.class, new TagPersister(databaseService, this));
        register(Sentence.class, new SentencePersister(databaseService, this));
        register(AnnotatedText.class, new AnnotatedTextPersister(databaseService, this));
        register(Keyword.class, new KeywordPersister(databaseService, this));
        register(VectorContainer.class, new VectorPersister(databaseService, this));
    }

    public final void register(Class clazz, Persister persister) {
        registeredPersisters.put(clazz, persister);
    }

    public Persister getPersister(Class clazz) {
        return registeredPersisters.get(clazz);
    }

}
