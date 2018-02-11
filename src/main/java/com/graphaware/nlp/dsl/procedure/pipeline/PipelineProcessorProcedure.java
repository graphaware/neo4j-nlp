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
package com.graphaware.nlp.dsl.procedure.pipeline;

import com.graphaware.nlp.dsl.AbstractDSL;
import com.graphaware.nlp.dsl.result.NodeResult;
import com.graphaware.nlp.dsl.result.ProcessorItem;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

public class PipelineProcessorProcedure extends AbstractDSL {
    
    @Procedure(name = "ga.nlp.pipeline.processor.available", mode = Mode.WRITE)
    @Description("Create a Pipeline Processor")
    public Stream<ProcessorItem> available() {
        Set<ProcessorItem> pipelineProcessors = getPipelineManager().getPipelineProcessors();
        return pipelineProcessors.stream();
    }
    
    @Procedure(name = "ga.nlp.pipeline.processor.create", mode = Mode.WRITE)
    @Description("Create a Pipeline Processor")
    public Stream<NodeResult> create(@Name(value = "id") String id,
            @Name(value = "processorName") String processorName, 
            @Name(value = "parameters") Map<String, Object> parameters) {
        getPipelineManager().createPipelineProcessor(id, processorName, parameters);
        return null;
    }
    
    @Procedure(name = "ga.nlp.pipeline.processor.list", mode = Mode.READ)
    @Description("List Pipelines")
    public Stream<NodeResult> list() {
        return null;
    }
    
    @Procedure(name = "ga.nlp.pipeline.processor.get", mode = Mode.READ)
    @Description("Get Pipeline info")
    public Stream<NodeResult> get() {
        return null;
    }
    
    @Procedure(name = "ga.nlp.pipeline.processor.update", mode = Mode.WRITE)
    @Description("Update a Pipeline update")
    public Stream<NodeResult> update(@Name(value = "name") String name,
            @Name(value = "class", defaultValue = "") String classname, 
            @Name(value = "parameters", defaultValue = "" ) Map<String, Object> parameters) {
        return null;
    }
    
    @Procedure(name = "ga.nlp.pipeline.processor.update", mode = Mode.WRITE)
    @Description("Delete a Pipeline")
    public Stream<NodeResult> delete(@Name(value = "name") String name) {
        return null;
    }  
    
}
