package com.graphaware.nlp.dsl;

import com.graphaware.nlp.NLPIntegrationTest;
import org.junit.Test;
import org.neo4j.graphdb.Label;

import java.util.Map;

import static org.junit.Assert.*;

public class ConfigurationProcedureTest extends NLPIntegrationTest {

    @Test
    public void testShowConfigurationProcedure() {
        executeInTransaction("CALL ga.nlp.config.show", (result -> {
            assertFalse(result.hasNext());
        }));
    }

    @Test
    public void testShowConfigProcedureReturnsResultWhenStoreIsNotEmpty() {
        getNLPManager().getConfiguration().update("LABEL_AnnotatedText", "TextAnnotation");
        executeInTransaction("CALL ga.nlp.config.show", (result -> {
            assertTrue(result.hasNext());
            Map<String, Object> record = result.next();
            assertEquals("TextAnnotation", record.get("value"));
            assertEquals("LABEL_AnnotatedText", record.get("key"));
        }));
    }

    @Test
    public void testSetConfigValueViaProcedure() {
        executeInTransaction("CALL ga.nlp.config.set('LABEL_Tag', 'Token')", (result -> {
            assertTrue(result.hasNext());
            assertEquals("SUCCESS", result.next().get("result"));
            assertEquals("Token", getNLPManager().getConfiguration().getLabelFor(Label.label("Tag")).toString());
        }));
    }

}
