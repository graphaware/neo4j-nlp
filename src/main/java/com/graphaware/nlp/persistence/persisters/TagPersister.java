package com.graphaware.nlp.persistence.persisters;

import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.persistence.PersistenceRegistry;
import com.graphaware.nlp.persistence.constants.Labels;
import com.graphaware.nlp.persistence.constants.Properties;
import com.graphaware.nlp.persistence.constants.Relationships;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.Map;

public class TagPersister extends AbstractPersister implements Persister<Tag> {

    public TagPersister(GraphDatabaseService database, DynamicConfiguration dynamicConfiguration, PersistenceRegistry registry) {
        super(database, dynamicConfiguration, registry);
    }

    @Override
    public Node persist(Tag object, String id, boolean force) {
        return null;
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
    public Node getOrCreate(Tag tag, String id, boolean force) {
        Node node = getIfExist(
                configuration().getLabelFor(configuration().getLabelFor(Labels.Tag)),
                configuration().getPropertyKeyFor(configuration().getPropertyKeyFor(Properties.PROPERTY_ID)),
                tag.getId());

        if (null == node) {
            node = database.createNode(configuration().getLabelFor(Labels.Tag));
            update(node, tag, null);
            storeTagParent(node, tag);
            return node;
        }

        if (force) {
            update(node, tag, null);
            storeTagParent(node, tag);
        }

        return node;
    }

    @Override
    public void update(Node node, Tag tag, String id) {
        node.setProperty(configuration().getPropertyKeyFor(Properties.PROPERTY_ID), tag.getId());
        node.setProperty(configuration().getPropertyKeyFor(Properties.LANGUAGE), tag.getLanguage());
        node.setProperty(configuration().getPropertyKeyFor(Properties.CONTENT_VALUE), tag.getLemma());
    }

    private void storeTagParent(Node tagNode, Tag tag) {
        if (tag.getParents() != null) {
            tag.getParents().stream().forEach((tagRelationship) -> {
                Node parentTagNode = getOrCreate(tag, null,true);
                Relationship parentRelationship = tagNode.createRelationshipTo(parentTagNode,
                        configuration().getRelationshipFor(Relationships.IS_RELATED_TO));
                //@todo mode type and weight to config constants
                parentRelationship.setProperty("type", tagRelationship.getRelation());
                parentRelationship.setProperty("weight", tagRelationship.getWeight());
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
}
