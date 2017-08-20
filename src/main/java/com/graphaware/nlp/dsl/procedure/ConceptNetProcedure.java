/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.dsl.procedure;

import com.graphaware.nlp.dsl.AbstractDSL;
import com.graphaware.nlp.dsl.ConceptRequest;
import com.graphaware.nlp.dsl.result.NodeResult;
import java.util.Map;
import java.util.stream.Stream;
import org.neo4j.graphdb.Node;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

/**
 *
 * @author ale
 */
public class ConceptNetProcedure extends AbstractDSL {
   
    @Procedure(name = "ga.nlp.concept.import", mode = Mode.WRITE)
    @Description("Performs the text annotation and store it into the graph")
    public Stream<NodeResult> annotate(@Name("conceptRequest") Map<String, Object> conceptRequest) {
        ConceptRequest request = mapper.convertValue(conceptRequest, ConceptRequest.class);
        Node result = getNLPManager().importConcept(request);
        return Stream.of(new NodeResult(result));
    }
}
