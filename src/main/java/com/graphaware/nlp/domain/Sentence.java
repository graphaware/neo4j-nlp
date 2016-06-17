/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.domain;

import static com.graphaware.nlp.domain.Labels.*;
import static com.graphaware.nlp.domain.Relationships.HAS_TAG;
import static com.graphaware.nlp.util.HashFunctions.MD5;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

/**
 *
 * @author ale
 */
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
                sentenceNode.addLabel(VeryNegative);
                break;
            case 2:
                sentenceNode.addLabel(VeryNegative);
                break;
            case 3:
                sentenceNode.addLabel(VeryNegative);
                break;
            case 4:
                sentenceNode.addLabel(VeryNegative);
                break;
        }
    }
}
