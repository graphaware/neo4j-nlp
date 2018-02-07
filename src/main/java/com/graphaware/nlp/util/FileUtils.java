package com.graphaware.nlp.util;

import java.net.URI;

public class FileUtils {

    public static String getFileUri(String path) {
        if (path.startsWith("file:///")) {
            return (path.replaceAll("file:///", ""));
        }

        return path;
    }
}
