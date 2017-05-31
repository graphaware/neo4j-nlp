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
import static com.graphaware.nlp.domain.Properties.PHRASE_TYPE;
import com.graphaware.nlp.domain.NLPDefaultValues;
import java.util.Objects;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

public class Phrase implements Persistable {
    private final String content;
    private String type;
    private Phrase reference;
    private Node phraseNode;

    public Phrase(String content) {
        this.content = content.trim();
        this.type = null;
    }

    public Phrase(String content, String type) {
      this(content);
      this.type = type;
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
    public Node storeOnGraph(GraphDatabaseService database, boolean force) {
        phraseNode = getOrCreate(database, force);
        return phraseNode;
    }
    
    public Node getOrCreate(GraphDatabaseService database, boolean force) {
        if (phraseNode != null) {
            return phraseNode;
        }
        phraseNode = database.findNode(Phrase, CONTENT_VALUE, content);
        if (phraseNode != null && !force) {
            return phraseNode;
        }
        if (phraseNode == null)
            phraseNode = database.createNode(Phrase);
        phraseNode.setProperty(CONTENT_VALUE, content);
        if (type!=null)
          phraseNode.setProperty(PHRASE_TYPE, type);
        else
          phraseNode.setProperty(PHRASE_TYPE, NLPDefaultValues.PHRASE_TYPE);
        return phraseNode;
    }
}
