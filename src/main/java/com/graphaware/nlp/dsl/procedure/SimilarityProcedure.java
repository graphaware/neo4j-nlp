package com.graphaware.nlp.dsl.procedure;

import com.graphaware.nlp.dsl.AbstractDSL;
import com.graphaware.nlp.dsl.result.SingleResult;
import java.util.List;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.stream.Stream;
import org.neo4j.graphdb.Node;

public class SimilarityProcedure extends AbstractDSL {

    @Procedure(name = "ga.nlp.ml.similarity.cosine", mode = Mode.WRITE)
    @Description("Compute similarity between Annotated Text")
    public Stream<SingleResult> applySentiment(@Name("input") List<Node> input, 
            @Name("depth") Long depth,
            @Name("query") String query,
            @Name("relationshipType") String relationshipType) {
        int processed = getNLPManager().getSimilarityProcess().compute(input, query, relationshipType, depth);
        return Stream.of(new SingleResult(processed));
    }

}
