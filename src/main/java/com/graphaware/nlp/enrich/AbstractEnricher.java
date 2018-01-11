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

import com.graphaware.common.util.Pair;
import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.dsl.request.ConceptRequest;
import com.graphaware.nlp.language.LanguageManager;
import com.graphaware.nlp.persistence.PersistenceRegistry;
import com.graphaware.nlp.persistence.constants.Labels;
import com.graphaware.nlp.persistence.persisters.Persister;
import com.graphaware.nlp.processor.TextProcessor;
import org.neo4j.graphdb.*;

import java.util.*;

public class AbstractEnricher {

    private final GraphDatabaseService database;

    private final PersistenceRegistry persistenceRegistry;

    private final NLPManager manager;

    public AbstractEnricher(GraphDatabaseService database, PersistenceRegistry persistenceRegistry) {
        this.database = database;
        this.persistenceRegistry = persistenceRegistry;
        this.manager = NLPManager.getInstance();
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

    protected Pair<Iterator<Node>, Node> getTagsIteratorFromRequest(ConceptRequest request) {
        Node annotatedNode = request.getAnnotatedNode();
        Node tagToBeAnnotated = null;
        if (annotatedNode == null) {
            tagToBeAnnotated = request.getTag();
        }
        Iterator<Node> tagsIterator;
        if (annotatedNode != null) {
            tagsIterator = getAnnotatedTextTags(annotatedNode);
        } else if (tagToBeAnnotated != null) {
            List<Node> proc = new ArrayList<>();
            proc.add(tagToBeAnnotated);
            tagsIterator = proc.iterator();
        } else {
            throw new RuntimeException("You need to specify or an annotated text or a list of tags");
        }

        return new Pair(tagsIterator, tagToBeAnnotated);
    }

    protected ResourceIterator<Node> getAnnotatedTextTags(Node annotatedNode) throws QueryExecutionException {
        Map<String, Object> params = new HashMap<>();
        params.put("id", annotatedNode.getId());
        Result queryRes = getDatabase().execute("MATCH (n)-[*..2]->"
                + "(t:" + getConfiguration().getLabelFor(Labels.Tag) + ") "
                + "where id(n) = {id} return distinct t", params);
        ResourceIterator<Node> tags = queryRes.columnAs("t");
        return tags;
    }

    public GraphDatabaseService getDatabase() {
        return database;
    }

    public PersistenceRegistry getPersistenceRegistry() {
        return persistenceRegistry;
    }

    public DynamicConfiguration getConfiguration() {
        return manager.getConfiguration();
    }

    protected Persister getPersister(Class clazz) {
        return persistenceRegistry.getPersister(clazz);
    }
}
