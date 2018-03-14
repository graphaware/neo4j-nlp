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
package com.graphaware.nlp.dsl.procedure.workflow;

import com.graphaware.nlp.dsl.AbstractDSL;
import com.graphaware.nlp.dsl.result.NodeResult;
import com.graphaware.nlp.dsl.result.WorkflowInstanceItemInfo;
import com.graphaware.nlp.dsl.result.WorkflowItemInfo;
import com.graphaware.nlp.workflow.output.WorkflowOutput;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowOutputProcedure extends AbstractDSL {

    private static final Logger LOG = LoggerFactory.getLogger(WorkflowOutputProcedure.class);

    @Procedure(name = "ga.nlp.workflow.output.class.list", mode = Mode.READ)
    @Description("List workflow output classes available")
    public Stream<WorkflowItemInfo> available() {
        try {
            Set<WorkflowItemInfo> workflowOutput = getPipelineManager().getPipelineOutputClasses();
            return workflowOutput.stream();
        } catch (Exception e) {
            LOG.error("ERROR in PipelineOutputProcedure", e);
            throw new RuntimeException(e);
        }
    }

    @Procedure(name = "ga.nlp.workflow.output.create", mode = Mode.WRITE)
    @Description("Create a Pipeline input")
    public Stream<WorkflowInstanceItemInfo> create(@Name(value = "name") String name,
            @Name(value = "class", defaultValue = "") String classname,
            @Name(value = "parameters", defaultValue = "") Map<String, Object> parameters) {
        try {
            WorkflowOutput workflowInput = getPipelineManager().createPipelineOutput(name, classname, parameters);
            return Stream.of(workflowInput.getInfo());
        } catch (Exception e) {
            LOG.error("ERROR in PipelineOutputProcedure", e);
            throw new RuntimeException(e);
        }
    }

    @Procedure(name = "ga.nlp.workflow.output.instance.list", mode = Mode.READ)
    @Description("List Pipelines")
    public Stream<WorkflowInstanceItemInfo> list() {
        try {
            Set<WorkflowInstanceItemInfo> workflowOutput = getPipelineManager().getPipelineOutputInstances();
            return workflowOutput.stream();
        } catch (Exception e) {
            LOG.error("ERROR in PipelineInputProcedure", e);
            throw new RuntimeException(e);
        }
    }

    @Procedure(name = "ga.nlp.workflow.output.get", mode = Mode.READ)
    @Description("Get Pipeline info")
    public Stream<NodeResult> get() {
        return null;
    }

    @Procedure(name = "ga.nlp.workflow.output.update", mode = Mode.WRITE)
    @Description("Update a Pipeline update")
    public Stream<NodeResult> update(@Name(value = "name") String name,
            @Name(value = "class", defaultValue = "") String classname,
            @Name(value = "parameters", defaultValue = "") Map<String, Object> parameters) {
        return null;
    }

    @Procedure(name = "ga.nlp.workflow.output.delete", mode = Mode.WRITE)
    @Description("Delete a Pipeline")
    public Stream<NodeResult> delete(@Name(value = "name") String name) {
        return null;
    }

}
