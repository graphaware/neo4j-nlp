package com.graphaware.nlp.util;

import java.util.Arrays;
import java.util.List;

public class ImportUtils {

    public static List<String> getImportQueriesFromApocExport(String content) {
        String[] parts = content.split(";\n");
        return Arrays.asList(parts);
    }

}
