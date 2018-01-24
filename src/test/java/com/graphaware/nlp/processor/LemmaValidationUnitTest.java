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
        //Det är idag kallt. Året är 2018
        assertTrue(match("är"));
        assertTrue(match("Året"));
        assertTrue(match("Året-är"));
        assertTrue(match("générale"));
        assertTrue(match("deçà"));
        assertTrue(match("vôtre"));
        assertTrue(match("aujourd'hui"));
        assertTrue(match("Dörfer"));
        assertTrue(match("Ausführung"));
        assertTrue(match("Maßstab"));

    }

    private boolean match(String value) {
        Matcher match = patternCheck.matcher(value);
        return match.find();
    }
}
