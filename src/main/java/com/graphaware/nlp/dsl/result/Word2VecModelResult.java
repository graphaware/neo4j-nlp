package com.graphaware.nlp.dsl.result;

public class Word2VecModelResult {

    public String name;

    public String path;

    public long indexCount;

    public Word2VecModelResult(String name, String path, long indexCount) {
        this.name = name;
        this.path = path;
        this.indexCount = indexCount;
    }
}
