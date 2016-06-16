/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.domain;

import static com.graphaware.nlp.domain.Labels.AnnotatedText;
import static com.graphaware.nlp.domain.Relationships.CONTAINS_SENTENCE;
import static com.graphaware.nlp.util.HashFunctions.MD5;
import java.util.ArrayList;
import java.util.List;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

/**
 *
 * @author ale
 */
public class AnnotatedText implements Persistable {
    private final Object id;
    private final List<Sentence> sentences;
    private final String text;
    private Node node;

    public AnnotatedText(String text, Object id) {
        sentences = new ArrayList<>();
        this.text = text;
        this.id = id;
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
        annotatedTextNode.setProperty(Properties.PROPERTY_ID, id);
        sentences.stream().map((sentence) -> sentence.storeOnGraph(database)).forEach((sentenceNode) -> {
            annotatedTextNode.createRelationshipTo(sentenceNode, CONTAINS_SENTENCE);
        });
        node = annotatedTextNode;
        return annotatedTextNode;
    }

    public Node getNode() {
        return node;
    }
    
}
