package com.graphaware.nlp.dsl.result;

public class SingleResult {

    private static final String SUCCESS_RESULT = "SUCCESS";

    public final Object result;

    public SingleResult(Object result) {
        this.result = result;
    }

    public static SingleResult success() {
        return new SingleResult(SUCCESS_RESULT);
    }
}
