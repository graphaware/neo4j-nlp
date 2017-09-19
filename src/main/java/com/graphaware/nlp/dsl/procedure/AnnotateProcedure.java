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
import com.graphaware.nlp.dsl.request.AnnotationRequest;
import com.graphaware.nlp.dsl.request.FilterRequest;
import com.graphaware.nlp.dsl.result.NodeResult;
import com.graphaware.nlp.dsl.result.SingleResult;
import org.neo4j.graphdb.Node;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

public class AnnotateProcedure extends AbstractDSL {

    @Procedure(name = "ga.nlp.annotate", mode = Mode.WRITE)
    @Description("Performs the text annotation and store it into the graph")
    public Stream<NodeResult> annotate(@Name("annotationRequest") Map<String, Object> annotationRequest) {
        try {
            AnnotationRequest request = AnnotationRequest.fromMap(annotationRequest);
            Node result = getNLPManager().annotateTextAndPersist(request);
            return Stream.of(new NodeResult(result));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    @Procedure(name = "ga.nlp.filter", mode = Mode.WRITE)
    @Description("Boolean filter for text accordingly to complex filter definition")
    public Stream<SingleResult> filter(@Name("filterRequest") Map<String, Object> filterRequest) {
        FilterRequest request = FilterRequest.fromMap(filterRequest);
        Object result = getNLPManager().filter(request);
        return Stream.of(new SingleResult(result));
    }
}
