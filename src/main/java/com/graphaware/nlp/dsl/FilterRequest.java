/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.dsl;

/**
 *
 * @author ale
 */
public class FilterRequest {
    
    private String text;
    private String filter;
    private String processor;
    private String pipeline;

    public FilterRequest() {
    }

    
    public FilterRequest(String text, String filter, String processor, String pipeline) {
        this.text = text;
        this.filter = filter;
        this.processor = processor;
        this.pipeline = pipeline;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getProcessor() {
        return processor;
    }

    public void setProcessor(String processor) {
        this.processor = processor;
    }

    public String getPipeline() {
        return pipeline;
    }

    public void setPipeline(String pipeline) {
        this.pipeline = pipeline;
    }
    
    
    
}
