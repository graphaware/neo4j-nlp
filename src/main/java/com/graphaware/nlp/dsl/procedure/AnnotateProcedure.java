package com.graphaware.nlp.dsl.procedure;

import com.graphaware.nlp.dsl.AbstractDSL;
import com.graphaware.nlp.dsl.AnnotationRequest;
import com.graphaware.nlp.dsl.FilterRequest;
import com.graphaware.nlp.dsl.result.NodeResult;
import com.graphaware.nlp.dsl.result.SingleResult;
import org.neo4j.graphdb.Node;
import org.neo4j.procedure.*;

import java.util.Map;
import java.util.stream.Stream;

public class AnnotateProcedure extends AbstractDSL {

    @Procedure(name = "ga.nlp.annotate", mode = Mode.WRITE)
    @Description("Performs the text annotation and store it into the graph")
    public Stream<NodeResult> annotate(@Name("annotationRequest") Map<String, Object> annotationRequest) {
        try {
            AnnotationRequest request = mapper.convertValue(annotationRequest, AnnotationRequest.class);
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
        FilterRequest request = mapper.convertValue(filterRequest, FilterRequest.class);
        Object result = getNLPManager().filter(request);
        return Stream.of(new SingleResult(result));
    }
}
