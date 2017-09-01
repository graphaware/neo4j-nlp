package com.graphaware.nlp.event;

public class DefaultEvent implements Event {

    private final String value;

    public DefaultEvent(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
