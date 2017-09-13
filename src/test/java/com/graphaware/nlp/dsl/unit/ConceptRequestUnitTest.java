package com.graphaware.nlp.dsl.unit;

import com.graphaware.nlp.dsl.request.ConceptRequest;
import com.graphaware.nlp.util.NodeProxy;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ConceptRequestUnitTest {

    @Test
    public void testConceptRequestFromMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("tag", new NodeProxy());
        map.put("admittedRelationships", Arrays.asList("IsA","PartOf"));
        map.put("admittedPos", Arrays.asList("NNS","NN"));
        map.put("language", "en");
        map.put("splitTag", true);

        ConceptRequest request = ConceptRequest.fromMap(map);
        assertEquals(2, request.getAdmittedRelationships().size());
        assertTrue(request.getAdmittedRelationships().contains("IsA"));
        assertEquals(2, request.getAdmittedPos().size());
        assertEquals("en", request.getLanguage());
    }

    @Test(expected = RuntimeException.class)
    public void testRequestThrowsFriendlyMessageWithAlmostGoodConfigKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("node", new NodeProxy());
        map.put("language", "en");
        map.put("admittedRelationship", new ArrayList<>());
        ConceptRequest request = ConceptRequest.fromMap(map);
    }
}
