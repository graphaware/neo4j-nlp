package com.graphaware.nlp.event;

import com.graphaware.nlp.domain.AnnotatedText;
import org.neo4j.graphdb.Node;

public class TextAnnotationEvent implements Event {

    private final Node annotatedNode;

    private final AnnotatedText annotatedText;

    private final String id;

    public TextAnnotationEvent(Node annotatedNode, AnnotatedText annotatedText, String id) {
        this.annotatedNode = annotatedNode;
        this.annotatedText = annotatedText;
        this.id = id;
    }

    public Node getAnnotatedNode() {
        return annotatedNode;
    }

    public AnnotatedText getAnnotatedText() {
        return annotatedText;
    }

    public String getId() {
        return id;
    }
}
