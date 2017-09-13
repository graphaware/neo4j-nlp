/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.persistence.persisters;

import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.domain.Keyword;
import com.graphaware.nlp.persistence.PersistenceRegistry;
import com.graphaware.nlp.persistence.constants.Labels;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

/**
 *
 * @author ale
 */
public class KeywordPersister extends AbstractPersister implements Persister<Keyword> {

    public KeywordPersister(GraphDatabaseService database, DynamicConfiguration dynamicConfiguration, PersistenceRegistry registry) {
        super(database, dynamicConfiguration, registry);
    }

    @Override
    public Node persist(Keyword keyword, String id, String txId) {
        return getOrCreate(keyword, id, txId);
    }

    @Override
    public Keyword fromNode(Node node) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean exists(String id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Node getOrCreate(Keyword keyword, String id, String txId) {
        Node newNode;
        final Label keywordLabel = configuration().getLabelFor(Labels.Keyword);
        Node storedKeyword = getIfExist(keywordLabel, "id", keyword.getKeyword());
        if (storedKeyword != null) {
            newNode = storedKeyword;
        } else {
            newNode = database.createNode(keywordLabel);
            update(newNode, keyword, id);
        }
        return newNode;
    }

    @Override
    public void update(Node node, Keyword keyword, String id) {
        node.setProperty("id", keyword.getKeyword());
        node.setProperty("value", keyword.getRawKeyword());
        node.setProperty("keywordsList", keyword.getListOfWords());
        node.setProperty("numTerms", keyword.getWordsCount());
    }
}
