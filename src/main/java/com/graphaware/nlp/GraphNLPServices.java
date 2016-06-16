/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp;

import com.graphaware.nlp.conceptnet5.ConceptNet5Importer;
import com.graphaware.nlp.processor.TextProcessor;

/**
 *
 * @author ale
 */
public class GraphNLPServices {

    private TextProcessor textProcessor;
    private ConceptNet5Importer conceptnet5Importer;

    private GraphNLPServices() {
    }

    public static GraphNLPServices getInstance() {
        return GraphNLPServicesHolder.INSTANCE;
    }

    private static class GraphNLPServicesHolder {

        private static final GraphNLPServices INSTANCE = new GraphNLPServices();
    }

    //TODO init function or sprig autowire/config
    public TextProcessor getTextProcessor() {
        if (textProcessor == null) {
            textProcessor = new TextProcessor();
        }
        return textProcessor;
    }

    public ConceptNet5Importer getConceptNet5Importer() {
        if (conceptnet5Importer == null) {
            conceptnet5Importer = new ConceptNet5Importer.Builder("http://conceptnet5.media.mit.edu/data/5.4", getTextProcessor())
                    .build();
        }
        return conceptnet5Importer;
    }

}
