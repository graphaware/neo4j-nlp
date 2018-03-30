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
import com.graphaware.nlp.dsl.result.SingleResult;
import com.graphaware.nlp.dsl.result.WorkflowInstanceItemInfo;
import com.graphaware.nlp.dsl.result.WorkflowItemInfo;
import com.graphaware.nlp.workflow.processor.WorkflowProcessor;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.graphaware.nlp.workflow.processor.WorkflowTextProcessor;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowProcessorProcedure extends AbstractDSL {

    private static final Logger LOG = LoggerFactory.getLogger(WorkflowInputProcedure.class);

    @Procedure(name = "ga.nlp.workflow.processor.class.list", mode = Mode.READ)
    @Description("Create a WorkflowProcessor")
    public Stream<WorkflowItemInfo> available() {
        try {
            Set<WorkflowItemInfo> workflowProcessors = getWorkflowManager().getWorkflowProcessorClasses();
            return workflowProcessors.stream();
        } catch (Exception e) {
            LOG.error("ERROR in WorkflowProcessorProcedure", e);
            throw new RuntimeException(e);
        }
    }

    @Procedure(name = "ga.nlp.workflow.processor.create", mode = Mode.WRITE)
    @Description("Create a WorkflowProcessor")
    public Stream<WorkflowInstanceItemInfo> create(@Name(value = "name") String name,
            @Name(value = "class") String className,
            @Name(value = "parameters") Map<String, Object> parameters) {
        try {
            WorkflowProcessor workflowInput = getWorkflowManager().createWorkflowProcessor(name, className, parameters);
            return Stream.of(workflowInput.getInfo());
        } catch (Exception e) {
            LOG.error("ERROR in WorkflowProcessorProcedure", e);
            throw new RuntimeException(e);
        }
    }

    @Procedure(name = "ga.nlp.workflow.createTextProcessor", mode = Mode.WRITE)
    @Description("Shortcut DSL method for creating a WorkflowTextProcessor")
    public Stream<WorkflowInstanceItemInfo> createTextProcessor(@Name("name") String name, @Name("parameters") Map<String, Object> parameters) {
        checkStringNotBlankOrFail(name, "name");
        checkMapContainsValueAndNotBlank("pipeline", parameters);
        String cl = WorkflowTextProcessor.class.getName();

        return create(name, cl, parameters);
    }

    @Procedure(name = "ga.nlp.workflow.processor.instance.list", mode = Mode.READ)
    @Description("List WorkflowProcessors")
    public Stream<WorkflowInstanceItemInfo> list() {
        try {
            Set<WorkflowInstanceItemInfo> workflowProcessors = getWorkflowManager().getWorkflowProcessorInstances();
            return workflowProcessors.stream();
        } catch (Exception e) {
            LOG.error("ERROR in WorkflowProcessorProcedure", e);
            throw new RuntimeException(e);
        }
    }

    @Procedure(name = "ga.nlp.workflow.processor.get", mode = Mode.READ)
    @Description("Get WorkflowProcessor info")
    public Stream<WorkflowInstanceItemInfo> get(@Name(value = "name") String name) {
        WorkflowProcessor workflowProc = getWorkflowManager().getWorkflowProcessor(name);
        if (workflowProc != null) {
            return Stream.of(workflowProc.getInfo());
        }
        return null;
    }

    @Procedure(name = "ga.nlp.workflow.processor.update", mode = Mode.WRITE)
    @Description("Update a WorkflowProcessor update")
    public Stream<NodeResult> update(@Name(value = "name") String name,
            @Name(value = "class", defaultValue = "") String classname,
            @Name(value = "parameters", defaultValue = "") Map<String, Object> parameters) {
        return null;
    }

    @Procedure(name = "ga.nlp.workflow.processor.delete", mode = Mode.WRITE)
    @Description("Delete a WorkflowProcessor")
    public Stream<SingleResult> delete(@Name(value = "name") String name) {
        WorkflowProcessor workflowProcessor = getWorkflowManager().deleteWorkflowProcessor(name);
        if (workflowProcessor != null) {
            return Stream.of(SingleResult.success());
        } else {
            return Stream.of(SingleResult.fail());
        }
    } 

}
