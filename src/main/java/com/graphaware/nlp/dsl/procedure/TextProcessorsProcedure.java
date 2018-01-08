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
import com.graphaware.nlp.dsl.request.CustomModelsRequest;
import com.graphaware.nlp.dsl.request.PipelineSpecification;
import com.graphaware.nlp.dsl.result.ProcessorsList;
import com.graphaware.nlp.dsl.result.SingleResult;
import com.graphaware.nlp.processor.PipelineInfo;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

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
    
    @Procedure("ga.nlp.processor.getPipelines")
    @Description("Returns the pipeline informations")
    public Stream<PipelineInfo> getPipelines(@Name(value = "pipelineName", defaultValue = "") String pipelineName) {
        return getNLPManager().getPipelineInformations(pipelineName).stream();
    }

    @Procedure(name = "ga.nlp.processor.train", mode = Mode.WRITE)
    @Description("Procedure for training custom models.")
    public Stream<SingleResult> train(@Name("customModelsRequest") Map<String, Object> customModelsRequest) {
        try {
            CustomModelsRequest request = CustomModelsRequest.fromMap(customModelsRequest);
            Object result = getNLPManager().train(request);
            return Stream.of(new SingleResult(result));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Procedure(name = "ga.nlp.processor.test", mode = Mode.WRITE)
    @Description("Procedure for testing custom models.")
    public Stream<SingleResult> test(@Name("customModelsRequest") Map<String, Object> customModelsRequest) {
        try {
            CustomModelsRequest request = CustomModelsRequest.fromMap(customModelsRequest);
            Object result = getNLPManager().test(request);
            return Stream.of(new SingleResult(result));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
