/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.domain;

import static com.graphaware.nlp.domain.Labels.Sentence;
import static com.graphaware.nlp.domain.Relationships.HAS_TAG;
import static com.graphaware.nlp.util.HashFunctions.MD5;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.neo4j.graphdb.DynamicLabel;
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

    @Override
    public Node storeOnGraph(GraphDatabaseService database) {
        Node sentenceNode = database.createNode(Sentence);
        sentenceNode.setProperty("hash", MD5(sentence));
        tags.values().stream().forEach((tag) -> {
            Node tagNode = tag.storeOnGraph(database);
            Relationship hasTagRel = sentenceNode.createRelationshipTo(tagNode, HAS_TAG);
            hasTagRel.setProperty("tf", tag.getMultiplicity());
        });
        
        return sentenceNode;
    }
}
