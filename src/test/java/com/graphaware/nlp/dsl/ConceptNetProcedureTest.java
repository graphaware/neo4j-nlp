package com.graphaware.nlp.dsl;

import com.graphaware.nlp.NLPIntegrationTest;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ConceptNetProcedureTest extends NLPIntegrationTest {

    @Test
    public void testConceptProcedureWithAnnotatedNode() {
        executeInTransaction("CALL ga.nlp.annotate({text:'John and Adam planned to kill the cat', id: '123', textProcessor:'stub'})", (result -> {
            assertTrue(result.hasNext());
        }));

        executeInTransaction("MATCH (n:AnnotatedText) CALL ga.nlp.enrich.concept({node: n, depth: 1, language:'en'}) YIELD result RETURN result", (result -> {
            assertTrue(result.hasNext());
        }));
    }
}
