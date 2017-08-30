package com.graphaware.nlp.dsl;

import com.graphaware.nlp.NLPIntegrationTest;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class LanguageProcedureTest extends NLPIntegrationTest {

    @Test
    public void testLanguageIsDetectedForEnglish() {
        executeInTransaction("CALL ga.nlp.detectLanguage('Mary was running in the park')", (result -> {
            assertTrue(result.hasNext());
            Map<String, Object> record = result.next();
            assertEquals("en", record.get("result"));
        }));
    }

    @Test
    public void testLanguageIsNotDetectedForNonText() {
        executeInTransaction("CALL ga.nlp.detectLanguage('com.graphaware.neo4j')", (result -> {
            assertTrue(result.hasNext());
            Map<String, Object> record = result.next();
            assertEquals("n/a", record.get("result"));
        }));
    }

}
