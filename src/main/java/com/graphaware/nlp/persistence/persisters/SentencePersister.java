package com.graphaware.nlp.persistence.persisters;

import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.domain.Sentence;
import com.graphaware.nlp.persistence.PersistenceRegistry;
import com.graphaware.nlp.persistence.constants.Labels;
import com.graphaware.nlp.persistence.constants.Properties;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

public class SentencePersister extends AbstractPersister implements Persister<Sentence> {

    public SentencePersister(GraphDatabaseService database, DynamicConfiguration dynamicConfiguration, PersistenceRegistry registry) {
        super(database, dynamicConfiguration, registry);
    }

    @Override
    public Node persist(Sentence object, String id, boolean force) {
        return null;
    }

    @Override
    public Sentence fromNode(Node node) {
        return null;
    }

    @Override
    public boolean exists(String id) {
        return false;
    }

    @Override
    public Node getOrCreate(Sentence sentence, String id, boolean force) {
        Node node = database.createNode(configuration().getLabelFor(Labels.Sentence));
        update(node, sentence, id);

        return node;
    }

    @Override
    public void update(Node node, Sentence sentence, String id) {
        node.setProperty(configuration().getPropertyKeyFor(Properties.PROPERTY_ID), String.format("%s_%s", id, sentence.getSentenceNumber()));
        node.setProperty(configuration().getPropertyKeyFor(Properties.SENTENCE_NUMBER), sentence.getSentenceNumber());
        node.setProperty(configuration().getPropertyKeyFor(Properties.HASH), sentence.hash());
        node.setProperty(configuration().getPropertyKeyFor(Properties.TEXT), sentence.getSentence());
    }
}
