/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.workflow.task;

import com.graphaware.nlp.annotation.NLPTask;
import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.dsl.procedure.workflow.WorkflowInputProcedure;
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

@NLPTask(name = "PipelineTask")
public class WorkflowTask
        extends WorkflowItem<WorkflowTaskConfiguration> implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(WorkflowInputProcedure.class);

    public static final String PIPELINE_TASK_KEY_PREFIX = "PIPELINE_TASK_";

    private WorkflowInput input;
    private WorkflowProcessor process;
    private WorkflowOutput output;

    public WorkflowTask(String name, GraphDatabaseService database) {
        super(name, database);
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
        this.input = WorkflowManager.getInstance().getPipelineInput(inputName);
        this.output = WorkflowManager.getInstance().getPipelineOutput(outputName);
        this.process = WorkflowManager.getInstance().getPipelineProcessor(processName);
        if (input == null || output == null || process == null) {
            throw new RuntimeException("The task cannot be initialized. "
                    + "Some parameters are invalid");
        }
    }

    @Override
    public void run() {
        doProcess();
    }

    public void doProcess() {
        Iterator inputIterator = input.iterator();
        while (inputIterator.hasNext()) {
            WorkflowInputEntry next = (WorkflowInputEntry) inputIterator.next();
            WorkflowProcessorOutputEntry processor = process.process(next);// Check if null
            AnnotatedText annotateText = processor.getAnnotateText();
            output.process(new WorkflowProcessorOutputEntry(annotateText, next.getId()));
        }
    }

    @Override
    public String getPrefix() {
        return PIPELINE_TASK_KEY_PREFIX;
    }
    
    public boolean isSync() {
        return getConfiguration().isSync();
    }
}
