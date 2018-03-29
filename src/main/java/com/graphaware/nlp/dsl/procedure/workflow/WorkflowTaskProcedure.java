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
import com.graphaware.nlp.dsl.result.WorkflowInstanceItemInfo;
import com.graphaware.nlp.dsl.result.WorkflowItemInfo;
import com.graphaware.nlp.dsl.result.SingleResult;
import com.graphaware.nlp.workflow.task.WorkflowTask;
import com.graphaware.nlp.workflow.task.TaskManager;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowTaskProcedure  extends AbstractDSL {
    private static final Logger LOG = LoggerFactory.getLogger(WorkflowTaskProcedure.class);

    @Procedure(name = "ga.nlp.workflow.task.class.list", mode = Mode.READ)
    @Description("List workflow task classes available")
    public Stream<WorkflowItemInfo> available() {
        try {
            Set<WorkflowItemInfo> workflowTask = getWorkflowManager().getWorkflowTaskClasses();
            return workflowTask.stream();
        } catch (Exception e) {
            LOG.error("ERROR in WorkflowTaskProcedure", e);
            throw new RuntimeException(e);
        }
    }
    
    @Procedure(name = "ga.nlp.workflow.task.create", mode = Mode.WRITE)
    @Description("Create a Pipeline input")
    public Stream<WorkflowInstanceItemInfo> create(@Name(value = "name") String name,
            @Name(value = "class", defaultValue = "") String classname, 
            @Name(value = "parameters", defaultValue = "" ) Map<String, Object> parameters) {
        try {
            WorkflowTask workflowTask = getWorkflowManager().createWorkflowTask(name, classname, parameters);
            return Stream.of(workflowTask.getInfo());
        } catch (Exception e) {
            LOG.error("ERROR in WorkflowTaskProcedure", e);
            throw new RuntimeException(e);
        }
    }
    
    @Procedure(name = "ga.nlp.workflow.task.instance.list", mode = Mode.READ)
    @Description("List Pipelines")
    public Stream<WorkflowInstanceItemInfo> list() {
        try {
            Set<WorkflowInstanceItemInfo> workflowTasks = getWorkflowManager().getWorkflowTaskInstances();
            return workflowTasks.stream();
        } catch (Exception e) {
            LOG.error("ERROR in WorkflowTaskProcedure", e);
            throw new RuntimeException(e);
        }
    }
    
    @Procedure(name = "ga.nlp.workflow.task.get", mode = Mode.READ)
    @Description("Get Pipeline info")
    public Stream<WorkflowInstanceItemInfo> get(@Name(value = "name") String name) {
        WorkflowTask workflowTask = getWorkflowManager().getWorkflowTask(name);
        if (workflowTask != null) {
            return Stream.of(workflowTask.getInfo());
        }
        return null;
    }
    
    @Procedure(name = "ga.nlp.workflow.task.update", mode = Mode.WRITE)
    @Description("Update a Pipeline update")
    public Stream<SingleResult> update(@Name(value = "name") String name,
            @Name(value = "class", defaultValue = "") String classname, 
            @Name(value = "parameters", defaultValue = "" ) Map<String, Object> parameters) {
        return null;
    }
    
    @Procedure(name = "ga.nlp.workflow.task.delete", mode = Mode.WRITE)
    @Description("Delete a Workflow Task")
    public Stream<SingleResult> delete(@Name(value = "name") String name) {
        WorkflowTask workflowTask = getWorkflowManager().deleteWorkflowTask(name);
        if (workflowTask != null) {
            return Stream.of(SingleResult.success());
        } else {
            return Stream.of(SingleResult.fail());
        }
    } 
    
    @Procedure(name = "ga.nlp.workflow.task.start", mode = Mode.WRITE)
    @Description("Start a Task")
    public Stream<SingleResult> start(@Name(value = "name") String name) {
        try {
            WorkflowTask workflowTask = getWorkflowManager().getWorkflowTask(name);
            if (workflowTask == null) {
                throw new RuntimeException("Pipeline task not found");
            }
            TaskManager.getInstance().execute(workflowTask);
            return Stream.of(SingleResult.success());
        } catch (Exception e) {
            LOG.error("ERROR in WorkflowTaskProcedure", e);
            throw new RuntimeException(e);
        }
    } 
    
    @Procedure(name = "ga.nlp.workflow.task.stop", mode = Mode.WRITE)
    @Description("Start a Task")
    public Stream<WorkflowInstanceItemInfo> stop(@Name(value = "name") String name) {
        try {
            WorkflowTask workflowTask = getWorkflowManager().getWorkflowTask(name);
            if (workflowTask == null) {
                throw new RuntimeException("Pipeline task not found");
            }
            TaskManager.getInstance().stop(workflowTask);
            return Stream.of(workflowTask.getInfo());
        } catch (Exception e) {
            LOG.error("ERROR in WorkflowTaskProcedure", e);
            throw new RuntimeException(e);
        }
    } 
    
    @Procedure(name = "ga.nlp.workflow.task.status", mode = Mode.READ)
    @Description("Start a Task")
    public Stream<WorkflowInstanceItemInfo> status(@Name(value = "name") String name) {
        try {
            WorkflowTask workflowTask = getWorkflowManager().getWorkflowTask(name);
            if (workflowTask == null) {
                throw new RuntimeException("Pipeline task not found");
            }
            return Stream.of(workflowTask.getInfo());
        } catch (Exception e) {
            LOG.error("ERROR in WorkflowTaskProcedure", e);
            throw new RuntimeException(e);
        }
    } 
    
}
