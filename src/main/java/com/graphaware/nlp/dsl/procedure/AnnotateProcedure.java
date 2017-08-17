package com.graphaware.nlp.dsl.procedure;

import com.graphaware.nlp.dsl.AbstractDSL;
import com.graphaware.nlp.dsl.AnnotationRequest;
import com.graphaware.nlp.dsl.result.ProcessorsList;
import com.graphaware.nlp.dsl.result.SingleResult;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class AnnotateProcedure extends AbstractDSL {

    @Procedure(name = "ga.nlp.annotate", mode = Mode.WRITE)
    @Description("Performs the text annotation and store it into the graph")
    public Stream<SingleResult> annotate(@Name("annotationRequest") Map<String, Object> annotationRequest) {

        AnnotationRequest request = mapper.convertValue(annotationRequest, AnnotationRequest.class);
        Object result = getNLPManager().annotateTextAndPersist(request);

        return Stream.of(new SingleResult(result));
    }
    
    @Procedure(name = "ga.nlp.getProcessors", mode = Mode.READ)
    @Description("Performs the text annotation and store it into the graph")
    public Stream<ProcessorsList> getProcessors() {
        Set<ProcessorsList> result = getNLPManager().getProcessors();
        return result.stream();
    }

}
