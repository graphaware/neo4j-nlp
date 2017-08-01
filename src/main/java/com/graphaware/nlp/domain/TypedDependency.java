package com.graphaware.nlp.domain;

public class TypedDependency {

    private final String source;

    private final String target;

    private final String name;

    private final String specific;

    public TypedDependency(String source, String target, String name, String specific) {
        this.source = source;
        this.target = target;
        this.name = name;
        this.specific = specific;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public String getName() {
        return name;
    }

    public String getSpecific() {
        return specific;
    }
}
