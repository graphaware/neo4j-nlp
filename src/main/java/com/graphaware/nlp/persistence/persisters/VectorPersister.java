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
import com.graphaware.nlp.domain.VectorContainer;
import com.graphaware.nlp.persistence.PersistenceRegistry;
import com.graphaware.nlp.persistence.constants.Labels;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VectorPersister extends AbstractPersister implements Persister<VectorContainer> {
    
    private static final Logger LOG = LoggerFactory.getLogger(VectorPersister.class);

    public VectorPersister(GraphDatabaseService database, PersistenceRegistry registry) {
        super(database, registry);
    }
    
    @Override
    public Node persist(VectorContainer object, String id, String txId) {
        return getOrCreate(object, id, txId);
    }

    @Override
    public VectorContainer fromNode(Node node) {
        throw new UnsupportedOperationException("This shouldn't be necessary");
    }

    @Override
    public boolean exists(String id) {
        Node node = database.getNodeById(Long.parseLong(id));
        Label label = configuration().getLabelFor(Labels.VectorContainer);
        if (node != null && node.hasLabel(label)) {
            return true;
        } else if (node != null) {
            LOG.warn("The node " + node.getId() + " has no lavel " + label + ". Available labels are: " + node.getLabels());
        }
        return false;
    }

    @Override
    public Node getOrCreate(VectorContainer object, String id, String txId) {
        Node node = database.getNodeById(object.getNodeId());

        if (null == node) {
            throw new RuntimeException("Node should exist to store a vector");
        } 
        
        node.addLabel(configuration().getLabelFor(Labels.VectorContainer));
        node.setProperty(object.getPropertyName(), object.getVector().getArray());
        
        return node;
    }

    @Override
    public void update(Node node, VectorContainer object, String id) {
        getOrCreate(object, null, null);
    }

    @Override
    public Node persist(VectorContainer object) {
        return getOrCreate(object, null, null);
    }
    
}
