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
package com.graphaware.nlp.domain;

/**
 * All constants used in the project.
 */
public final class Constants {

    public static final String UNKNOWN = "unknown";
    public static final String SESSION_SIMILARITY = "sessionSimilarity";
    public static final String FEATURE_SIMILARITY = "featureSimilarity";

    public static final long WINDOW_SIZE = 90l * 24l * 60l * 60l * 1000l;
    public static final long UPDATE_INTERVALL = 30l * 60l * 1000l;
    public static final float DECAY_VALUE_FOR_SESSION_NUMBER = 3.0f;
    public static final int KNN_SIZE = 2000;

    //
    public static final String PROCESS_STATUS_STOPPED = "STOPPED";
    public static final String PROCESS_STATUS_STARTED = "STARTED";
    public static final String PROCESS_STATUS_FAILED = "FAILED";
    //
    public static final String PARAMETER_DEPTH = "depth";
    //    
    public static final String LANGUAGE_NA = "n/a";
    public static final String LANGUAGE_EN = "en";
    
    public static final String VECTOR_PROPERTY = "vector";

    private Constants() {
    }
}
