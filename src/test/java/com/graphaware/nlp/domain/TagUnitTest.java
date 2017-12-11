package com.graphaware.nlp.domain;

import org.junit.Test;

import static org.junit.Assert.*;

public class TagUnitTest {

    @Test
    public void testTagIsTrimmedOnConstruct() {
        Tag tag = new Tag(" Institute", "en");
        assertEquals("institute", tag.getLemma());
        assertEquals("institute_en", tag.getId());
    }
}
