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

import static com.graphaware.nlp.domain.Labels.Phrase;
import static com.graphaware.nlp.domain.Properties.CONTENT_VALUE;
import java.util.Objects;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

public class Phrase implements Persistable {
    private final String content;
    private Phrase reference;
    private Node phraseNode;

    public Phrase(String content) {
        this.content = content.trim();
    }    

    public String getContent() {
        return content;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Phrase))
            return false;
        return this.content.equalsIgnoreCase(((Phrase)o).content);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + Objects.hashCode(this.content);
        return hash;
    }

    public Phrase getReference() {
        return reference;
    }

    public void setReference(Phrase reference) {
        this.reference = reference;
    }

    @Override
    public Node storeOnGraph(GraphDatabaseService database) {
        phraseNode = getOrCreate(database);
        return phraseNode;
    }
    
    public Node getOrCreate(GraphDatabaseService database) {
        if (phraseNode != null) {
            return phraseNode;
        }
        phraseNode = database.findNode(Phrase, CONTENT_VALUE, content);
        if (phraseNode != null) {
            return phraseNode;
        }
        phraseNode = database.createNode(Phrase);
        phraseNode.setProperty(CONTENT_VALUE, content);
        return phraseNode;
    }
}
