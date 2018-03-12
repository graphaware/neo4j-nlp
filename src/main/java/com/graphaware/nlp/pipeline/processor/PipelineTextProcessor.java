/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.pipeline.processor;

import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.annotation.NLPProcessor;
import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.pipeline.input.PipelineInputEntry;
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
public class PipelineTextProcessor extends PipelineProcessor<PipelineTextProcessorConfiguration> {

    private static final Logger LOG = LoggerFactory.getLogger(PipelineTextProcessor.class);

    private TextProcessor textProcessor;

    public PipelineTextProcessor(String name, GraphDatabaseService database) {
        super(name, database);
    }

    @Override
    public PipelineProcessorOutputEntry process(PipelineInputEntry entry) {
        if (isValid()) {
            String lang = NLPManager.getInstance().checkTextLanguage(entry.getText(), getConfiguration().checkLanguage());
            AnnotatedText annotateText = textProcessor.annotateText(entry.getText(), getConfiguration().getPipeline(), lang, null);
            return new PipelineProcessorOutputEntry(annotateText, entry.getId());
        } else {
            LOG.warn("The Processor " + this.getName()+ " is in an invalid state");
            return null;
        }
    }

    @Override
    public void init(Map<String, Object> parameters) {
        setConfiguration(new PipelineTextProcessorConfiguration(parameters));
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
