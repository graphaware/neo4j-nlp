/*
 * Copyright (c) 2013-2016 GraphAware
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
package com.graphaware.nlp.domain;

import static com.graphaware.nlp.domain.Labels.PhraseOccurrence;
import static com.graphaware.nlp.domain.SentimentLabels.*;
import static com.graphaware.nlp.domain.Labels.Sentence;
import static com.graphaware.nlp.domain.Labels.TagOccurrence;
import static com.graphaware.nlp.domain.Properties.END_POSITION;
import static com.graphaware.nlp.domain.Properties.HASH;
import static com.graphaware.nlp.domain.Properties.PROPERTY_ID;
import static com.graphaware.nlp.domain.Properties.SENTENCE_NUMBER;
import static com.graphaware.nlp.domain.Properties.START_POSITION;
import static com.graphaware.nlp.domain.Properties.TEXT;
import static com.graphaware.nlp.domain.Relationships.HAS_PHRASE;
import static com.graphaware.nlp.domain.Relationships.HAS_TAG;
import static com.graphaware.nlp.domain.Relationships.PHRASE_OCCURRENCE_PHRASE;
import static com.graphaware.nlp.domain.Relationships.SENTENCE_PHRASE_OCCURRENCE;
import static com.graphaware.nlp.domain.Relationships.SENTENCE_TAG_OCCURRENCE;
import static com.graphaware.nlp.domain.Relationships.TAG_OCCURRENCE_TAG;
import static com.graphaware.nlp.util.HashFunctions.MD5;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.*;

public class Sentence implements Persistable, Serializable, Comparable<Sentence> {
    
    private static final long serialVersionUID = -1L;

    public static final int NO_SENTIMENT = -1;

    private Map<String, Tag> tags;
    private Map<Integer, List<PartOfTextOccurrence<Tag>>> tagOccurrences = new HashMap<>();
    private Map<Integer, Map<Integer, PartOfTextOccurrence<Phrase>>> phraseOccurrences = new HashMap<>();
    private List<TypedDependency> typedDependencies = new ArrayList<>();

    private final String sentence;
    private int sentiment = NO_SENTIMENT;

    private boolean store = false;
    private String id;
    private int sentenceNumber;

    public Sentence(String sentence, boolean store, String id, int sentenceNumber) {
        this(sentence, id);
        this.store = store;
        this.sentenceNumber = sentenceNumber;
    }

    public Sentence(String sentence, String id) {
        this.tags = new HashMap<>();
        this.tagOccurrences = new HashMap<>();
        this.sentence = sentence;
        this.id = id;
    }

    public Collection<Tag> getTags() {
        return tags.values();
    }

    public Tag addTag(Tag tag) {
        if (tags.containsKey(tag.getLemma())) {
            Tag result = tags.get(tag.getLemma());
            result.incMultiplicity();
            return result;
        } else {
            tags.put(tag.getLemma(), tag);
            return tag;
        }
    }

    public int getSentiment() {
        return sentiment;
    }

    public void setSentiment(int sentiment) {
        this.sentiment = sentiment;
    }

    public String getId() {
        return id;
    }

    public void addTagOccurrence(int begin, int end, Tag tag) {
        if (begin < 0) {
            throw new RuntimeException("Begin cannot be negative (for tag: " + tag.getLemma() + ")");
        }
        if (tagOccurrences.containsKey(begin))
          tagOccurrences.get(begin).add(new PartOfTextOccurrence<>(tag, begin, end));
        else
          tagOccurrences.put(begin, new ArrayList<>(Arrays.asList(new PartOfTextOccurrence<>(tag, begin, end))));
    }

    public void addTagOccurrence(int begin, int end, Tag tag, List<String> tokenIds) {
        if (begin < 0) {
            throw new RuntimeException("Begin cannot be negative (for tag: " + tag.getLemma() + ")");
        }
        if (tagOccurrences.containsKey(begin))
            tagOccurrences.get(begin).add(new PartOfTextOccurrence<>(tag, begin, end, tokenIds));
        else
            tagOccurrences.put(begin, new ArrayList<>(Arrays.asList(new PartOfTextOccurrence<>(tag, begin, end, tokenIds))));
    }

    public void addTypedDependency(TypedDependency typedDependency) {
        this.typedDependencies.add(typedDependency);
    }

    public List<TypedDependency> getTypedDependencies() {
        return typedDependencies;
    }

    //Currently used only for testing purpose
    public Tag getTagOccurrence(int begin) {
        if (begin < 0) {
            throw new RuntimeException("Begin cannot be negative");
        }
        List<PartOfTextOccurrence<Tag>> occurrence = tagOccurrences.get(begin);
        if (occurrence != null) {
            return occurrence.get(0).getElement(); // TO DO: take into account that more than one PartOfTextOccurrence is possible
        } else {
            return null;
        }
    }

    public void addPhraseOccurrence(int begin, int end, Phrase phrase) {
        if (begin < 0) {
            throw new RuntimeException("Begin cannot be negative (for phrase: " + phrase.getContent() + ")");
        }
        if (phraseOccurrences == null) {
            phraseOccurrences = new HashMap<>();
        }
        if (!phraseOccurrences.containsKey(begin)) {
            phraseOccurrences.put(begin, new HashMap<>());
        }
        //Will update end if already exist
        phraseOccurrences.get(begin).put(end, new PartOfTextOccurrence<>(phrase, begin, end));
    }

    //Currently used only for testing purpose
    public List<Phrase> getPhraseOccurrence(int begin) {
        if (begin < 0) {
            throw new RuntimeException("Begin cannot be negative");
        }
        Map<Integer, PartOfTextOccurrence<Phrase>> occurrence = phraseOccurrences.get(begin);

        if (occurrence != null) {
            List<Phrase> result = new ArrayList<>();
            occurrence.values().stream().forEach((item) -> {
                result.add(item.getElement());
            });
            return result;
        } else {
            return new ArrayList<>();
        }
    }

    public Phrase getPhraseOccurrence(int begin, int end) {
        if (begin < 0) {
            throw new RuntimeException("Begin cannot be negative");
        }
        Map<Integer, PartOfTextOccurrence<Phrase>> occurrences = phraseOccurrences.get(begin);

        if (occurrences != null && occurrences.containsKey(end)) {
            return occurrences.get(end).getElement();
        }
        return null;
    }

    public List<Phrase> getPhraseOccurrence() {
        List<Phrase> result = new ArrayList<>();
        phraseOccurrences.values().stream().forEach((phraseList) -> {
            phraseList.values().stream().forEach((item) -> {
                result.add(item.getElement());
            });
        });

        return result;

    }

    @Override
    public Node storeOnGraph(GraphDatabaseService database, boolean force) {
        Node sentenceNode = checkIfExist(database, id);
        if (sentenceNode == null || force) {
            try (Transaction tx = database.beginTx();) {
                Node newSentenceNode;
                if (sentenceNode == null)
                    newSentenceNode = database.createNode(Sentence);
                else 
                    newSentenceNode = sentenceNode;
                newSentenceNode.setProperty(HASH, MD5(sentence));
                newSentenceNode.setProperty(PROPERTY_ID, id);
                newSentenceNode.setProperty(SENTENCE_NUMBER, sentenceNumber);
                if (store) {
                    newSentenceNode.setProperty(TEXT, sentence);
                }
                storeTags(database, newSentenceNode, force);
                storePhrases(database, newSentenceNode, force);
                sentenceNode = newSentenceNode;
                assignSentimentLabel(sentenceNode);
                tx.success();
            }
        } else {
            assignSentimentLabel(sentenceNode);
        }
        return sentenceNode;
    }

    private void storeTags(GraphDatabaseService database, Node newSentenceNode, boolean force) {
        tags.values().stream().forEach((tag) -> {
            Node tagNode = tag.storeOnGraph(database, force);
            Relationship hasTagRel = newSentenceNode.createRelationshipTo(tagNode, HAS_TAG);
            hasTagRel.setProperty("tf", tag.getMultiplicity());
        });

        Map<String, Long> tokenIdToTagOccurenceNodeIdMap = new HashMap<>();

        tagOccurrences.values().stream().forEach((tagOccurrences) -> {
            for (PartOfTextOccurrence<Tag> tagOccurrenceAtPosition: tagOccurrences) {
                Node tagNode = tagOccurrenceAtPosition.getElement().getOrCreate(database, force);
                Node tagOccurrenceNode = database.createNode(TagOccurrence);
                for (String tokenId : tagOccurrenceAtPosition.getPartIds()) {
                    tokenIdToTagOccurenceNodeIdMap.put(tokenId, tagOccurrenceNode.getId());
                }
                tagOccurrenceNode.setProperty(START_POSITION, tagOccurrenceAtPosition.getSpan().first());
                tagOccurrenceNode.setProperty(END_POSITION, tagOccurrenceAtPosition.getSpan().second());
                newSentenceNode.createRelationshipTo(tagOccurrenceNode, SENTENCE_TAG_OCCURRENCE);
                tagOccurrenceNode.createRelationshipTo(tagNode, TAG_OCCURRENCE_TAG);
            }
        });

        typedDependencies.forEach(typedDependency -> {
            if (!tokenIdToTagOccurenceNodeIdMap.containsKey(typedDependency.getSource())) {
                System.out.println(String.format("could not find reference in map for %s", typedDependency.getSource()));
                return;
            }

            if (!tokenIdToTagOccurenceNodeIdMap.containsKey(typedDependency.getTarget())) {
                System.out.println(String.format("could not find reference in map for %s", typedDependency.getTarget()));
                return;
            }

            Node source = database.getNodeById(tokenIdToTagOccurenceNodeIdMap.get(typedDependency.getSource()));
            Node target = database.getNodeById(tokenIdToTagOccurenceNodeIdMap.get(typedDependency.getTarget()));
            if (source.getId() == target.getId()) {
                return;
            }
            String relType = typedDependency.getName().toUpperCase();
            Relationship dependencyRelationship = source.createRelationshipTo(target, RelationshipType.withName(relType));
            if (null != typedDependency.getSpecific()) {
                dependencyRelationship.setProperty("specifc", typedDependency.getSpecific());
            }

            System.out.println(String.format("Created relationship from %s to %s with type %s",
                    typedDependency.getSource(),
                    typedDependency.getTarget(),
                    relType));

        });
    }

    private void storePhrases(GraphDatabaseService database, Node newSentenceNode, boolean force) {
        if (phraseOccurrences != null) {
            phraseOccurrences.values().stream().forEach((phraseOccurrencesAtPosition) -> {
                phraseOccurrencesAtPosition.values().stream().forEach((phraseOccurrence) -> {
                    Node phraseNode = phraseOccurrence.getElement().storeOnGraph(database, force);
                    newSentenceNode.createRelationshipTo(phraseNode, HAS_PHRASE);
                    Node phraseOccurrenceNode = database.createNode(PhraseOccurrence);
                    phraseOccurrenceNode.setProperty(START_POSITION, phraseOccurrence.getSpan().first());
                    phraseOccurrenceNode.setProperty(END_POSITION, phraseOccurrence.getSpan().second());
                    newSentenceNode.createRelationshipTo(phraseOccurrenceNode, SENTENCE_PHRASE_OCCURRENCE);
                    phraseOccurrenceNode.createRelationshipTo(phraseNode, PHRASE_OCCURRENCE_PHRASE);
                    //TODO: Add relationship with tags
                });
            });
        }
    }

    private void assignSentimentLabel(Node sentenceNode) {
        switch (sentiment) {
            case 0:
                sentenceNode.addLabel(VeryNegative);
                break;
            case 1:
                sentenceNode.addLabel(Negative);
                break;
            case 2:
                sentenceNode.addLabel(Neutral);
                break;
            case 3:
                sentenceNode.addLabel(Positive);
                break;
            case 4:
                sentenceNode.addLabel(VeryPositive);
                break;
            default:
                break;
        }
    }

    public String getSentence() {
        return sentence;
    }

    public static Sentence load(Node sentenceNode) {
        if (!sentenceNode.hasProperty(TEXT)) {
            throw new RuntimeException("Sentences need to contain text inside to can extract sentiment");
        }
        String text = (String) sentenceNode.getProperty(TEXT);
        String id = (String) sentenceNode.getProperty(PROPERTY_ID);
        Integer sentenceNumber = (Integer) sentenceNode.getProperty(SENTENCE_NUMBER);
        return new Sentence(text, true, id, sentenceNumber);
    }

    private Node checkIfExist(GraphDatabaseService database, Object id) {
        if (id != null) {
            ResourceIterator<Node> findNodes = database.findNodes(Labels.Sentence, Properties.PROPERTY_ID, id);
            if (findNodes.hasNext()) {
                return findNodes.next();
            }
        }
        return null;
    }
    
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeObject(tags);        
    }
   
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        this.tags = (Map<String, Tag>)s.readObject();
    }

    @Override
    public int compareTo(Sentence o) {
        if (o == null || !(o instanceof Sentence))
            return 1;
        return this.sentenceNumber - o.sentenceNumber;
    }
}
