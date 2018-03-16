/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.workflow.task;

import com.graphaware.nlp.annotation.NLPTask;
import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.dsl.procedure.workflow.WorkflowInputProcedure;
import com.graphaware.nlp.dsl.result.WorkflowInstanceItemInfo;
import com.graphaware.nlp.workflow.WorkflowItem;
import com.graphaware.nlp.workflow.WorkflowManager;
import com.graphaware.nlp.workflow.processor.WorkflowProcessor;
import com.graphaware.nlp.workflow.input.WorkflowInput;
import com.graphaware.nlp.workflow.input.WorkflowInputEntry;
import com.graphaware.nlp.workflow.output.WorkflowOutput;
import com.graphaware.nlp.workflow.processor.WorkflowProcessorOutputEntry;
import java.util.Iterator;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NLPTask(name = "WorkflowTask")
public class WorkflowTask
        extends WorkflowItem<WorkflowTaskConfiguration> {

    private static final Logger LOG = LoggerFactory.getLogger(WorkflowInputProcedure.class);

    public static final String PIPELINE_TASK_KEY_PREFIX = "PIPELINE_TASK_";

    private WorkflowInput input;
    private WorkflowProcessor process;
    private WorkflowOutput output;

    private TaskStatus status;
    private volatile boolean cancelled = false;

    public WorkflowTask(String name, GraphDatabaseService database) {
        super(name, database);
        this.status = TaskStatus.IDLE;
    }

    @Override
    public void init(Map<String, Object> parameters) {
        setConfiguration(new WorkflowTaskConfiguration(parameters));
        String inputName = getConfiguration().getInput();
        String outputName = getConfiguration().getOutput();
        String processName = getConfiguration().getProcessor();
        if (inputName == null || outputName == null || processName == null) {
            throw new RuntimeException("The task cannot be initialized. "
                    + "Some parameters are null");
        }
        this.input = WorkflowManager.getInstance().getWorkflowInput(inputName);
        this.output = WorkflowManager.getInstance().getWorkflowOutput(outputName);
        this.process = WorkflowManager.getInstance().getWorkflowProcessor(processName);
        if (input == null || output == null || process == null) {
            throw new RuntimeException("The task cannot be initialized. "
                    + "Some parameters are invalid");
        }
    }

    public void doProcess() {
        if (getStatus() != TaskStatus.IDLE) {
            throw new RuntimeException("The task " + getName() + " is not in IDLE state");
        }
        if (isValid()) {
            throw new RuntimeException("The task is invalid. Check logs for the reason.");
        }
        setStatus(TaskStatus.RUNNING);
        try {
            Iterator inputIterator = input.iterator();
            while (inputIterator.hasNext()
                    && !cancelled) {
                WorkflowInputEntry next = (WorkflowInputEntry) inputIterator.next();
                WorkflowProcessorOutputEntry processorOutput = process.process(next);
                if (processorOutput != null) {
                    AnnotatedText annotateText = processorOutput.getAnnotateText();
                    output.process(new WorkflowProcessorOutputEntry(annotateText, next.getId()));
                }
            }
        } catch (Exception ex) {
            LOG.error("The task " + getName() + " failed", ex);
            setStatus(TaskStatus.FAILED);
            return;
        }
        if (cancelled) {
            setStatus(TaskStatus.CANCELLED);
        } else {
            setStatus(TaskStatus.SUCCEEDED);
        }
    }

    @Override
    public String getPrefix() {
        return PIPELINE_TASK_KEY_PREFIX;
    }

    public boolean isSync() {
        return getConfiguration().isSync();
    }

    public void cancel() {
        cancelled = true;
    }

    public void reset() {
        status = TaskStatus.IDLE;
        cancelled = false;
    }

    public TaskStatus getStatus() {
        return status;
    }

    private void setStatus(TaskStatus status) {
        this.status = status;
    }

    @Override
    public boolean isValid() {
        if (!input.isValid()) {
            LOG.warn("The input " + input.getName() + " for the task " + getName() + " is no valid");
            return false;
        } 
        if (!process.isValid()) {
            LOG.warn("The processor " + input.getName() + " for the task " + getName() + " is no valid");
            return false;
        }
        if (!output.isValid()) {
            LOG.warn("The output " + input.getName() + " for the task " + getName() + " is no valid");
            return false;
        }
        return true;
    }

    @Override
    public WorkflowInstanceItemInfo getInfo() {
        return new WorkflowTaskInstanceItemInfo(
                this.getClass().getName(),
                getName(),
                getConfiguration().getConfiguration(),
                isValid(),
                status);
    }
    
    

}
