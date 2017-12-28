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
package com.graphaware.nlp.dsl.procedure;

import com.graphaware.nlp.dsl.AbstractDSL;
import com.graphaware.nlp.dsl.request.ComputeVectorRequest;
import com.graphaware.nlp.dsl.result.NodeResult;
import java.util.Map;
import java.util.stream.Stream;
import org.neo4j.graphdb.Node;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

public class ComputeVectorProcedure extends AbstractDSL {
    
    @Procedure(name = "ga.nlp.vector.compute", mode = Mode.WRITE)
    @Description("Compute vectors for input node and store vector in the property specified")
    public Stream<NodeResult> annotate(@Name("computeVectorRequest") Map<String, Object> computeVectorRequest) {
        try {
            ComputeVectorRequest request = ComputeVectorRequest.fromMap(computeVectorRequest);
            Node result = getNLPManager().computeVectorAndPersist(request);
            return Stream.of(new NodeResult(result));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
