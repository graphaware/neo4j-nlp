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
    public static final String PARAMETER_WEIGHT_SESSION = "weightSession";
    public static final String PARAMETER_WEIGHT_FEATURE = "weightFeature";
    

    /**
     * Private constructor to prevent people from instantiating this class -
     * it's not meant to be instantiated.
     */
    private Constants() {
    }
}
