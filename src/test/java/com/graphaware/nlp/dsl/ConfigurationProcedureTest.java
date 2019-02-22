package com.graphaware.nlp.dsl;

import com.graphaware.nlp.NLPIntegrationTest;
import org.junit.Test;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;

import java.util.Collections;
import java.util.HashMap;
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

    @Test
    public void testRemoveConfigValueViaProcedure() {
        executeInTransaction("CALL ga.nlp.config.set('LABEL_Tag', 'Token')", (result -> {
            assertTrue(result.hasNext());
            assertEquals("SUCCESS", result.next().get("result"));
            assertEquals("Token", getNLPManager().getConfiguration().getLabelFor(Label.label("Tag")).toString());
        }));
        assertTrue(getNLPManager().getConfiguration().hasStoreValue("LABEL_Tag"));
        executeInTransaction("CALL ga.nlp.config.remove('LABEL_Tag')", (result -> {
            assertTrue(result.hasNext());
        }));
        assertFalse(getNLPManager().getConfiguration().hasStoreValue("LABEL_Tag"));
    }

    @Test
    public void testSetMultipleConfigurationValuesViaProcedure() {
        Map<String, Object> map = new HashMap<>();
        map.put("LABEL_Sentence", "NLPSentence");
        map.put("SETTING_CONCEPT_NET_URL", "http://demoserver:8000");
        map.put("RELATIONSHIP_TAG_OCCURRENCE_TAG", "TOT");

        executeInTransaction("CALL ga.nlp.config.setAll({config})", Collections.singletonMap("config", map), (result -> {
            assertTrue(result.hasNext());
            assertEquals("SUCCESS", result.next().get("result"));
            assertEquals("http://demoserver:8000", getNLPManager().getConfiguration().getSettingValueFor("CONCEPT_NET_URL"));
            assertEquals("TOT", getNLPManager().getConfiguration().getRelationshipFor(RelationshipType.withName("TAG_OCCURRENCE_TAG")).toString());
        }));
    }

    @Test
    public void testSettingDefaultModelWorkdir() {
        String path = getClass().getClassLoader().getResource("").getPath();
        executeInTransaction("CALL ga.nlp.config.model.workdir({p0})", buildSeqParameters(path), emptyConsumer());
        assertEquals(path, getNLPManager().getDefaultModelWorkdir());
        assertTrue(getNLPManager().hasDefaultModelWorkdir());
    }

    @Test
    public void testListModelsWithProcedure() {
        String path = getClass().getClassLoader().getResource("").getPath();
        executeInTransaction("CALL ga.nlp.config.model.add('hello', $p0)", buildSeqParameters(path), emptyConsumer());

        executeInTransaction("CALL ga.nlp.config.model.list", (result -> {
            assertTrue(result.hasNext());
            while (result.hasNext()) {
                assertTrue(result.next().get("value").toString().equals(path));
            }
        }));
    }

    @Test
    public void testSettingDefaultLanguage() {
        executeInTransaction("CALL ga.nlp.config.setDefaultLanguage('en')", emptyConsumer());
        assertEquals("en", getNLPManager().getConfiguration().getSettingValueFor("fallbackLanguage").toString());
    }

    @Test
    public void testDefaultLanguageShouldMapFullLanguageNameToAbbreviation() {
        executeInTransaction("CALL ga.nlp.config.setDefaultLanguage('English')", emptyConsumer());
        assertEquals("en", getNLPManager().getConfiguration().getSettingValueFor("fallbackLanguage").toString());

        executeInTransaction("CALL ga.nlp.config.setDefaultLanguage('german')", emptyConsumer());
        assertEquals("de", getNLPManager().getConfiguration().getSettingValueFor("fallbackLanguage").toString());
    }
}
