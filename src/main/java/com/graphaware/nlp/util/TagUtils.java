package com.graphaware.nlp.util;

public class TagUtils {

    public static String getNamedEntityValue(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }
}
