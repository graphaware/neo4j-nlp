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
import static com.graphaware.nlp.domain.Relationships.HAS_TAG;
import static com.graphaware.nlp.util.HashFunctions.MD5;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public class Sentence implements Persistable {
    private final Map<String, Tag> tags;
    private final String sentence;
    private int sentiment;
    
    public Sentence(String sentence) {
        this.tags = new HashMap<>();
        this.sentence = sentence;
    }

    public Collection<Tag> getTags() {
        return tags.values();
    }
    
    public void addTag(Tag tag) {
        if (tags.containsKey(tag.getLemma()))
            tags.get(tag.getLemma()).incMultiplicity();
        else 
            tags.put(tag.getLemma(), tag);
    }

    public int getSentiment() {
        return sentiment;
    }

    public void setSentiment(int sentiment) {
        this.sentiment = sentiment;
    }
    
    @Override
    public Node storeOnGraph(GraphDatabaseService database) {
        Node sentenceNode = database.createNode(Sentence);
        sentenceNode.setProperty("hash", MD5(sentence));
        assignSentimentLabel(sentenceNode);
        tags.values().stream().forEach((tag) -> {
            Node tagNode = tag.storeOnGraph(database);
            Relationship hasTagRel = sentenceNode.createRelationshipTo(tagNode, HAS_TAG);
            hasTagRel.setProperty("tf", tag.getMultiplicity());
        });
        
        return sentenceNode;
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
        }
    }
}
