package com.graphaware.nlp;

import com.graphaware.nlp.event.Events;

public enum NLPEvents implements Events {
    TRANSACTION_BEFORE_COMMIT,
    PRE_TEXT_ANNOTATION,
    POST_TEXT_ANNOTATION
}
