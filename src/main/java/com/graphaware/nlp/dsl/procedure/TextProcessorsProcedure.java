package com.graphaware.nlp.dsl.procedure;

import com.graphaware.nlp.dsl.AbstractDSL;
import com.graphaware.nlp.processor.PipelineInfo;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

public class TextProcessorsProcedure extends AbstractDSL {

    @Procedure("ga.nlp.processor.getPipelineInfos")
    @Description("Returns the pipeline informations")
    public Stream<PipelineInfo> getPipelineInfos() {
        return getNLPManager().getPipelineInformations().stream();
    }
}
