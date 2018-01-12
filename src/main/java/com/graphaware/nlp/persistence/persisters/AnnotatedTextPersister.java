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
import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.domain.Sentence;
import com.graphaware.nlp.persistence.PersistenceRegistry;
import com.graphaware.nlp.persistence.constants.Labels;
import com.graphaware.nlp.persistence.constants.Properties;
import com.graphaware.nlp.persistence.constants.Relationships;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.logging.Log;

import java.util.concurrent.atomic.AtomicReference;

public class AnnotatedTextPersister extends AbstractPersister implements Persister<AnnotatedText> {

    private static final Log LOG = LoggerFactory.getLogger(AnnotatedTextPersister.class);

    public AnnotatedTextPersister(GraphDatabaseService database, PersistenceRegistry registry) {
        super(database, registry);
    }
    
    @Override
    public Node persist(AnnotatedText annotatedText) {
        throw new UnsupportedOperationException("This cannot implemented for this persister");
    }

    @Override
    public Node persist(AnnotatedText annotatedText, String id, String txId) {
        LOG.info("Start storing annotatedText " + id);
        Node tmpAnnotatedNode = getIfExist(configuration().getLabelFor(Labels.AnnotatedText), Properties.PROPERTY_ID, id);
        final Node annotatedTextNode;
        if (tmpAnnotatedNode != null) {
            annotatedTextNode = tmpAnnotatedNode;
        } else {
            annotatedTextNode = getOrCreate(annotatedText, id, txId);

        }
        iterateSentencesAndStore(annotatedTextNode, annotatedText, id, txId);
        tmpAnnotatedNode = annotatedTextNode;

        LOG.info("end storing annotatedText " + id);
        return tmpAnnotatedNode;
    }

    @Override
    public AnnotatedText fromNode(Node node) {
        if (!node.hasLabel(configuration().getLabelFor(Labels.AnnotatedText))) {
            throw new RuntimeException("Expected an " + configuration().getLabelFor(Labels.AnnotatedText) + " node.");
        }
        AnnotatedText annotatedText = mapper().convertValue(node.getAllProperties(), AnnotatedText.class);

        node.getRelationships(configuration().getRelationshipFor(Relationships.CONTAINS_SENTENCE), Direction.OUTGOING).forEach(relationship -> {
            Sentence sentence = (Sentence) getPersister(Sentence.class).fromNode(relationship.getEndNode());
            annotatedText.addSentence(sentence);
        });

        return annotatedText;
    }

    @Override
    public boolean exists(String id) {
        return null != getIfExist(configuration().getLabelFor(Labels.AnnotatedText), Properties.PROPERTY_ID, id);
    }

    @Override
    public Node getOrCreate(AnnotatedText annotatedText, String id, String txId) {
        Node node = database.createNode(configuration().getLabelFor(Labels.AnnotatedText));
        node.setProperty(configuration().getPropertyKeyFor(Properties.PROPERTY_ID), id);
        node.setProperty(configuration().getPropertyKeyFor(Properties.NUM_TERMS), annotatedText.getTokens().size());

        return node;
    }

    @Override
    public void update(Node node, AnnotatedText object, String id) {

    }

    private void iterateSentencesAndStore(Node annotatedTextNode, AnnotatedText annotatedText, String id, String txId) {
        final AtomicReference<Node> previousSentenceReference = new AtomicReference<>();
        annotatedText.getSentences().sort((Sentence o1, Sentence o2) -> o1.compareTo(o2));
        annotatedText.getSentences().forEach((sentence) -> {
            Node sentenceNode = getPersister(Sentence.class).persist(sentence, id, txId);
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
