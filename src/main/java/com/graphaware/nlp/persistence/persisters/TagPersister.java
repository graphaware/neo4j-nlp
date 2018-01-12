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
package com.graphaware.nlp.persistence.persisters;

import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.persistence.PersistenceRegistry;
import com.graphaware.nlp.persistence.constants.Labels;
import com.graphaware.nlp.persistence.constants.Properties;
import com.graphaware.nlp.persistence.constants.Relationships;
import com.graphaware.nlp.util.TagUtils;
import com.graphaware.nlp.util.TypeConverter;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import java.util.*;

public class TagPersister extends AbstractPersister implements Persister<Tag> {

    public TagPersister(GraphDatabaseService database, PersistenceRegistry registry) {
        super(database, registry);
    }

    @Override
    public Node persist(Tag object, String id, String txId) {
        throw new UnsupportedOperationException("This cannot implemented for this persister");
    }

    @Override
    public Tag fromNode(Node node) {
        checkNodeIsATag(node);
        return new Tag(String.valueOf(node.getProperty(configuration().getPropertyKeyFor(Properties.CONTENT_VALUE))),
                String.valueOf(node.getProperty(configuration().getPropertyKeyFor(Properties.LANGUAGE))));
    }

    @Override
    public boolean exists(String id) {
        return false;
    }

    @Override
    public Node getOrCreate(Tag tag, String id, String txId) {
        Node node = getIfExist(
                configuration().getLabelFor(configuration().getLabelFor(Labels.Tag)),
                configuration().getPropertyKeyFor(configuration().getPropertyKeyFor(Properties.PROPERTY_ID)),
                tag.getId());

        if (null == node) {
            node = database.createNode(configuration().getLabelFor(Labels.Tag));
        }

        if (shouldBeUpdated(tag, node)) {
            assignNamedEntityOnTag(node, tag);
            assignPartOfSpeechOnTag(node, tag);
            storeExtraProperties(tag, node);
        }

        if (!checkSameTransaction(node, txId)) {
            update(node, tag, tag.getId());
            assignNamedEntityOnTag(node, tag);
            assignPartOfSpeechOnTag(node, tag);
            setLastTransaction(node, txId);
            storeTagParent(node, tag, txId);
        }
        return node;
    }

    private boolean shouldBeUpdated(Tag tag, Node tagNode) {
        if (tagNode.hasProperty(configuration().getPropertyKeyFor(Properties.PART_OF_SPEECH))) {
            String[] pos = (String[]) tagNode.getProperty(configuration().getPropertyKeyFor(Properties.PART_OF_SPEECH));
            if (tag.getPosAsList().size() != pos.length) {
                return true;
            }
            List<String> original = Arrays.asList(pos);

            if (tag.getPosAsList().stream().anyMatch((s) -> (!original.contains(s)))) {
                return true;
            }
        }

        if (tagNode.hasProperty(configuration().getPropertyKeyFor(Properties.NAMED_ENTITY))) {
            String[] pos = (String[]) tagNode.getProperty(configuration().getPropertyKeyFor(Properties.NAMED_ENTITY));
            if (tag.getNeAsList().size() != pos.length) {
                return true;
            }
            List<String> original = Arrays.asList(pos);

            for (String s : tag.getNeAsList()) {
                if (!original.contains(s)) {
                    return true;
                }
            }
        }

        for (String k : tag.getExtraProperties().keySet()) {
            if (!tagNode.hasProperty(k)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void update(Node node, Tag tag, String id) {
        node.setProperty(configuration().getPropertyKeyFor(Properties.PROPERTY_ID), tag.getId());
        node.setProperty(configuration().getPropertyKeyFor(Properties.LANGUAGE), tag.getLanguage());
        node.setProperty(configuration().getPropertyKeyFor(Properties.CONTENT_VALUE), tag.getLemma());
    }

    private void assignNamedEntityOnTag(Node tagNode, Tag tag) {
        List<String> allNEs = new ArrayList<>();
        if (tagNode.hasProperty(configuration().getPropertyKeyFor(Properties.NAMED_ENTITY))) {
            String[] nes = (String[]) tagNode.getProperty(configuration().getPropertyKeyFor(Properties.NAMED_ENTITY));
            allNEs.addAll(Arrays.asList(nes));
        }

        tag.getNeAsList()
                .stream()
                .filter(n -> !allNEs.contains(n))
                .forEach(allNEs::add);

        tagNode.setProperty(configuration().getPropertyKeyFor(Properties.NAMED_ENTITY), TypeConverter.convertStringListToArray(allNEs));
        allNEs.forEach(ner -> {
            String labelName = configuration().getPropertyKeyFor(Properties.NAMED_ENTITY_PREFIX) + TagUtils.getNamedEntityValue(ner);
            tagNode.addLabel(Label.label(labelName));
        });
    }

    private void assignPartOfSpeechOnTag(Node tagNode, Tag tag) {
        List<String> allPos = new ArrayList<>();
        if (tagNode.hasProperty(configuration().getPropertyKeyFor(Properties.PART_OF_SPEECH))) {
            String[] posV = (String[]) tagNode.getProperty(configuration().getPropertyKeyFor(Properties.PART_OF_SPEECH));
            allPos.addAll(Arrays.asList(posV));
        }
        tag.getPosAsList()
                .stream()
                .filter(t -> !allPos.contains(t))
                .forEach(allPos::add);

        tagNode.setProperty(configuration().getPropertyKeyFor(Properties.PART_OF_SPEECH), TypeConverter.convertStringListToArray(allPos));
    }

    private void storeExtraProperties(Tag tag, Node tagNode) {
        for (String k : tag.getExtraProperties().keySet()) {
            tagNode.setProperty(k, tag.getExtraProperties().get(k));
        }
    }

    private void storeTagParent(Node tagNode, Tag tag, String txId) {
        if (tag.getParents() != null) {
            tag.getParents().stream().forEach((tagRelationship) -> {
                Tag parent = tagRelationship.getParent();
                Node parentTagNode = getOrCreate(parent, parent.getId(), txId);
                long sourceId = tagNode.getId();
                long targetId = parentTagNode.getId();
                //@todo mode type and weight to config constants
                String query = String.format("MATCH (source:`%s`), (target:`%s`) " +
                        "WHERE id(source) = {source} AND id(target) = {target} " +
                        "MERGE (source)-[r:`%s` {%s: {type} }]->(target) " +
                        "ON CREATE SET r.%s = {weight}, r.source = {sourceId} ",
                        configuration().getLabelFor(Labels.Tag),
                        configuration().getLabelFor(Labels.Tag),
                        Relationships.IS_RELATED_TO,
                        "type",
                        "weight");
                Map<String, Object> parameters = new HashMap<>();
                parameters.put("source", sourceId);
                parameters.put("target", targetId);
                parameters.put("type", tagRelationship.getRelation());
                parameters.put("weight", tagRelationship.getWeight());
                parameters.put("sourceId", tagRelationship.getSource());
                getDatabase().execute(query, parameters);
            });
        }
    }

    private void checkNodeIsATag(Node tagNode) {
        Map<String, Object> allProperties = tagNode.getAllProperties();
        assert (tagNode.hasLabel(configuration().getLabelFor(Labels.Tag)));
        assert (allProperties.containsKey(configuration().getPropertyKeyFor(Properties.PROPERTY_ID)));
        assert (allProperties.containsKey(configuration().getPropertyKeyFor(Properties.CONTENT_VALUE)));
        assert (allProperties.containsKey(configuration().getPropertyKeyFor(Properties.LANGUAGE)));
    }
    
    private boolean checkSameTransaction(Node tagNode, String txId) {
        String nodeTxId = (String) tagNode.getProperty(configuration().getPropertyKeyFor(Properties.LAST_TX_ID), null);
        return nodeTxId != null ? nodeTxId.equalsIgnoreCase(txId) : false;
    }

    private void setLastTransaction(Node node, String txId) {
        node.setProperty(configuration().getPropertyKeyFor(Properties.LAST_TX_ID), txId);
    }

    @Override
    public Node persist(Tag object) {
        throw new UnsupportedOperationException("This cannot implemented for this persister");
    }
}
