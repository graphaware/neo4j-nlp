package com.graphaware.nlp.dsl.result;

import org.neo4j.graphdb.Node;

public class AnnotationResult {

    public Node result;

    public AnnotationResult(Node result) {
        this.result = result;
    }
}
