/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.workflow.processor;

import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.annotation.NLPProcessor;
import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.language.LanguageManager;
import com.graphaware.nlp.workflow.input.WorkflowInputEntry;
import com.graphaware.nlp.processor.TextProcessor;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ale
 */
@NLPProcessor(name = "PipelineTextProcessor")
public class WorkflowTextProcessor extends WorkflowProcessor<WorkflowTextProcessorConfiguration> {

    private static final Logger LOG = LoggerFactory.getLogger(WorkflowTextProcessor.class);

    private TextProcessor textProcessor;

    public WorkflowTextProcessor(String name, GraphDatabaseService database) {
        super(name, database);
        //Preinitialize
        LanguageManager.getInstance().initialize();
    }

    @Override
    public WorkflowProcessorOutputEntry process(WorkflowInputEntry entry) {
        if (isValid()) {
            long start = -System.currentTimeMillis();
            String lang = NLPManager.getInstance().checkTextLanguage(entry.getText(), getConfiguration().checkLanguage());
            System.out.println("Time for getting lang: " + (System.currentTimeMillis() + start));
            AnnotatedText annotateText = textProcessor.annotateText(entry.getText(), getConfiguration().getPipeline(), lang, null);
            return new WorkflowProcessorOutputEntry(annotateText, entry.getId());
        } else {
            LOG.warn("The Processor " + this.getName()+ " is in an invalid state");
            return null;
        }
    }

    @Override
    public void init(Map<String, Object> parameters) {
        setConfiguration(new WorkflowTextProcessorConfiguration(parameters));
        try {
            textProcessor = NLPManager.getInstance()
                    .getTextProcessorsManager()
                    .retrieveTextProcessor(getConfiguration().getTextProcessor(), getConfiguration().getPipeline());
            setValid(true);
        } catch (Exception ex) {
            LOG.error("Error while initializing the processor. Setting to invalid", ex);
            setValid(false);
        }
    }

}
