package com.graphaware.nlp.event;

import com.graphaware.tx.event.improved.api.ImprovedTransactionData;

public class DatabaseTransactionEvent implements Event {

    private final ImprovedTransactionData transactionData;

    public DatabaseTransactionEvent(ImprovedTransactionData transactionData) {
        this.transactionData = transactionData;
    }

    public ImprovedTransactionData getTransactionData() {
        return transactionData;
    }
}
