package com.graphaware.nlp.dsl.procedure;

import com.graphaware.nlp.dsl.AbstractDSL;
import com.graphaware.nlp.dsl.result.SingleResult;
import com.graphaware.nlp.processor.PipelineSpecification;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

public class AnnotateProcedure extends AbstractDSL {

    @Procedure(name = "ga.nlp.annotate", mode = Mode.WRITE)
    @Description("Performs the text annotation and store it into the graph")
    public Stream<SingleResult> annotate(@Name("text") String text, @Name("id") Object id, @Name("specifications") Map<String, Object> spec, @Name(value = "force", defaultValue = "false") Object force) {
        String annotationId = String.valueOf(id);
        PipelineSpecification pipelineSpecification = PipelineSpecification.fromMap(spec);
        boolean shouldForce = (boolean) force;

        Object result = getNLPManager().annotateTextAndPersist(text, annotationId, pipelineSpecification, shouldForce);

        return Stream.of(new SingleResult(result));
    }

}
