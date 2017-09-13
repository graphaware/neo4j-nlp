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
import com.graphaware.nlp.domain.Keyword;
import com.graphaware.nlp.persistence.PersistenceRegistry;
import com.graphaware.nlp.persistence.constants.Labels;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

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
