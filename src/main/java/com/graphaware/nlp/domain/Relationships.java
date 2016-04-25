package com.graphaware.nlp.domain;

import org.neo4j.graphdb.RelationshipType;

/**
 * All relationships used in the project.
 */
public enum Relationships implements RelationshipType {

    CONTAINS_SENTENCE,
    HAS_TAG,
    IS_RELATED_TO
}
