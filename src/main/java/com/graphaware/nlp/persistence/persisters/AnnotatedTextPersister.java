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
package com.graphaware.nlp.persistence.persisters;

import com.graphaware.common.log.LoggerFactory;
import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.domain.*;
import com.graphaware.nlp.persistence.PersistenceRegistry;
import com.graphaware.nlp.persistence.constants.Labels;
import com.graphaware.nlp.persistence.constants.Properties;
import com.graphaware.nlp.persistence.constants.Relationships;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;

import java.util.concurrent.atomic.AtomicReference;


public class AnnotatedTextPersister extends AbstractPersister implements Persister<AnnotatedText> {

    private static final Log LOG = LoggerFactory.getLogger(AnnotatedTextPersister.class);

    public AnnotatedTextPersister(GraphDatabaseService database, DynamicConfiguration dynamicConfiguration, PersistenceRegistry registry) {
        super(database, dynamicConfiguration, registry);
    }

    @Override
    public Node persist(AnnotatedText annotatedText, String id, boolean force) {
        LOG.info("Start storing annotatedText " + id);
        Node tmpAnnotatedNode = getIfExist(configuration().getLabelFor(Labels.AnnotatedText), Properties.PROPERTY_ID, id);
        if (tmpAnnotatedNode == null || force) {
            final Node annotatedTextNode;
            if (tmpAnnotatedNode != null) {
                annotatedTextNode = tmpAnnotatedNode;
            } else {
                annotatedTextNode = getOrCreate(annotatedText, id, force);

            }
            iterateSentencesAndStore(annotatedTextNode, annotatedText, id, force);
            tmpAnnotatedNode = annotatedTextNode;
        } else {
            /*
            * Currently only labels could change so if the AnnotatedText already exist
            * only the Sentence are updated
             */
            annotatedText.getSentences().forEach((sentence) -> {
                getPersister(Sentence.class).persist(sentence, id, force);
            });
        }

        LOG.info("end storing annotatedText " + id);
        return tmpAnnotatedNode;
    }

    @Override
    public AnnotatedText fromNode(Node node) {
        return mapper().convertValue(node.getAllProperties(), AnnotatedText.class);
    }

    @Override
    public boolean exists(String id) {
        return null != getIfExist(configuration().getLabelFor(Labels.AnnotatedText), Properties.PROPERTY_ID, id);
    }

    @Override
    public Node getOrCreate(AnnotatedText annotatedText, String id, boolean force) {
        Node node = database.createNode(configuration().getLabelFor(Labels.AnnotatedText));
        node.setProperty(configuration().getPropertyKeyFor(Properties.PROPERTY_ID), id);
        node.setProperty(configuration().getPropertyKeyFor(Properties.NUM_TERMS), annotatedText.getTokens().size());

        return node;
    }

    @Override
    public void update(Node node, AnnotatedText object, String id) {

    }

    private void iterateSentencesAndStore(Node annotatedTextNode, AnnotatedText annotatedText, String id, boolean force) {
        final AtomicReference<Node> previousSentenceReference = new AtomicReference<>();
        annotatedText.getSentences().sort((Sentence o1, Sentence o2) -> o1.compareTo(o2));
        annotatedText.getSentences().forEach((sentence) -> {
            Node sentenceNode = getPersister(Sentence.class).persist(sentence, id, force);
            Node previousSentence = previousSentenceReference.get();
            boolean isFirstSentence = previousSentence == null;
            relateSentenceToAnnotatedText(sentenceNode, annotatedTextNode, isFirstSentence);
            if (!isFirstSentence) {
                relatePreviousSentenceToNext(previousSentence, sentenceNode);
            }
            previousSentenceReference.set(sentenceNode);
        });
    }

    private void relateSentenceToAnnotatedText(Node sentence, Node annotatedText, boolean isFirstSentence) {
        annotatedText.createRelationshipTo(
                sentence,
                configuration().getRelationshipFor(Relationships.CONTAINS_SENTENCE));
        if (isFirstSentence) {
            annotatedText.createRelationshipTo(sentence, configuration().getRelationshipFor(Relationships.FIRST_SENTENCE));
        }
    }

    private void relatePreviousSentenceToNext(Node previous, Node next) {
        previous.createRelationshipTo(next, configuration().getRelationshipFor(Relationships.NEXT_SENTENCE));
    }
}
