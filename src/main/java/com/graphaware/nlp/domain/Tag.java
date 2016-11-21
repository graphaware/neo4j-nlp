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

import static com.graphaware.nlp.domain.Labels.Tag;
import static com.graphaware.nlp.domain.Properties.CONTENT_VALUE;
import static com.graphaware.nlp.domain.Properties.LANGUAGE;
import static com.graphaware.nlp.domain.Properties.PROPERTY_ID;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

public class Tag implements Persistable, Serializable {

    private static final long serialVersionUID = -1L;

    private int multiplicity = 1;
    private String lemma;
    private String pos;
    private String ne;
    private Collection<TagParentRelation> parents;
    private String language;

    public Tag(String lemma, String language) {
        this.lemma = lemma;
        this.language = language;
    }

    public String getLemma() {
        return lemma;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }

    public void setNe(String ne) {
        this.ne = ne;
    }

    public int getMultiplicity() {
        return multiplicity;
    }

    public void incMultiplicity() {
        multiplicity++;
    }

    public void setMultiplicity(int multiplicity) {
        this.multiplicity = multiplicity;
    }

    public String getPos() {
        return pos;
    }

    public String getNe() {
        return ne;
    }

    public String getId() {
        return lemma + "_" + language;
    }

    public void addParent(String rel, Tag storedTag, float weight) {
        addParent(new TagParentRelation(storedTag, rel, weight));
    }

    public void addParent(String rel, Tag storedTag) {
        addParent(new TagParentRelation(storedTag, rel));
    }

    public void addParent(TagParentRelation parentRelationship) {
        if (parents == null) {
            parents = new HashSet<>();
        }
        parents.add(parentRelationship);
    }

    @Override
    public Node storeOnGraph(GraphDatabaseService database, boolean force) {
        Node tagNode = getOrCreate(database, force);
        if (parents != null) {
            parents.stream().forEach((tagRelationship) -> {
                Node parentTagNode = tagRelationship.getParent().storeOnGraph(database, force);
                Map<String, Object> params = new HashMap<>();
                params.put("type", tagRelationship.getRelation());
                params.put("weight", tagRelationship.getWeight());
                params.put("sourceId", tagNode.getId());
                params.put("destId", parentTagNode.getId());
                database.execute("MATCH (source:Tag), (destination:Tag)\n"
                        + "WHERE id(source) = {sourceId} and id(destination) = {destId}\n"
                        + "MERGE (source)-[:IS_RELATED_TO {type: {type}, weight: {weight}}]->(destination)", params);
            });
        }
        return tagNode;
    }

    public Node getOrCreate(GraphDatabaseService database, boolean force) {
        Node tagNode = database.findNode(Tag, PROPERTY_ID, getId());
        if (tagNode != null && !force) {
            return tagNode;
        }
        if (tagNode == null) {
            tagNode = database.createNode(Tag);
        }
        tagNode.setProperty(PROPERTY_ID, getId());
        tagNode.setProperty(CONTENT_VALUE, lemma);
        tagNode.setProperty(LANGUAGE, language);
        if (ne != null) {
            tagNode.setProperty("ne", ne);
        }
        if (pos != null) {
            tagNode.setProperty("pos", pos);
        }
        return tagNode;
    }

    public static Tag createTag(Node tagNode) {
        checkNodeIsATag(tagNode);
        Tag tag = new Tag(String.valueOf(tagNode.getProperty(CONTENT_VALUE)),
                String.valueOf(tagNode.getProperty(LANGUAGE)));
        return tag;
    }

    private static void checkNodeIsATag(Node tagNode) {
        Map<String, Object> allProperties = tagNode.getAllProperties();
        assert (tagNode.hasLabel(Tag));
        assert (allProperties.containsKey(PROPERTY_ID));
        assert (allProperties.containsKey(CONTENT_VALUE));
        assert (allProperties.containsKey(LANGUAGE));
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeObject(lemma);
        s.writeObject(language);
        s.writeObject(pos);
        s.writeObject(ne);
        s.writeInt(multiplicity);
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        this.lemma = (String) s.readObject();
        this.language = (String) s.readObject();
        this.pos = (String) s.readObject();
        this.ne = (String) s.readObject();
        this.multiplicity = s.readInt();
    }

}
