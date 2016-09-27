/*
 * Copyright (c) 2013-2016 GraphAware
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

import com.graphaware.nlp.procedure.NLPProcedure;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.neo4j.collection.RawIterator;
import org.neo4j.graphdb.Node;
import org.neo4j.helpers.collection.Iterators;
import org.neo4j.kernel.api.exceptions.ProcedureException;
import org.neo4j.kernel.api.proc.CallableProcedure;
import org.neo4j.kernel.api.proc.Neo4jTypes;
import org.neo4j.kernel.api.proc.ProcedureSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.neo4j.kernel.api.proc.ProcedureSignature.procedureSignature;

public class SimilarityProcedure extends NLPProcedure {

    private static final Logger LOG = LoggerFactory.getLogger(SimilarityProcedure.class);

    private final FeatureBasedProcessLogic featureBusinessLogic;

    public SimilarityProcedure(FeatureBasedProcessLogic featureBusinessLogic) {
        this.featureBusinessLogic = featureBusinessLogic;
    }

    public CallableProcedure.BasicProcedure computeAll() {
        return new CallableProcedure.BasicProcedure(procedureSignature(getProcedureName("ml", "cosine", "compute"))
                .mode(ProcedureSignature.Mode.READ_WRITE)
                .in(PARAMETER_NAME_INPUT, Neo4jTypes.NTAny)
                .out(PARAMETER_NAME_INPUT_OUTPUT, Neo4jTypes.NTInteger).build()) {

            @Override
            public RawIterator<Object[], ProcedureException> apply(CallableProcedure.Context ctx, Object[] input) throws ProcedureException {
                int processed = 0;
                List<Long> firstNodeIds = getNodesFromInput(input);
                processed = featureBusinessLogic.computeFeatureSimilarityForNodes(firstNodeIds);
                return Iterators.asRawIterator(Collections.<Object[]>singleton(new Integer[]{processed}).iterator());
            }
        };
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
}
