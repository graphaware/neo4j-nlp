package com.graphaware.nlp.util;

public class TokenUtils {

    private static final String DELIMITER = "_";

    public static String buildTokenId(int startPosition, int endPosition, String lemma) {
        return startPosition + DELIMITER + endPosition + DELIMITER + lemma;
    }
}
