package com.graphaware.nlp.persistence;

import com.graphaware.common.log.LoggerFactory;
import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.domain.Properties;
import com.graphaware.nlp.domain.Sentence;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.logging.Log;

import java.util.concurrent.atomic.AtomicReference;

public class AnnotatedTextPersister extends AbstractPersister implements Persister<AnnotatedText> {

    private static final Log LOG = LoggerFactory.getLogger(AnnotatedTextPersister.class);

    public AnnotatedTextPersister(GraphDatabaseService database) {
        super(database);
    }

    @Override
    public Node persist(AnnotatedText object, String id) {
        return persist(object, id, false);
    }

    public Node persist(AnnotatedText object, String id, boolean force) {
        LOG.info("Start storing annotatedText " + id);
        Node tmpAnnotatedNode = getIfExist(getPersistenceConfiguration().getLabelFor(Labels.AnnotatedText), Properties.PROPERTY_ID, id);
        if (tmpAnnotatedNode == null || force) {
            final Node annotatedTextNode;
            if ( tmpAnnotatedNode != null) {
                annotatedTextNode = tmpAnnotatedNode;
            } else {
                annotatedTextNode = database.createNode(getPersistenceConfiguration().getLabelFor(Labels.AnnotatedText));
                annotatedTextNode.setProperty(Properties.PROPERTY_ID, id);
                annotatedTextNode.setProperty(Properties.NUM_TERMS, object.getTokens().size());
                final AtomicReference<Node> previousSentenceReference = new AtomicReference<>();
                object.getSentences().sort((Sentence o1, Sentence o2) -> o1.compareTo(o2));

                object.getSentences().forEach((sentence) -> {
                    Node sentenceNode = sentence.storeOnGraph(database, force);
                    annotatedTextNode.createRelationshipTo(sentenceNode, CONTAINS_SENTENCE);
                    Node previousSentence = previousSentenceReference.get();
                    if (previousSentence == null) {
                        annotatedTextNode.createRelationshipTo(sentenceNode, FIRST_SENTENCE);
                    } else {
                        previousSentence.createRelationshipTo(sentenceNode, NEXT_SENTENCE);
                    }
                    previousSentenceReference.set(sentenceNode);
                    List<Phrase> phraseOccurrences = sentence.getPhraseOccurrence();
                    phraseOccurrences.stream().forEach((phrase) -> {
                        if (phrase.getReference() != null) {
                            Node phraseNode = phrase.getOrCreate(database, force);
                            Node referredPhraseNode = phrase.getReference().getOrCreate(database, force);
                            phraseNode.createRelationshipTo(referredPhraseNode, REFER_TO);
                        }
                    });
                });
                tmpAnnotatedNode = annotatedTextNode;
            }

        } else {
            /*
            * Currently only labels could change so if the AnnotatedText already exist
            * only the Sentence are updated
             */
            sentences.stream().forEach((sentence) -> {
                sentence.storeOnGraph(database, force);
            });
        }
        node = tmpAnnotatedNode;
        LOG.info("end storing annotatedText " + id);
        return tmpAnnotatedNode;
    }

    @Override
    public AnnotatedText fromNode(Node node) {
        return mapper().convertValue(node.getAllProperties(), AnnotatedText.class);
    }

    @Override
    public boolean exists(String id) {
        return null != getIfExist(getPersistenceConfiguration().getLabelFor(Labels.AnnotatedText), Properties.PROPERTY_ID, id);
    }


}
