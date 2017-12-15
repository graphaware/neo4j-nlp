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

/**
 * All node property keys used in the persistence layer.
 */
public final class Properties {

    public static final String PROPERTY_ID = "id";
    public static final String TIMESTAMP = "__timestamp";
    public static final String SIMILARITY_VALUE = "value";
    public static final String PROCESS_TYPE = "processType";
    public static final String PROCESS_STATUS = "processStatus";
    public static final String HASH = "hash";
    public static final String TEXT = "text";
    public static final String NUM_TERMS = "numTerms";
    public static final String SENTENCE_NUMBER = "sentenceNumber";
    public static final String START_POSITION = "startPosition";
    public static final String END_POSITION = "endPosition";
    public static final String CONTENT_VALUE = "value";
    public static final String TAG_ORIGINAL_VALUE = "value";
    public static final String PHRASE_TYPE = "type";
    public static final String LANGUAGE = "language";
    public static final String TF = "tf";
    public static final String OCCURRENCE_BEGIN = "startPosition";
    public static final String OCCURRENCE_END = "endPosition";
    public static final String DEPENDENCY_SPECIFIC = "specific";
    public static final String NAMED_ENTITY = "ne";
    public static final String NAMED_ENTITY_PREFIX = "NER_";
    public static final String PART_OF_SPEECH = "pos";
    public static final String LAST_TX_ID = "lastTxId";

    /**
     * Private constructor to prevent people from instantiating this class - it's not meant to be instantiated.
     */
    private Properties() {
    }
}
