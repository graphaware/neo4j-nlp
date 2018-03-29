package com.graphaware.nlp.util;

import java.net.URI;

public class FileUtils {

    public static String getFileUri(String path) {
        if (path.startsWith("file:///")) {
            return (path.replaceAll("file:///", ""));
        }

        return path;
    }

    public static String resolveFilePath(String rootPath, String file) {
        return !rootPath.endsWith("/") ? rootPath + "/" + file : rootPath + file;
    }
}
