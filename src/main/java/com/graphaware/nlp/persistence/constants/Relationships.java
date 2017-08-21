/*
 * Copyright (c) 2013-2017 GraphAware
 *
 * This file is part of the GraphAware Framework.
 *
 * GraphAware Framework is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package com.graphaware.nlp.persistence.constants;

import org.neo4j.graphdb.RelationshipType;

/**
 * Default RelationshipType used in the persistence layer
 */
public enum Relationships implements RelationshipType {
    CONTAINS_SENTENCE,
    HAS_TAG,
    IS_RELATED_TO,
    SIMILARITY_COSINE,
    SIMILARITY_COSINE_CN5,
    FIRST_SENTENCE,
    NEXT_SENTENCE,
    HAS_PHRASE,
    SENTENCE_TAG_OCCURRENCE,
    TAG_OCCURRENCE_TAG,
    SENTENCE_PHRASE_OCCURRENCE,
    PHRASE_OCCURRENCE_PHRASE,
    REFER_TO,
    DESCRIBES,
    ROOT
}
