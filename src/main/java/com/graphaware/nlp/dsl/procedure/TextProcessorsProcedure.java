package com.graphaware.nlp.dsl.procedure;

import com.graphaware.nlp.dsl.AbstractDSL;
import static com.graphaware.nlp.dsl.AbstractDSL.mapper;
import com.graphaware.nlp.dsl.PipelineSpecification;
import com.graphaware.nlp.dsl.result.ProcessorsList;
import com.graphaware.nlp.dsl.result.SingleResult;
import com.graphaware.nlp.processor.PipelineInfo;
import java.util.Map;
import java.util.Set;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Procedure;

import java.util.stream.Stream;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;

public class TextProcessorsProcedure extends AbstractDSL {

    @Procedure(name = "ga.nlp.processor.getProcessors", mode = Mode.READ)
    @Description("Get the list of available Text Processors with the related classes")
    public Stream<ProcessorsList> getProcessors() {
        Set<ProcessorsList> result = getNLPManager().getProcessors();
        return result.stream();
    }
    
    @Procedure(name = "ga.nlp.processor.addPipeline", mode = Mode.WRITE)
    @Description("Add custom pipeline to a Text Processor")
    public Stream<SingleResult> addPipeline(@Name("addPipelineRequest") Map<String, Object> addPipelineRequest) {
        try {
            PipelineSpecification request = mapper.convertValue(addPipelineRequest, PipelineSpecification.class);
            getNLPManager().addPipeline(request);
            return Stream.of(SingleResult.success());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Procedure(name = "ga.nlp.processor.removePipeline", mode = Mode.WRITE)
    @Description("Remove the given pipeline from the given text processor")
    public Stream<SingleResult> removePipeline(@Name("pipeline") String pipeline, @Name("textProcessor") String textProcessor) {
        try {
            getNLPManager().removePipeline(pipeline, textProcessor);
            return Stream.of(SingleResult.success());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Procedure("ga.nlp.processor.getPipelineInfos")
    @Description("Returns the pipeline informations")
    public Stream<PipelineInfo> getPipelineInfos() {
        return getNLPManager().getPipelineInformations().stream();
    }
}
