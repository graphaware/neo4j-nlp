package com.graphaware.nlp.dsl;

public class AnnotationRequest {

    private String text;

    private Object id;

    private String textProcessor;

    private String pipeline;

    private boolean force;

    private boolean checkLanguage = true;

    public AnnotationRequest() {

    }

    public AnnotationRequest(String text, Object id, String textProcessor, String pipeline, boolean force, boolean checkLanguage) {
        this.text = text;
        this.id = id;
        this.textProcessor = textProcessor;
        this.pipeline = pipeline;
        this.force = force;
        this.checkLanguage = checkLanguage;
    }

    public String getText() {
        return text;
    }

    public String getId() {
        return String.valueOf(id);
    }

    public String getTextProcessor() {
        return textProcessor;
    }

    public String getPipeline() {
        return pipeline;
    }

    public boolean isForce() {
        return force;
    }

    public boolean isCheckLanguage() {
        return checkLanguage;
    }

    public boolean shouldCheckLanguage() {
        return checkLanguage;
    }
}
