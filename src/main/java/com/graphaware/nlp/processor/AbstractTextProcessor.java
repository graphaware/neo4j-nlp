/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.processor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author ale
 */
public abstract class AbstractTextProcessor implements TextProcessor{
    //    private static final String PUNCT_REGEX_PATTERN = "\\p{Punct}";
    protected static final String PUNCT_REGEX_PATTERN = "^([a-z0-9]+)([-_][a-z0-9]+)?$";
    private final Pattern patternCheck;
    public AbstractTextProcessor() {
        patternCheck = Pattern.compile(PUNCT_REGEX_PATTERN, Pattern.CASE_INSENSITIVE);
    }

    @Override
    public boolean checkLemmaIsValid(String value) {
        Matcher match = patternCheck.matcher(value);
        return match.find();
    }
}
