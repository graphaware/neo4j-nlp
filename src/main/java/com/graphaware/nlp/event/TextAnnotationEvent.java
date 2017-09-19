/*
 * Copyright (c) 2013-2017 GraphAware
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
package com.graphaware.nlp.event;

import com.graphaware.nlp.domain.AnnotatedText;
import org.neo4j.graphdb.Node;

public class TextAnnotationEvent implements Event {

    private final Node annotatedNode;

    private final AnnotatedText annotatedText;

    private final String id;

    private final String txId;

    public TextAnnotationEvent(Node annotatedNode, AnnotatedText annotatedText, String id, String txId) {
        this.annotatedNode = annotatedNode;
        this.annotatedText = annotatedText;
        this.id = id;
        this.txId = txId;
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

    public String getTxId() {
        return txId;
    }
}
