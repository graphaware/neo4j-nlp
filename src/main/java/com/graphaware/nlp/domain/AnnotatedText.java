/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.domain;

import static com.graphaware.nlp.domain.Labels.AnnotatedText;
import static com.graphaware.nlp.domain.Relationships.CONTAINS_SENTENCE;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

/**
 *
 * @author ale
 */
public class AnnotatedText implements Persistable {
    private final List<Sentence> sentences;
    private final String text;

    public AnnotatedText(String text) {
        sentences = new ArrayList<>();
        this.text = text;
    }

    public List<Sentence> getSentences() {
        return sentences;
    }
    
    public void addSentence(Sentence sentence) {
        sentences.add(sentence);
    }

    @Override
    public Node storeOnGraph(GraphDatabaseService database) {
        Node annotatedTextNode = database.createNode(AnnotatedText);
        annotatedTextNode.setProperty("hash", text.hashCode());
        sentences.stream().map((sentence) -> sentence.storeOnGraph(database)).forEach((sentenceNode) -> {
            annotatedTextNode.createRelationshipTo(sentenceNode, CONTAINS_SENTENCE);
        });
        return annotatedTextNode;
    }
}
