package com.graphaware.nlp.persistence;

import com.graphaware.common.log.LoggerFactory;
import com.graphaware.nlp.domain.*;
import com.graphaware.nlp.util.SentenceUtils;
import com.graphaware.nlp.util.TagUtils;
import com.graphaware.nlp.util.TypeConverter;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.graphaware.nlp.domain.SentimentLabels.*;
import static com.graphaware.nlp.domain.SentimentLabels.VeryPositive;
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

    public Node persist(AnnotatedText object, String id, boolean force) {
        LOG.info("Start storing annotatedText " + id);
        Node tmpAnnotatedNode = getIfExist(configuration().getLabelFor(Labels.AnnotatedText), Properties.PROPERTY_ID, id);
        if (tmpAnnotatedNode == null || force) {
            final Node annotatedTextNode;
            if ( tmpAnnotatedNode != null) {
                annotatedTextNode = tmpAnnotatedNode;
            } else {
                annotatedTextNode = createAnnotatedTextNode(id, object);

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

    private Node createAnnotatedTextNode(String id, AnnotatedText annotatedText) {
        Node node = database.createNode(configuration().getLabelFor(Labels.AnnotatedText));
        node.setProperty(configuration().getPropertyKeyFor(Properties.PROPERTY_ID), id);
        node.setProperty(configuration().getPropertyKeyFor(Properties.NUM_TERMS), annotatedText.getTokens().size());

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
            assignNamedEntityOnTag(tagNode, tag);
            assignPartOfSpeechOnTag(tagNode, tag);
            relateSentenceToTag(sentenceNode, tagNode, tag.getMultiplicity());
        });
    }

    private void assignNamedEntityOnTag(Node tagNode, Tag tag) {
        List<String> namedEntities = tag.getNeAsList();
        tagNode.setProperty(configuration().getPropertyKeyFor(Properties.NAMED_ENTITY), TypeConverter.convertStringListToArray(namedEntities));
        namedEntities.forEach(ner -> {
            String labelName = configuration().getPropertyKeyFor(Properties.NAMED_ENTITY_PREFIX) + TagUtils.getNamedEntityValue(ner);
            tagNode.addLabel(Label.label(labelName));
        });
    }

    private void assignPartOfSpeechOnTag(Node tagNode, Tag tag) {
        List<String> parts = tag.getPosAsList();
        tagNode.setProperty(configuration().getPropertyKeyFor(Properties.PART_OF_SPEECH), TypeConverter.convertStringListToArray(parts));
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
            }
            updateSentenceNode(newSentenceNode, sentenceId, sentence.getSentenceNumber(), MD5(sentence.getSentence()), sentence.getSentence());
            storeSentenceTags(sentence, newSentenceNode, id, force);
            storeSentenceTagOccurrences(sentence, newSentenceNode, force);
            storeUniversalDependenciesForSentence(sentence, newSentenceNode);
            storePhrases(sentence, newSentenceNode, force);
            assignSentimentLabel(sentence, newSentenceNode);
            sentenceNode = newSentenceNode;
        } else {
            assignSentimentLabel(sentence, sentenceNode);
        }

        return sentenceNode;
    }

    private void storeUniversalDependenciesForSentence(Sentence sentence, Node sentenceNode) {
        final Map<String, Long> tokenIdsToNodeIds = new HashMap<>();
        sentence.getTagOccurrences().values().forEach(occurence -> {
            occurence.forEach(tagOccurrence -> {
                Node tagOccurrenceNode = getTagOccurrenceInSentence(sentenceNode, tagOccurrence);
                if (tagOccurrenceNode == null) {
                    throw new RuntimeException("Expected to find a TagOccurrence node, got null");
                }
                tagOccurrence.getPartIds().forEach(tokenId -> {
                    tokenIdsToNodeIds.put(tokenId, tagOccurrenceNode.getId());
                });
            });
        });

        sentence.getTypedDependencies().forEach(typedDependency -> {
            if (!tokenIdsToNodeIds.containsKey(typedDependency.getSource()) || !tokenIdsToNodeIds.containsKey(typedDependency.getTarget())) {
                LOG.info("source: {} or target: {} for typed dependency not found", typedDependency.getSource(), typedDependency.getTarget());
                return;
            }

            Node sourceNode = database.getNodeById(tokenIdsToNodeIds.get(typedDependency.getSource()));
            Node targetNode = database.getNodeById(tokenIdsToNodeIds.get(typedDependency.getTarget()));
            relateTypedDependencySourceAndTarget(sourceNode, targetNode, typedDependency);
        });
    }

    private void relateTypedDependencySourceAndTarget(Node source, Node target, TypedDependency typedDependency) {
        RelationshipType relationshipType = RelationshipType.withName(typedDependency.getName().toUpperCase());
        Relationship relationship = source.createRelationshipTo(target, relationshipType);
        if (null != typedDependency.getSpecific()) {
            relationship.setProperty(configuration().getPropertyKeyFor(Properties.DEPENDENCY_SPECIFIC), typedDependency.getSpecific());
        }
        if (relationshipType.name().equals("ROOT")) {
            source.addLabel(configuration().getLabelFor(Labels.Root));
        }
    }

    private void assignSentimentLabel(Sentence sentence, Node sentenceNode) {
        int sentiment = sentence.getSentiment();
        Label sentimentLabel = SentenceUtils.getDefaultLabelForSentimentLevel(sentiment);
        if (sentimentLabel == null) {
            return;
        }
        sentenceNode.addLabel(configuration().getLabelFor(sentimentLabel));
    }

    private Node getTagOccurrenceInSentence(Node sentenceNode, PartOfTextOccurrence<Tag> tagOccurrence) {
        for (Relationship relationship : sentenceNode.getRelationships(configuration().getRelationshipFor(Relationships.SENTENCE_TAG_OCCURRENCE), Direction.OUTGOING)) {
            Node otherNode = relationship.getEndNode();
            if (otherNode.getProperty(configuration().getPropertyKeyFor(Properties.OCCURRENCE_BEGIN)).equals(tagOccurrence.getSpan().first())
                    && otherNode.getProperty(configuration().getPropertyKeyFor(Properties.OCCURRENCE_END)).equals(tagOccurrence.getSpan().second())) {
                return otherNode;
            }
        }

        return null;
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
        });
    }

    private void storePhrases(Sentence sentence, Node sentenceNode, boolean force) {
        sentence.getPhraseOccurrences().values().forEach(phraseOccurrenceAtPosition -> {
            phraseOccurrenceAtPosition.values().forEach(occurrence -> {
                Node phraseNode = getOrCreatePhrase(occurrence.getElement(), force);
                relateSentenceToPhrase(sentenceNode, phraseNode);
                Node phraseOccurrenceNode = createPhraseOccurrence(occurrence);
                relateSentenceToPhraseOccurrence(sentenceNode, phraseOccurrenceNode);
                relatePhraseOccurrenceToPhrase(phraseOccurrenceNode, phraseNode);
            });
        });
    }

    private void relateSentenceToPhrase(Node sentenceNode, Node phraseNode) {
        sentenceNode.createRelationshipTo(phraseNode,
                configuration().getRelationshipFor(Relationships.HAS_PHRASE));
    }

    private void relatePhraseOccurrenceToPhrase(Node phraseOccurrenceNode, Node phraseNode) {
        phraseOccurrenceNode.createRelationshipTo(phraseNode,
                configuration().getRelationshipFor(Relationships.PHRASE_OCCURRENCE_PHRASE));
    }

    private void relateSentenceToPhraseOccurrence(Node sentenceNode, Node phraseOccurrenceNode) {
        sentenceNode.createRelationshipTo(phraseOccurrenceNode,
                configuration().getRelationshipFor(Relationships.SENTENCE_PHRASE_OCCURRENCE));
    }

    private Node createPhraseOccurrence(PartOfTextOccurrence<Phrase> occurrence) {
        Node node = database.createNode(configuration().getLabelFor(Labels.PhraseOccurrence));
        node.setProperty(configuration().getPropertyKeyFor(Properties.START_POSITION), occurrence.getSpan().first());
        node.setProperty(configuration().getPropertyKeyFor(Properties.END_POSITION), occurrence.getSpan().second());

        return node;
    }

    private Node getOrCreatePhrase(Phrase phrase, boolean force) {
        Node node = database.findNode(configuration().getLabelFor(Labels.Phrase),
                configuration().getPropertyKeyFor(Properties.CONTENT_VALUE),
                phrase.getContent()
        );
        if (node != null && !force) {
            return node;
        }

        if (node == null) {
            node = database.createNode(configuration().getLabelFor(Labels.Phrase));
            updatePhrase(phrase, node);
        } else {
            updatePhrase(phrase, node);
        }

        return node;
    }

    private void updatePhrase(Phrase phrase, Node phraseNode) {
        phraseNode.setProperty(configuration().getPropertyKeyFor(Properties.CONTENT_VALUE), phrase.getContent());
        String type = phrase.getType() != null ? phrase.getType() : NLPDefaultValues.PHRASE_TYPE;
        phraseNode.setProperty(configuration().getPropertyKeyFor(Properties.PHRASE_TYPE), type);
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
