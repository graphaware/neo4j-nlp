package com.graphaware.nlp.workflow;

public interface MessageHandler<E> {
    public void handle(E entry);
    public void setSuccessor(MessageHandler next);
}
