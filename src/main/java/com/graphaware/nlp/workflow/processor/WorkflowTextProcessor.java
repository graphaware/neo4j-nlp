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
        //Preinitialize
        LanguageManager.getInstance().initialize();
    }

    @Override
    public void handle(WorkflowInputEntry entry) {
        if (entry instanceof WorkflowInputEndOfQueueEntry) {
            super.checkAndHandle(new WorkflowProcessorEndOfQueueEntry());
        }
        if (isValid()) {
            long start = -System.currentTimeMillis();
            String lang = NLPManager.getInstance().checkTextLanguage(entry.getText(), getConfiguration().checkLanguage());
            System.out.println("Time for getting lang: " + (System.currentTimeMillis() + start));
            String pipeline = NLPManager.getInstance().getPipeline(getConfiguration().getPipeline());
            PipelineSpecification pipelineSpecification = NLPManager.getInstance().getConfiguration().loadPipeline(pipeline);
            if (null == pipelineSpecification) {
                throw new RuntimeException("No pipeline " + pipeline);
            }
            AnnotatedText annotateText = textProcessor.annotateText(entry.getText(), lang, pipelineSpecification);
            super.checkAndHandle(new WorkflowProcessorOutputEntry(annotateText, entry.getId()));
        } else {
            LOG.warn("The Processor " + this.getName()+ " is in an invalid state");
            super.checkAndHandle(null);
        }
    }

    @Override
    public void init(Map<String, Object> parameters) {
        setConfiguration(new WorkflowTextProcessorConfiguration(parameters));
        String pipeline = NLPManager.getInstance().getPipeline(getConfiguration().getPipeline());
        PipelineSpecification pipelineSpecification = NLPManager.getInstance().getConfiguration().loadPipeline(pipeline);
        if (null != pipelineSpecification) {
            textProcessor = NLPManager.getInstance().getTextProcessorsManager().getTextProcessor(pipelineSpecification.getTextProcessor());
            setValid(true);
        } else {
            LOG.error("Error while initializing the processor. Setting to invalid");
            setValid(false);
        }
    }

}
