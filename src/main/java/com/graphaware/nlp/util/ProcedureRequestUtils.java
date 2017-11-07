package com.graphaware.nlp.util;


import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class ProcedureRequestUtils {

    public static String hasPossibleRequestKey(String given, List<String> configKeys) {
        if (configKeys.contains(given)) {
            return null;
        }

        for (String s : configKeys) {
            int distance = StringUtils.getLevenshteinDistance(given, s);
            if (distance != 0 && distance < 3) {
                return s;
            }
        }

        return null;
    }
}
