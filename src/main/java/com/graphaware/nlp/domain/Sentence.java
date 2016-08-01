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

import static com.graphaware.nlp.domain.SentimentLabels.*;
import static com.graphaware.nlp.domain.Labels.Sentence;
import static com.graphaware.nlp.domain.Properties.HASH;
import static com.graphaware.nlp.domain.Properties.PROPERTY_ID;
import static com.graphaware.nlp.domain.Properties.TEXT;
import static com.graphaware.nlp.domain.Relationships.HAS_TAG;
import static com.graphaware.nlp.util.HashFunctions.MD5;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;

public class Sentence implements Persistable {

    public static final int NO_SENTIMENT = -1;
    
    private final Map<String, Tag> tags;
    private Map<Integer, PartOfTextOccurrence<Tag>> tagOccurrences;
    private Map<Integer, Map<Integer, PartOfTextOccurrence<Phrase>>> phraseOccurrences;

    private final String sentence;
    private int sentiment = NO_SENTIMENT;
    
    private boolean store = false;
    private String id;

    public Sentence(String sentence, boolean store, String id) {
        this(sentence, id);
        this.store = store;
    }

    public Sentence(String sentence, String id) {
        this.tags = new HashMap<>();
        this.sentence = sentence;
        this.id = id;
    }

    public Collection<Tag> getTags() {
        return tags.values();
    }

    public void addTag(Tag tag) {
        if (tags.containsKey(tag.getLemma())) {
            tags.get(tag.getLemma()).incMultiplicity();
        } else {
            tags.put(tag.getLemma(), tag);
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
            throw new RuntimeException("Begin cannot be negative (for tag: " + tag.getLemma() + ")" );
        }
        if (tagOccurrences == null) {
            tagOccurrences = new HashMap<>();
        }
        //Will update end if already exist
        tagOccurrences.put(begin, new PartOfTextOccurrence<>(tag, begin, end));
    }

    //Currently used only for testing purpose
    public Tag getTagOccurrence(int begin) {
        if (begin < 0) {
            throw new RuntimeException("Begin cannot be negative");
        }
        PartOfTextOccurrence<Tag> occurrence = tagOccurrences.get(begin);
        if (occurrence != null) {
            return occurrence.getElement();
        } else {
          return null;  
        }
    }
    
    public void addPhraseOccurrence(int begin, int end, Phrase phrase) {
        if (begin < 0) {
            throw new RuntimeException("Begin cannot be negative (for phrase: " + phrase.getContent()+ ")" );
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

    @Override
    public Node storeOnGraph(GraphDatabaseService database) {
        Node sequenceNode = checkIfExist(database, id);
        if (sequenceNode == null) {
            Node newSentenceNode = database.createNode(Sentence);
            newSentenceNode.setProperty(HASH, MD5(sentence));
            newSentenceNode.setProperty(PROPERTY_ID, id);
            if (store) {
                newSentenceNode.setProperty(TEXT, sentence);
            }
            tags.values().stream().forEach((tag) -> {
                Node tagNode = tag.storeOnGraph(database);
                Relationship hasTagRel = newSentenceNode.createRelationshipTo(tagNode, HAS_TAG);
                hasTagRel.setProperty("tf", tag.getMultiplicity());
            });
            sequenceNode = newSentenceNode;
        }
        assignSentimentLabel(sequenceNode);
        return sequenceNode;
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
        return new Sentence(text, true, id);
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
}
