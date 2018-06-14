package com.graphaware.nlp.dsl.request;

public class Word2VecModelSpecification {
    private String sourcePath;
    private String destinationPath;
    private String modelName;
    private String language;

    public Word2VecModelSpecification() {
    }

    public Word2VecModelSpecification(String sourcePath, String destinationPath, String modelName, String language) {
        this.sourcePath = sourcePath;
        this.destinationPath = destinationPath;
        this.modelName = modelName;
        this.language = language;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
