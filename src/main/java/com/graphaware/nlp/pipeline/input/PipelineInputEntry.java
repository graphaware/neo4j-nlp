/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.pipeline.input;

public class PipelineInputEntry<T> {
    private final String text;
    private final T id;

    public PipelineInputEntry(String text, T id) {
        this.text = text;
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public T getId() {
        return id;
    }
    
    
}
