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
import com.graphaware.nlp.workflow.output.StoreAnnotatedTextWorkflowOutput;
import com.graphaware.nlp.workflow.output.WorkflowOutput;

import java.util.Collections;
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
    @Description("List WorkflowOutput classes available")
    public Stream<WorkflowItemInfo> available() {
        try {
            Set<WorkflowItemInfo> workflowOutput = getWorkflowManager().getWorkflowOutputClasses();
            return workflowOutput.stream();
        } catch (Exception e) {
            LOG.error("ERROR in WorkflowOutputProcedure", e);
            throw new RuntimeException(e);
        }
    }

    @Procedure(name = "ga.nlp.workflow.output.create", mode = Mode.WRITE)
    @Description("Create a WorkflowOutput")
    public Stream<WorkflowInstanceItemInfo> create(@Name(value = "name") String name,
            @Name(value = "class", defaultValue = "") String classname,
            @Name(value = "parameters", defaultValue = "") Map<String, Object> parameters) {
        try {
            WorkflowOutput workflowInput = getWorkflowManager().createWorkflowOutput(name, classname, parameters);
            return Stream.of(workflowInput.getInfo());
        } catch (Exception e) {
            LOG.error("ERROR in WorkflowOutputProcedure", e);
            throw new RuntimeException(e);
        }
    }

    @Procedure(name = "ga.nlp.workflow.createStoreAnnotationOutput", mode = Mode.WRITE)
    @Description("Shortcut DSL method for StoreAnnotatedTextWorkflowOutput")
    public Stream<WorkflowInstanceItemInfo> createStoreAnnotationOutput(@Name("name") String name, @Name(value = "relType", defaultValue = "HAS_ANNOTATED_TEXT") String relType) {
        String query = "MATCH (n), (x) WHERE id(n) = toInteger({entryId}) AND id(x) = toInteger({annotatedTextId}) MERGE (n)-[:`" + relType.trim() + "`]->(x)";
        Map<String, Object> parameters = Collections.singletonMap("query", query);
        String cl = StoreAnnotatedTextWorkflowOutput.class.getName();

        return create(name, cl, parameters);
    }

    @Procedure(name = "ga.nlp.workflow.output.instance.list", mode = Mode.READ)
    @Description("List WorkflowOutput")
    public Stream<WorkflowInstanceItemInfo> list() {
        try {
            Set<WorkflowInstanceItemInfo> workflowOutput = getWorkflowManager().getWorkflowOutputInstances();
            return workflowOutput.stream();
        } catch (Exception e) {
            LOG.error("ERROR in WorkflowOutputProcedure", e);
            throw new RuntimeException(e);
        }
    }

    @Procedure(name = "ga.nlp.workflow.output.get", mode = Mode.READ)
    @Description("Get WorkflowOutput")
    public Stream<WorkflowInstanceItemInfo> get(@Name(value = "name") String name) {
        WorkflowOutput workflowOutput = getWorkflowManager().getWorkflowOutput(name);
        if (workflowOutput != null) {
            return Stream.of(workflowOutput.getInfo());
        }
        return null;
    }

    @Procedure(name = "ga.nlp.workflow.output.update", mode = Mode.WRITE)
    @Description("Update a WorkflowOutput")
    public Stream<NodeResult> update(@Name(value = "name") String name,
            @Name(value = "class", defaultValue = "") String classname,
            @Name(value = "parameters", defaultValue = "") Map<String, Object> parameters) {
        return null;
    }

    @Procedure(name = "ga.nlp.workflow.output.delete", mode = Mode.WRITE)
    @Description("Delete a WorkflowOutput")
    public Stream<SingleResult> delete(@Name(value = "name") String name) {
        WorkflowOutput workflowOutput = getWorkflowManager().deleteWorkflowOutput(name);
        if (workflowOutput != null) {
            return Stream.of(SingleResult.success());
        } else {
            return Stream.of(SingleResult.fail());
        }
    } 

}
