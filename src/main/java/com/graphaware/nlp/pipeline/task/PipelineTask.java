/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.pipeline.task;

import com.graphaware.nlp.annotation.NLPTask;
import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.dsl.procedure.pipeline.PipelineInputProcedure;
import com.graphaware.nlp.pipeline.PipelineItem;
import com.graphaware.nlp.pipeline.PipelineManager;
import com.graphaware.nlp.pipeline.processor.PipelineProcessor;
import com.graphaware.nlp.pipeline.input.PipelineInput;
import com.graphaware.nlp.pipeline.input.PipelineInputEntry;
import com.graphaware.nlp.pipeline.output.PipelineOutput;
import com.graphaware.nlp.pipeline.processor.PipelineProcessorOutputEntry;
import java.util.Iterator;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NLPTask(name = "PipelineTask")
public class PipelineTask
        extends PipelineItem<PipelineTaskConfiguration> implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(PipelineInputProcedure.class);

    public static final String PIPELINE_TASK_KEY_PREFIX = "PIPELINE_TASK_";

    private PipelineInput input;
    private PipelineProcessor process;
    private PipelineOutput output;

    public PipelineTask(String name, GraphDatabaseService database) {
        super(name, database);
    }

    @Override
    public void init(Map<String, Object> parameters) {
        setConfiguration(new PipelineTaskConfiguration(parameters));
        String inputName = getConfiguration().getInput();
        String outputName = getConfiguration().getOutput();
        String processName = getConfiguration().getProcessor();
        if (inputName == null || outputName == null || processName == null) {
            throw new RuntimeException("The task cannot be initialized. "
                    + "Some parameters are null");
        }
        this.input = PipelineManager.getInstance().getPipelineInput(inputName);
        this.output = PipelineManager.getInstance().getPipelineOutput(outputName);
        this.process = PipelineManager.getInstance().getPipelineProcessor(processName);
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
            PipelineInputEntry next = (PipelineInputEntry) inputIterator.next();
            PipelineProcessorOutputEntry processor = process.process(next);// Check if null
            AnnotatedText annotateText = processor.getAnnotateText();
            output.process(new PipelineProcessorOutputEntry(annotateText, next.getId()));
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
