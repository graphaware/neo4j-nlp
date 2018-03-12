/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.pipeline.processor;

import com.graphaware.nlp.domain.AnnotatedText;

/**
 *
 * @author ale
 */
public class PipelineProcessorOutputEntry {
    private final AnnotatedText annotateText;
    private final Object id;

    public PipelineProcessorOutputEntry(AnnotatedText annotateText, Object id) {
        this.annotateText = annotateText;
        this.id = id;
    }

    public AnnotatedText getAnnotateText() {
        return annotateText;
    }

    public Object getId() {
        return id;
    }
}
