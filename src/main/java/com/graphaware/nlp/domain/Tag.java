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
import java.util.Map;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

public class Tag implements Persistable, Serializable {

    private static final long serialVersionUID = -1L;

    private int multiplicity = 1;
    private String lemma;
    private List<String> posL;
    private List<String> neL;
    private final Collection<TagParentRelation> parents;
    private String language;
    
    private Map<String, Object> properties;

    public Tag(String lemma, String language) {
        this.lemma = lemma;
        this.language = language;
        this.parents = new CopyOnWriteArraySet<>();
    }

    public String getLemma() {
        if ((neL != null && neL.contains("O")) || neL == null) {
            return lemma.toLowerCase();
        } else {
            return lemma;
        }
            
            
    }

    public void setPos(List<String> pos) {
        this.posL = pos;
    }

    public void setNe(List<String> ne) {
        this.neL = ne;
    }

    public int getMultiplicity() {
        return multiplicity;
    }

    public synchronized void incMultiplicity() {
        multiplicity++;
    }

    public void setMultiplicity(int multiplicity) {
        this.multiplicity = multiplicity;
    }

    public List<String> getPosAsList() {
        return posL;
    }

    public List<String> getNeAsList() {
        return neL;
    }

    public String getId() {
        return getLemma() + "_" + language;
    }

    public void addParent(String rel, Tag storedTag, float weight) {
        addParent(new TagParentRelation(storedTag, rel, weight));
    }

    public void addParent(String rel, Tag storedTag) {
        addParent(new TagParentRelation(storedTag, rel));
    }

    public void addParent(TagParentRelation parentRelationship) {
        parents.add(parentRelationship);
    }

    public void addProperties(String key, Object value) {
        if (properties == null) {
            this.properties = new HashMap<>();
        }
        properties.put(key, value);
    }

    @Override
    public Node storeOnGraph(GraphDatabaseService database, boolean force) {
        Node tagNode = getOrCreate(database, force);
        assignNERLabel(tagNode);
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
        tagNode.setProperty(CONTENT_VALUE, getLemma());
        tagNode.setProperty(LANGUAGE, language);

        if (neL != null) {
            tagNode.setProperty("ne", neL.toArray(new String[neL.size()]));
        }
        if (posL != null) {
            tagNode.setProperty("pos", posL.toArray(new String[posL.size()]));
        }
        
        if (properties != null) {
            for (Map.Entry<String, Object> property : properties.entrySet()) {
                tagNode.setProperty(property.getKey(), property.getValue());
            }
        }

        return tagNode;
    }

    public static Tag createTag(Node tagNode) {
        checkNodeIsATag(tagNode);
        Tag tag = new Tag(String.valueOf(tagNode.getProperty(CONTENT_VALUE)),
                String.valueOf(tagNode.getProperty(LANGUAGE)));
        return tag;
    }

    private void assignNERLabel(Node node) {
        if (neL != null) {
            neL.stream().filter((ent) -> !(ent == null)).forEach((ent) -> {
                node.addLabel(new NERLabel(ent));
            });
        }
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
        s.writeObject(getLemma());
        s.writeObject(language);
        if (posL != null) {
            s.writeObject(posL);
        }
        if (neL != null) {
            s.writeObject(neL);
        }
        s.writeInt(multiplicity);
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        this.lemma = (String) s.readObject();
        this.language = (String) s.readObject();
        this.posL = (List<String>) s.readObject();
        this.neL = (List<String>) s.readObject();
        this.multiplicity = s.readInt();
    }

}
