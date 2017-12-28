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
package com.graphaware.nlp.ml.similarity;

import com.graphaware.nlp.annotation.NLPModuleExtension;
import com.graphaware.nlp.dsl.request.SimilarityRequest;
import com.graphaware.nlp.extension.AbstractExtension;
import com.graphaware.nlp.extension.NLPExtension;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@NLPModuleExtension(name = "SimilarityProcessor")
public class SimilarityProcessor extends AbstractExtension implements NLPExtension {

    private static final Logger LOG = LoggerFactory.getLogger(SimilarityProcessor.class);

    private FeatureBasedProcessLogic featureBusinessLogic;
    private VectorProcessLogic vectorBusinessLogic;

    //private static final Boolean PARAMETER_NAME_ADJ_ADV = "adjectives_adverbs";
    @Override
    public void postLoaded() {
        featureBusinessLogic = new FeatureBasedProcessLogic(getDatabase());
        featureBusinessLogic.start();
        vectorBusinessLogic = new VectorProcessLogic(getDatabase());
        vectorBusinessLogic.start();
    }

    public int compute(SimilarityRequest request) {

        int processed;
        if (request.getPropertyName() != null) {
            processed = computeUsingProperty(request.getInput(), request.getPropertyName(), request.getRelationshipType(), request.getkSize());
        } else {
            Long depth = request.getDepth();
            if (depth != null && depth > 0) {
                processed = computeAllCn5(request.getInput(), depth.intValue());
            } else {
                processed = computeAll(request.getInput(), request.getQuery(), request.getRelationshipType());
            }
        }

        return processed;
    }

    public int computeAll(List<Node> input, String query, String relationshipType) {
        int processed = 0;
        List<Long> firstNodeIds = getNodesFromInput(input);
        if (query != null && relationshipType != null) {
            processed = featureBusinessLogic.computeFeatureSimilarityForNodes(firstNodeIds, query, relationshipType, 0);
        } else {
            processed = featureBusinessLogic.computeFeatureSimilarityForNodes(firstNodeIds);
        }
        return processed;

    }

    private List<Long> getNodesFromInput(List<Node> input) {
        return input.stream().map(x -> x.getId()).collect(Collectors.toList());
    }

    protected List<Long> getNodesFromInput(Object[] input) {
        List<Long> firstNodeIds = new ArrayList<>();
        if (input[0] == null) {
            return null;
        } else if (input[0] instanceof Node) {
            firstNodeIds.add(((Node) input[0]).getId());
            return firstNodeIds;
        } else if (input[0] instanceof Map) {
            Map<String, Object> nodesMap = (Map) input[0];
            nodesMap.values().stream().filter((element) -> (element instanceof Node)).forEach((element) -> {
                firstNodeIds.add(((Node) element).getId());
            });
            if (!firstNodeIds.isEmpty()) {
                return firstNodeIds;
            } else {
                return null;
            }
        } else {
            throw new RuntimeException("Invalid input parameters " + input[0]);
        }
    }

    public int computeAllCn5(List<Node> input, int depth) {
        int processed = 0;
        List<Long> firstNodeIds = getNodesFromInput(input);
        processed = featureBusinessLogic.computeFeatureSimilarityForNodes(firstNodeIds, depth);
        return processed;
    }

    private int computeUsingProperty(List<Node> input, String propertyName, String relationshipType, int kSize) {
        int processed = 0;
        processed = vectorBusinessLogic.computeFeatureSimilarityForNodes(input, propertyName, relationshipType, kSize);
        return processed;
    }
}
