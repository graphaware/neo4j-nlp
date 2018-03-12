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
import com.graphaware.nlp.dsl.result.PipelineInstanceItemInfo;
import com.graphaware.nlp.dsl.result.PipelineItemInfo;
import com.graphaware.nlp.dsl.result.SingleResult;
import com.graphaware.nlp.pipeline.task.PipelineTask;
import com.graphaware.nlp.pipeline.task.TaskManager;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PipelineTaskProcedure  extends AbstractDSL {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineTaskProcedure.class);

    @Procedure(name = "ga.nlp.pipeline.task.class.list", mode = Mode.READ)
    @Description("List pipeline task classes available")
    public Stream<PipelineItemInfo> available() {
        try {
            Set<PipelineItemInfo> pipelineTask = getPipelineManager().getPipelineTaskClasses();
            return pipelineTask.stream();
        } catch (Exception e) {
            LOG.error("ERROR in PipelineTaskProcedure", e);
            throw new RuntimeException(e);
        }
    }
    
    @Procedure(name = "ga.nlp.pipeline.task.create", mode = Mode.WRITE)
    @Description("Create a Pipeline input")
    public Stream<PipelineInstanceItemInfo> create(@Name(value = "name") String name,
            @Name(value = "class", defaultValue = "") String classname, 
            @Name(value = "parameters", defaultValue = "" ) Map<String, Object> parameters) {
        try {
            PipelineTask pipelineTask = getPipelineManager().createPipelineTask(name, classname, parameters);
            return Stream.of(pipelineTask.getInfo());
        } catch (Exception e) {
            LOG.error("ERROR in PipelineTaskProcedure", e);
            throw new RuntimeException(e);
        }
    }
    
    @Procedure(name = "ga.nlp.pipeline.task.instance.list", mode = Mode.READ)
    @Description("List Pipelines")
    public Stream<PipelineInstanceItemInfo> list() {
        try {
            Set<PipelineInstanceItemInfo> pipelineTasks = getPipelineManager().getPipelineTaskInstances();
            return pipelineTasks.stream();
        } catch (Exception e) {
            LOG.error("ERROR in PipelineTaskProcedure", e);
            throw new RuntimeException(e);
        }
    }
    
    @Procedure(name = "ga.nlp.pipeline.task.get", mode = Mode.READ)
    @Description("Get Pipeline info")
    public Stream<NodeResult> get() {
        return null;
    }
    
    @Procedure(name = "ga.nlp.pipeline.task.update", mode = Mode.WRITE)
    @Description("Update a Pipeline update")
    public Stream<NodeResult> update(@Name(value = "name") String name,
            @Name(value = "class", defaultValue = "") String classname, 
            @Name(value = "parameters", defaultValue = "" ) Map<String, Object> parameters) {
        return null;
    }
    
    @Procedure(name = "ga.nlp.pipeline.task.delete", mode = Mode.WRITE)
    @Description("Delete a Pipeline")
    public Stream<NodeResult> delete(@Name(value = "name") String name) {
        return null;
    } 
    
    @Procedure(name = "ga.nlp.pipeline.task.start", mode = Mode.WRITE)
    @Description("Start a Task")
    public Stream<SingleResult> start(@Name(value = "name") String name) {
        try {
            PipelineTask pipelineTask = getPipelineManager().getPipelineTask(name);
            if (pipelineTask == null) {
                throw new RuntimeException("Pipeline task not found");
            }
            TaskManager.getInstance().execute(pipelineTask);
            return Stream.of(SingleResult.success());
        } catch (Exception e) {
            LOG.error("ERROR in PipelineTaskProcedure", e);
            throw new RuntimeException(e);
        }
    } 
    
    @Procedure(name = "ga.nlp.pipeline.task.stop", mode = Mode.WRITE)
    @Description("Start a Task")
    public Stream<NodeResult> stop(@Name(value = "name") String name) {
        return null;
    } 
    
    @Procedure(name = "ga.nlp.pipeline.task.status", mode = Mode.READ)
    @Description("Start a Task")
    public Stream<NodeResult> status(@Name(value = "name") String name) {
        return null;
    } 
    
}
