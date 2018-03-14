/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.workflow.input;

public class WorkflowInputEntry<T> {
    private final String text;
    private final T id;

    public WorkflowInputEntry(String text, T id) {
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
