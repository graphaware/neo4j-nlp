package com.graphaware.nlp.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TextUtilsTest {

    @Test
    public void testTextReplaceEach() {
        String original = "● From Belgium but living in Southern Italy ■ Information Extraction";
        List<String> searches = Arrays.asList("●","■");

        String processed = TextUtils.replaceEach(searches, original);
        assertEquals(" From Belgium but living in Southern Italy  Information Extraction", processed);
    }
}
