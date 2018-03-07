/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.pipeline.task;

import com.graphaware.nlp.dsl.procedure.pipeline.PipelineInputProcedure;
import com.graphaware.nlp.pipeline.PipelineItem;
import com.graphaware.nlp.pipeline.PipelineManager;
import com.graphaware.nlp.pipeline.processor.PipelineProcessor;
import com.graphaware.nlp.pipeline.input.PipelineInput;
import com.graphaware.nlp.pipeline.output.PipelineOutput;
import com.graphaware.nlp.pipeline.processor.PipelineTextProcessorConfiguration;
import java.util.Iterator;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PipelineTask
        extends PipelineItem<PipelineTextProcessorConfiguration> implements Runnable {

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
        setConfiguration(new PipelineTextProcessorConfiguration(parameters));
//        PipelineManager.getInstance().getPipelineInput();
    }

    public void setInput(PipelineInput input) {
        this.input = input;
    }

    public void setProcess(PipelineProcessor process) {
        this.process = process;
    }

    public void setOutput(PipelineOutput output) {
        this.output = output;
    }

    @Override
    public void run() {
        Iterator inputIterator = input.iterator();
        while (inputIterator.hasNext()) {
            Object next = inputIterator.next();

        }
    }

    @Override
    public String getPrefix() {
        return PIPELINE_TASK_KEY_PREFIX;
    }

}
