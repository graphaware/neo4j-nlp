/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.workflow.processor;

import com.graphaware.common.log.LoggerFactory;
import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.annotation.NLPProcessor;
import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.dsl.request.PipelineSpecification;
import com.graphaware.nlp.language.LanguageManager;
import com.graphaware.nlp.workflow.input.WorkflowInputEntry;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.workflow.input.WorkflowInputEndOfQueueEntry;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.logging.Log;

@NLPProcessor(name = "PipelineTextProcessor")
public class WorkflowTextProcessor extends WorkflowProcessor<WorkflowTextProcessorConfiguration> {

    private static final Log LOG = LoggerFactory.getLogger(WorkflowTextProcessor.class);

    private TextProcessor textProcessor;

    public WorkflowTextProcessor(String name, GraphDatabaseService database) {
        super(name, database);
    }

    @Override
    public void handle(WorkflowInputEntry entry) {
        if (entry instanceof WorkflowInputEndOfQueueEntry) {
            super.checkAndHandle(new WorkflowProcessorEndOfQueueEntry());
            return;
        }
        if (isValid()) {
            AnnotatedText annotateText = NLPManager.getInstance().getTextProcessorsManager().annotate(entry.getText(), getConfiguration().getPipeline());
            super.checkAndHandle(new WorkflowProcessorOutputEntry(annotateText, entry.getId()));
        } else {
            LOG.warn("The Processor " + this.getName() + " is in an invalid state");
            return;
        }
    }

    @Override
    public void init(Map<String, Object> parameters) {
        setConfiguration(new WorkflowTextProcessorConfiguration(parameters));
        PipelineSpecification pipelineSpecification = NLPManager.getInstance().getTextProcessorsManager().getPipelineSpecification(getConfiguration().getPipeline());
        if (null != pipelineSpecification) {
            textProcessor = NLPManager.getInstance().getTextProcessorsManager().getTextProcessor(pipelineSpecification.getTextProcessor());
            setValid(true);
        } else {
            LOG.error("Error while initializing the processor. Setting to invalid");
            setValid(false);
        }
    }

    @Override
    public void stop() {

    }

}
