/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.workflow.output;

import com.graphaware.nlp.annotation.NLPOutput;
import com.graphaware.nlp.workflow.processor.WorkflowProcessorOutputEntry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NLPOutput(name = "DeferredStoreAnnotatedTextWorkflowOutput")
public class DeferredStoreAnnotatedTextWorkflowOutput extends StoreAnnotatedTextWorkflowOutput {

    private static final Logger LOG = LoggerFactory.getLogger(DeferredStoreAnnotatedTextWorkflowOutput.class);

    private final BlockingQueue<WorkflowProcessorOutputEntry> queue;
    private final StoreThread backgroundThread;

    public DeferredStoreAnnotatedTextWorkflowOutput(String name, GraphDatabaseService database) {
        super(name, database);
        this.queue = new LinkedBlockingQueue<>();
        this.backgroundThread = new StoreThread(this);
        backgroundThread.start();
    }

    @Override
    public void handle(WorkflowProcessorOutputEntry entry) {
        queue.offer(entry);
    }

    class StoreThread extends Thread {

        private final DeferredStoreAnnotatedTextWorkflowOutput store;

        public StoreThread(DeferredStoreAnnotatedTextWorkflowOutput store) {
            this.store = store;
        }

        @Override
        public void run() {
            try {
                WorkflowProcessorOutputEntry entry = queue.take();
                store.handle(entry);

            } catch (InterruptedException ex) {
                LOG.warn("Interrupted excetpion", ex);
            }
        }

    }
}
