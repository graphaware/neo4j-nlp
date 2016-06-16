package com.graphaware.nlp.domain;

/**
 * All node properties used in the project.
 */
public final class Properties {

    public static final String NAME = "name";
    public static final String UUID = "uuid";
    public static final String SESSION_ID = "sessionId";
    public static final String PROPERTY_ID = "id";
    public static final String USER_ID = "userId";
    public static final String COOKIE_ID = "cookieId";
    public static final String TIMESTAMP = "__timestamp";
    public static final String SIMILARITY_VALUE = "value";
    public static final String PROCESS_TYPE = "processType";
    public static final String PROCESS_STATUS = "processStatus";
    public static final String HASH = "hash";

    /**
     * Private constructor to prevent people from instantiating this class - it's not meant to be instantiated.
     */
    private Properties() {
    }
}
