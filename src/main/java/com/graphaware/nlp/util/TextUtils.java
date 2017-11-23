package com.graphaware.nlp.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtils {

    private static String REGEX = "\\(..\\)$";
    private static String REPLACE = "";
    private static Pattern pattern = Pattern.compile(REGEX);

    public static String removeApices(String text) {
        if (text != null) {
            return text.replace("\"", "");
        }
        return null;
    }

    public static String removeParenthesis(String concept) {
        Matcher m = pattern.matcher(concept);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, REPLACE);
        }
        m.appendTail(sb);
        return sb.toString();
    }

}
