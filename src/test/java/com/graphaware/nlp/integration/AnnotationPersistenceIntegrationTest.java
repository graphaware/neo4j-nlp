package com.graphaware.nlp.integration;

import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.module.NLPConfiguration;
import com.graphaware.nlp.persistence.Labels;
import com.graphaware.nlp.persistence.Relationships;
import com.graphaware.nlp.processor.PipelineSpecification;
import com.graphaware.nlp.stub.StubTextProcessor;
import com.graphaware.test.integration.EmbeddedDatabaseIntegrationTest;
import org.junit.Test;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import static org.junit.Assert.*;

public class AnnotationPersistenceIntegrationTest extends EmbeddedDatabaseIntegrationTest {


    @Test
    public void testAnnotatedTextIsPersisted() {
        NLPManager manager = new NLPManager(getDatabase(), NLPConfiguration.defaultConfiguration());
        PipelineSpecification specification = new PipelineSpecification("tokenizer", StubTextProcessor.class.getName());

        try (Transaction tx = getDatabase().beginTx()) {
            Node annotatedText = manager.annotateTextAndPersist(
                    "hello my name is John.",
                    "123",
                    specification,
                    false);
            assertEquals("123", annotatedText.getProperty("id").toString());
            assertTrue(annotatedText.hasLabel(Labels.AnnotatedText));
            assertTrue(annotatedText.hasRelationship(Relationships.CONTAINS_SENTENCE));
            assertTrue(annotatedText.hasRelationship(Relationships.FIRST_SENTENCE));
            tx.success();
        }
    }

    @Test
    public void testAnnotatedTextIsPersistedWithCustomLabel() {
        NLPManager manager = new NLPManager(getDatabase(), NLPConfiguration.defaultConfiguration());
        PipelineSpecification specification = new PipelineSpecification("tokenizer", StubTextProcessor.class.getName());
        manager.updateConfigurationSetting(Labels.AnnotatedText.toString(), "TextAnnotation");
        try (Transaction tx = getDatabase().beginTx()) {
            Node annotatedText = manager.annotateTextAndPersist(
                    "hello my name is John.",
                    "123",
                    specification,
                    false);
            assertEquals("123", annotatedText.getProperty("id").toString());
            assertTrue(annotatedText.hasLabel(Label.label("TextAnnotation")));
            tx.success();
        }
    }

}
