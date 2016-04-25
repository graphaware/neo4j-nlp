/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.domain;

import static com.graphaware.nlp.domain.Labels.Tag;
import static com.graphaware.nlp.domain.Relationships.IS_RELATED_TO;
import java.util.Collection;
import java.util.TreeSet;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

/**
 *
 * @author ale
 */
public class Tag implements Persistable {

    private int multiplicity;
    private final String lemma;
    private String pos;
    private String ne;
    private Collection<TagParentRelation> parents;

    public Tag(String lemma) {
        this.lemma = lemma;
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

    public String getPos() {
        return pos;
    }

    public String getNe() {
        return ne;
    }

    public void addParent(String rel, Tag storedTag) {
        if (parents == null) {
            parents = new TreeSet<>();
        }
        parents.add(new TagParentRelation(storedTag, rel));
    }

    @Override
    public Node storeOnGraph(GraphDatabaseService database) {
        Node tagNode = getOrCreate(database);
        if (parents != null) {
            parents.stream().forEach((tagRelationship) -> {
                Node parentTagNode = tagRelationship.getParent().storeOnGraph(database);
                Relationship isRelatedRel = tagNode.createRelationshipTo(parentTagNode, IS_RELATED_TO);
                isRelatedRel.setProperty("type", tagRelationship.getRelation());
            });
        }
        return tagNode;
    }

    private Node getOrCreate(GraphDatabaseService database) {
        Node tagNode = database.findNode(Tag, "value", lemma);
        if (tagNode != null) {
            return tagNode;
        }
        tagNode = database.createNode(Tag);
        tagNode.setProperty("value", lemma);
        tagNode.setProperty("ne", ne);
        if (pos != null)
            tagNode.setProperty("pos", pos);
        return tagNode;
    }
}
