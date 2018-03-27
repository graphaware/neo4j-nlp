package com.graphaware.nlp.workflow;

public abstract class AbstractMessageHandler<E> implements MessageHandler<E> {
    private MessageHandler next;
    
    @Override
    public void setSuccessor(MessageHandler next) {
        this.next = next;
    }
    
    protected MessageHandler getSuccessor() {
        return next;
    }
    
    protected void checkAndHandle(Object entry) {
        if (next != null) {
            next.handle(entry);
        }
    }
    
    
}
