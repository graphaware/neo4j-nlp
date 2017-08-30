package com.graphaware.nlp.dsl.procedure;

import com.graphaware.nlp.dsl.AbstractDSL;
import com.graphaware.nlp.dsl.result.SingleResult;
import org.neo4j.graphdb.Node;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.stream.Stream;

public class SentimentProcedure extends AbstractDSL {

    @Procedure(name = "ga.nlp.sentiment", mode = Mode.WRITE)
    @Description("Apply sentiment extraction on the given annotated text")
    public Stream<SingleResult> applySentiment(@Name("annotatedText") Node annotatedText, @Name(value = "textProcessor", defaultValue = "") String textProcessor) {
        getNLPManager().applySentiment(annotatedText, textProcessor);

        return Stream.of(SingleResult.success());
    }

}
