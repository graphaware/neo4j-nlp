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
import org.neo4j.graphdb.Relationship;

import java.util.List;
import java.util.Map;

public class TagPersister extends AbstractPersister implements Persister<Tag> {

    public TagPersister(GraphDatabaseService database, DynamicConfiguration dynamicConfiguration, PersistenceRegistry registry) {
        super(database, dynamicConfiguration, registry);
    }

    @Override
    public Node persist(Tag object, String id, String txId) {
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
    public Node getOrCreate(Tag tag, String id, String txId) {
        Node node = getIfExist(
                configuration().getLabelFor(configuration().getLabelFor(Labels.Tag)),
                configuration().getPropertyKeyFor(configuration().getPropertyKeyFor(Properties.PROPERTY_ID)),
                tag.getId());

        if (null == node) {
            node = database.createNode(configuration().getLabelFor(Labels.Tag));
        }

        if (!checkSameTransaction(node, txId)) {
            setLastTransaction(node, txId);
            update(node, tag, tag.getId());
            storeTagParent(node, tag, txId);
            assignNamedEntityOnTag(node, tag);
            assignPartOfSpeechOnTag(node, tag);
        }
        return node;
    }

    @Override
    public void update(Node node, Tag tag, String id) {
        node.setProperty(configuration().getPropertyKeyFor(Properties.PROPERTY_ID), tag.getId());
        node.setProperty(configuration().getPropertyKeyFor(Properties.LANGUAGE), tag.getLanguage());
        node.setProperty(configuration().getPropertyKeyFor(Properties.CONTENT_VALUE), tag.getLemma());
    }

    private void assignNamedEntityOnTag(Node tagNode, Tag tag) {
        List<String> namedEntities = tag.getNeAsList();
        tagNode.setProperty(configuration().getPropertyKeyFor(Properties.NAMED_ENTITY), TypeConverter.convertStringListToArray(namedEntities));
        namedEntities.forEach(ner -> {
            String labelName = configuration().getPropertyKeyFor(Properties.NAMED_ENTITY_PREFIX) + TagUtils.getNamedEntityValue(ner);
            tagNode.addLabel(Label.label(labelName));
        });
    }

    private void assignPartOfSpeechOnTag(Node tagNode, Tag tag) {
        List<String> parts = tag.getPosAsList();
        tagNode.setProperty(configuration().getPropertyKeyFor(Properties.PART_OF_SPEECH), TypeConverter.convertStringListToArray(parts));
    }

    private void storeTagParent(Node tagNode, Tag tag, String txId) {
        if (tag.getParents() != null) {
            tag.getParents().stream().forEach((tagRelationship) -> {
                Tag parent = tagRelationship.getParent();
                Node parentTagNode = getOrCreate(parent, parent.getId(), txId);
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
    
    private boolean checkSameTransaction(Node tagNode, String txId) {
        String nodeTxId = (String)tagNode.getProperty(configuration().getPropertyKeyFor(Properties.LAST_TX_ID), null);
        return nodeTxId != null ? nodeTxId.equalsIgnoreCase(txId) : false;
    }

    private void setLastTransaction(Node node, String txId) {
        node.setProperty(configuration().getPropertyKeyFor(Properties.LAST_TX_ID), txId);
    }
}
