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
import com.graphaware.nlp.workflow.input.QueryBasedWorkflowInput;
import com.graphaware.nlp.workflow.input.WorkflowInput;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowInputProcedure extends AbstractDSL {

    private static final Logger LOG = LoggerFactory.getLogger(WorkflowInputProcedure.class);

    @Procedure(name = "ga.nlp.workflow.input.class.list", mode = Mode.READ)
    @Description("List workflow input classes available")
    public Stream<WorkflowItemInfo> available() {
        try {
            Set<WorkflowItemInfo> workflowInput = getWorkflowManager().getWorkflowInputClasses();
            return workflowInput.stream();
        } catch (Exception e) {
            LOG.error("ERROR in WorkflowInputProcedure", e);
            throw new RuntimeException(e);
        }
    }

    @Procedure(name = "ga.nlp.workflow.input.create", mode = Mode.WRITE)
    @Description("Create a Pipeline input")
    public Stream<WorkflowInstanceItemInfo> create(@Name(value = "name") String name,
            @Name(value = "class", defaultValue = "") String classname,
            @Name(value = "parameters", defaultValue = "") Map<String, Object> parameters) {
        try {
            WorkflowInput workflowInput = getWorkflowManager().createWorkflowInput(name, classname, parameters);
            return Stream.of(workflowInput.getInfo());
        } catch (Exception e) {
            LOG.error("ERROR in WorkflowInputProcedure", e);
            throw new RuntimeException(e);
        }
    }

    @Procedure(name = "ga.nlp.workflow.createQueryInput", mode = Mode.WRITE)
    public Stream<WorkflowInstanceItemInfo> createQueryInput(@Name("name") String name, @Name("parameters") Map<String, Object> parameters) {
        if (!parameters.containsKey("query")) {
            throw new RuntimeException("the parameters must contain a query key");
        }
        if (!StringUtils.isNotBlank(name)) {
            throw new RuntimeException("Invalid name");
        }
        if (!StringUtils.isNotBlank(parameters.get("query").toString())) {
            throw new RuntimeException("Invalid query");
        }
        String cl = QueryBasedWorkflowInput.class.getName();

        return create(name, cl, parameters);
    }

    @Procedure(name = "ga.nlp.workflow.input.instance.list", mode = Mode.READ)
    @Description("List Pipelines input")
    public Stream<WorkflowInstanceItemInfo> list() {
        try {
            Set<WorkflowInstanceItemInfo> workflowInput = getWorkflowManager().getWorkflowInputInstances();
            return workflowInput.stream();
        } catch (Exception e) {
            LOG.error("ERROR in WorkflowInputProcedure", e);
            throw new RuntimeException(e);
        }
    }

    @Procedure(name = "ga.nlp.workflow.input.get", mode = Mode.READ)
    @Description("Get WorkflowInput")
    public Stream<WorkflowInstanceItemInfo> get(@Name(value = "name") String name) {
        WorkflowInput workflowInput = getWorkflowManager().getWorkflowInput(name);
        if (workflowInput != null) {
            return Stream.of(workflowInput.getInfo());
        }
        return null;
    }

    @Procedure(name = "ga.nlp.workflow.input.update", mode = Mode.WRITE)
    @Description("Update a Pipeline update")
    public Stream<NodeResult> update(@Name(value = "name") String name,
            @Name(value = "class", defaultValue = "") String classname,
            @Name(value = "parameters", defaultValue = "") Map<String, Object> parameters
    ) {
        return null;
    }

    @Procedure(name = "ga.nlp.workflow.input.delete", mode = Mode.WRITE)
    @Description("Delete a WorkflowInput")
    public Stream<SingleResult> delete(@Name(value = "name") String name) {
        WorkflowInput workflowInput = getWorkflowManager().deleteWorkflowInput(name);
        if (workflowInput != null) {
            return Stream.of(SingleResult.success());
        } else {
            return Stream.of(SingleResult.fail());
        }
    }
}
