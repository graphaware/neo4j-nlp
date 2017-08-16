package com.graphaware.nlp.persistence;

import com.graphaware.common.log.LoggerFactory;
import com.graphaware.nlp.domain.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.logging.Log;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.graphaware.nlp.util.HashFunctions.MD5;

public class AnnotatedTextPersister extends AbstractPersister implements Persister<AnnotatedText> {

    private static final Log LOG = LoggerFactory.getLogger(AnnotatedTextPersister.class);

    public AnnotatedTextPersister(GraphDatabaseService database) {
        super(database);
    }

    @Override
    public Node persist(AnnotatedText object, String id) {
        return persist(object, id, false);
    }

    private Node createAnnotatedTextNode(String id) {
        Node node = database.createNode(configuration().getLabelFor(Labels.AnnotatedText));
        node.setProperty(configuration().getPropertyKeyFor(Properties.PROPERTY_ID), id);

        return node;
    }

    private Node createSentenceNode(String id, int sentenceNumber, String hash, String text) {
        Node node = database.createNode(configuration().getLabelFor(Labels.Sentence));
        updateSentenceNode(node, id, sentenceNumber, hash, text);

        return node;
    }

    private void updateSentenceNode(Node node, String id, int sentenceNumber, String hash, String text) {
        node.setProperty(configuration().getPropertyKeyFor(Properties.PROPERTY_ID), id);
        node.setProperty(configuration().getPropertyKeyFor(Properties.SENTENCE_NUMBER), sentenceNumber);
        node.setProperty(configuration().getPropertyKeyFor(Properties.HASH), hash);
        node.setProperty(configuration().getPropertyKeyFor(Properties.TEXT), text);
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

    private void storeSentenceTags(Sentence sentence, Node sentenceNode, String id, boolean force) {
        sentence.getTags().forEach(tag -> {
            Node tagNode = getOrCreateTag(tag, force);
            relateSentenceToTag(sentenceNode, tagNode, tag.getMultiplicity());
        });
    }

    private void storeSentenceTagOccurrences(Sentence sentence, Node sentenceNode, boolean force) {
        sentence.getTagOccurrences().values().forEach(occurrence -> {
            for (PartOfTextOccurrence<Tag> tagAtPosition : occurrence) {
                Node tagNode = getOrCreateTag(tagAtPosition.getElement(), force);
                Node tagOccurrenceNode = createTagOccurrenceNode(tagAtPosition);
                relateTagOccurrenceToTag(tagOccurrenceNode, tagNode);
                relateSentenceToTagOccurrence(sentenceNode, tagOccurrenceNode);
            }
        });
    }

    private void relateSentenceToTagOccurrence(Node sentenceNode, Node tagOccurrenceNode) {
        sentenceNode.createRelationshipTo(tagOccurrenceNode, configuration().getRelationshipFor(Relationships.SENTENCE_TAG_OCCURRENCE));
    }

    private void relateSentenceToTag(Node sentenceNode, Node tagNode, int multiplicity) {
        Relationship rel = sentenceNode.createRelationshipTo(tagNode, configuration().getRelationshipFor(Relationships.HAS_TAG));
        rel.setProperty(configuration().getPropertyKeyFor(Properties.TF), multiplicity);
    }

    private Node createTagOccurrenceNode(PartOfTextOccurrence<Tag> occurrence) {
        Node node = database.createNode(configuration().getLabelFor(Labels.TagOccurrence));
        node.setProperty(configuration().getPropertyKeyFor(Properties.OCCURRENCE_BEGIN), occurrence.getSpan().first());
        node.setProperty(configuration().getPropertyKeyFor(Properties.OCCURRENCE_END), occurrence.getSpan().second());

        return node;
    }

    private void relateTagOccurrenceToTag(Node tagOccurrence, Node tag) {
        tagOccurrence.createRelationshipTo(tag, configuration().getRelationshipFor(Relationships.TAG_OCCURRENCE_TAG));
    }

    private Node getOrCreateTag(Tag tag, boolean force) {
        Node node = getIfExist(
                configuration().getLabelFor(Labels.Tag),
                configuration().getPropertyKeyFor(Properties.PROPERTY_ID),
                tag.getId());

        if (null == node) {
            node = database.createNode(configuration().getLabelFor(Labels.Tag));
            updateTag(node, tag.getId(), tag.getLemma(), tag.getLanguage());

            return node;
        }

        if (force) {
            updateTag(node, tag.getId(), tag.getLemma(), tag.getLanguage());
        }

        return node;
    }

    private void updateTag(Node node, String id, String value, String language) {
        node.setProperty(configuration().getPropertyKeyFor(Properties.PROPERTY_ID), id);
        node.setProperty(configuration().getPropertyKeyFor(Properties.LANGUAGE), language);
        node.setProperty(configuration().getPropertyKeyFor(Properties.CONTENT_VALUE), value);
    }

    private Node storeSentence(Sentence sentence, String id, boolean force) {
        String sentenceId = String.format("%s_%s", id, sentence.getSentenceNumber());
        Node sentenceNode = getIfExist(configuration().getLabelFor(Labels.Sentence), configuration().getPropertyKeyFor(Properties.PROPERTY_ID), sentenceId);
        if (sentenceNode == null || force) {
            Node newSentenceNode;
            if (sentenceNode == null) {
                newSentenceNode = createSentenceNode(sentenceId, sentence.getSentenceNumber(), MD5(sentence.getSentence()), sentence.getSentence());
            } else {
                newSentenceNode = sentenceNode;
                updateSentenceNode(newSentenceNode, sentenceId, sentence.getSentenceNumber(), MD5(sentence.getSentence()), sentence.getSentence());
                storeSentenceTags(sentence, newSentenceNode, id, force);
                storeSentenceTagOccurrences(sentence, newSentenceNode, force);
//              storeTags(database, newSentenceNode, force);
//              storePhrases(database, newSentenceNode, force);
//              sentenceNode = newSentenceNode;
//              assignSentimentLabel(sentenceNode);
            }
            sentenceNode = newSentenceNode;
        } else {
//            assignSentimentLabel(sentenceNode);
        }

        return sentenceNode;
    }

    private void iterateSentencesAndStore(Node annotatedTextNode, AnnotatedText annotatedText, String id, boolean force) {
        final AtomicReference<Node> previousSentenceReference = new AtomicReference<>();
        annotatedText.getSentences().sort((Sentence o1, Sentence o2) -> o1.compareTo(o2));
        annotatedText.getSentences().forEach((sentence) -> {
            Node sentenceNode = storeSentence(sentence, id, force);
            Node previousSentence = previousSentenceReference.get();
            boolean isFirstSentence = previousSentence == null;
            relateSentenceToAnnotatedText(sentenceNode, annotatedTextNode, isFirstSentence);
            if (!isFirstSentence) {
                relatePreviousSentenceToNext(previousSentence, sentenceNode);
            }
            previousSentenceReference.set(sentenceNode);

            //@todo extract this
//            List<Phrase> phraseOccurrences = sentence.getPhraseOccurrence();
//            phraseOccurrences.stream().forEach((phrase) -> {
//                if (phrase.getReference() != null) {
//                    Node phraseNode = phrase.getOrCreate(database, force);
//                    Node referredPhraseNode = phrase.getReference().getOrCreate(database, force);
//                    phraseNode.createRelationshipTo(referredPhraseNode, REFER_TO);
//                }
//            });
        });
    }

    public Node persist(AnnotatedText object, String id, boolean force) {
        LOG.info("Start storing annotatedText " + id);
        Node tmpAnnotatedNode = getIfExist(configuration().getLabelFor(Labels.AnnotatedText), Properties.PROPERTY_ID, id);
        if (tmpAnnotatedNode == null || force) {
            final Node annotatedTextNode;
            if ( tmpAnnotatedNode != null) {
                annotatedTextNode = tmpAnnotatedNode;
            } else {
                annotatedTextNode = createAnnotatedTextNode(id);
                annotatedTextNode.setProperty(configuration().getPropertyKeyFor(Properties.NUM_TERMS), object.getTokens().size());

            }
            iterateSentencesAndStore(annotatedTextNode, object, id, force);
            tmpAnnotatedNode = annotatedTextNode;
        } else {
            /*
            * Currently only labels could change so if the AnnotatedText already exist
            * only the Sentence are updated
             */
            object.getSentences().forEach((sentence) -> {
                storeSentence(sentence, id, force);
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


}
