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
import com.graphaware.nlp.dsl.request.SimilarityRequest;
import com.graphaware.nlp.dsl.result.SingleResult;
import com.graphaware.nlp.ml.similarity.SimilarityProcessor;
import com.graphaware.nlp.ml.similarity.VectorProcessLogic;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimilarityProcedure extends AbstractDSL {

    private static final Logger LOG = LoggerFactory.getLogger(VectorProcessLogic.class);

//    @Procedure(name = "ga.nlp.ml.similarity.cosine", mode = Mode.WRITE)
//    @Description("Compute similarity between Annotated Text")
//    public Stream<SingleResult> similarityOld(@Name("input") List<Node> input, 
//            @Name("depth") Long depth,
//            @Name("query") String query,
//            @Name("relationshipType") String relationshipType) {
//        SimilarityProcessor similarityProcessor = (SimilarityProcessor) getNLPManager().getExtension(SimilarityProcessor.class);
//        int processed = similarityProcessor.compute(input, query, relationshipType, depth);
//        return Stream.of(new SingleResult(processed));
//    }
    @Procedure(name = "ga.nlp.ml.similarity.cosine", mode = Mode.WRITE)
    @Description("Compute similarity between Annotated Text")
    public Stream<SingleResult> similarity(@Name("similarityRequest") Map<String, Object> similarityRequest) {
        try {
            SimilarityRequest request = SimilarityRequest.fromMap(similarityRequest);
            SimilarityProcessor similarityProcessor = (SimilarityProcessor) getNLPManager().getExtension(SimilarityProcessor.class);
            int processed = similarityProcessor.compute(request);
            return Stream.of(new SingleResult(processed));
        } catch (Exception ex) {
            LOG.error("Error while computing similarity", ex);
            throw new RuntimeException(ex);
        }

    }

}
