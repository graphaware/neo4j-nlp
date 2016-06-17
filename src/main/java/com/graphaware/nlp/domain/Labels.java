package com.graphaware.nlp.domain;

import org.neo4j.graphdb.Label;

/**
 * All labels used in the project.
 */
public enum Labels implements Label {
    AnnotatedText,
    Sentence,
    Tag,
    VeryNegative,
    Negative,
    Neutral,
    Positive,
    VeryPositive
}
