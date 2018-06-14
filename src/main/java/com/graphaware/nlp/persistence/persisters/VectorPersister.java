/*
 * Copyright (c) 2013-2018 GraphAware
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

import com.graphaware.nlp.domain.VectorContainer;
import com.graphaware.nlp.persistence.PersistenceRegistry;
import com.graphaware.nlp.persistence.constants.Labels;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.logging.Log;
import com.graphaware.common.log.LoggerFactory;
import com.graphaware.nlp.vector.VectorFactory;
import com.graphaware.nlp.vector.VectorHandler;

import java.util.Optional;

public class VectorPersister extends AbstractPersister implements Persister<VectorContainer> {
    
    private static final Log LOG = LoggerFactory.getLogger(VectorPersister.class);

    public VectorPersister(GraphDatabaseService database, PersistenceRegistry registry) {
        super(database, registry);
    }
    
    @Override
    public Node persist(VectorContainer object, String label, String txId) {
        return getOrCreate(object, label, txId);
    }

    @Override
    public VectorContainer fromNode(Node node, Object... properties) {
        String basePropertyname = (String)properties[0];
        String type = (String)node.getProperty(getTypePropertyName(basePropertyname));
        float[] vector = (float[]) node.getProperty(getArrayPropertyName(basePropertyname));
        VectorHandler createVector = VectorFactory.createVector(type, vector);
        return new VectorContainer(node.getId(), basePropertyname, createVector);
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
    public Node getOrCreate(VectorContainer object, String label, String txId) {
        Node node = database.getNodeById(object.getNodeId());

        if (null == node) {
            throw new RuntimeException("Node should exist to store a vector");
        }
        storeVector(node, object.getPropertyName(), object.getVectorHandler().getType(), object.getVectorHandler().getArray(), Optional.ofNullable(label));
        
        return node;
    }

    public void storeVector(Node node, String propertyName, String type, float[] vector, Optional<String> label) {
        Label vectorContainerLabel;
        if (label.isPresent()) {
            vectorContainerLabel = Label.label(label.get());
        } else {
            vectorContainerLabel = configuration().getLabelFor(Labels.VectorContainer);
        }
        node.addLabel(vectorContainerLabel);
        node.setProperty(getTypePropertyName(propertyName), type);
        node.setProperty(getArrayPropertyName(propertyName), vector);
    }

    private static String getTypePropertyName(String basePropertyname) {
        return basePropertyname + "_type";
    }
    
    private static String getArrayPropertyName(String basePropertyname) {
        return basePropertyname + "_array";
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
