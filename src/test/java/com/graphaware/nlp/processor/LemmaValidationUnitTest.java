package com.graphaware.nlp.processor;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class LemmaValidationUnitTest {

    private final Pattern patternCheck = Pattern.compile(AbstractTextProcessor.PUNCT_REGEX_PATTERN, Pattern.CASE_INSENSITIVE);

    @Test
    public void testLemmaValidation() {
        assertTrue(match("hello"));
        assertTrue(match("play-off"));
        assertTrue(match("play-by-play"));
        assertTrue(match("vd-810-mil"));
        assertFalse(match("("));
        assertFalse(match("-"));
        assertFalse(match("/"));
        assertFalse(match(","));
        assertFalse(match(";"));
        assertFalse(match("-lrb-"));
        assertTrue(match("MIL-A-8625"));
    }

    private boolean match(String value) {
        Matcher match = patternCheck.matcher(value);
        return match.find();
    }
}
