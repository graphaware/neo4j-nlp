/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.workflow.processor;

import com.graphaware.nlp.annotation.NLPProcessor;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NLPProcessor(name = "PipelineTextProcessor")
public class WorkflowParallelTextProcessor extends WorkflowTextProcessor {
    
    private final BlockingQueue<WorkflowProcessorOutputEntry> queue;
    private static final Logger LOG = LoggerFactory.getLogger(WorkflowParallelTextProcessor.class);
    
    public WorkflowParallelTextProcessor(String name, GraphDatabaseService database) {
        super(name, database);
        this.queue = new LinkedBlockingQueue<>();
    }
    
    
    
    
    
}
