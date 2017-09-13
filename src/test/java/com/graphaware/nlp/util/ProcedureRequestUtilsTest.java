package com.graphaware.nlp.util;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class ProcedureRequestUtilsTest {

    @Test
    public void testPossibleValueIsReturned() {
        assertEquals("admittedRelationships", ProcedureRequestUtils.hasPossibleRequestKey("admittedRelationship",
                Collections.singletonList("admittedRelationships")));
        assertNull(ProcedureRequestUtils.hasPossibleRequestKey("check", Collections.singletonList("checkLanguage")));
    }

    @Test
    public void testItWorksWithLowercaseUppercaseHandling() {
        assertEquals("admittedRelationships", ProcedureRequestUtils.hasPossibleRequestKey("admittedrelationships", Collections.singletonList("admittedRelationships")));
    }

}
