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

import org.neo4j.graphdb.Label;

import java.util.Arrays;

/**
 * Default Node labels used in the persistence layer
 */
public enum Labels implements Label {
    AnnotatedText,
    Sentence,
    Tag,
    Phrase,
    PhraseOccurrence,
    TagOccurrence,
    Keyword,
    Root,
    Pipeline,
    Positive,
    Negative,
    VeryPositive,
    VeryNegative,
    Neutral,
    VectorContainer;


    public static boolean contains(String label) {
        for (Labels l : Labels.values()) {
            if (l.name().equals(label)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isSentimentLabel(String label) {
        return Arrays.asList(
                VeryNegative.name(),
                Negative.name(),
                Neutral.name(),
                Positive.name(),
                VeryPositive.name()
        ).contains(label);
    }
}
