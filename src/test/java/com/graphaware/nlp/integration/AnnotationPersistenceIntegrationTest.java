package com.graphaware.nlp.integration;

import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.module.NLPConfiguration;
import com.graphaware.nlp.persistence.Labels;
import com.graphaware.nlp.persistence.Relationships;
import com.graphaware.nlp.processor.PipelineSpecification;
import com.graphaware.nlp.stub.StubTextProcessor;
import com.graphaware.test.integration.EmbeddedDatabaseIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.util.Iterator;

import static org.junit.Assert.*;

public class AnnotationPersistenceIntegrationTest extends EmbeddedDatabaseIntegrationTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        clearDatabase();
    }

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

            assertEquals(1L, getDatabase().execute("MATCH (n:Sentence) RETURN count(n) AS c").next().get("c"));

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

    @Test
    public void testNamedEntityLabelIsAdded() {
        NLPManager manager = new NLPManager(getDatabase(), NLPConfiguration.defaultConfiguration());
        PipelineSpecification specification = new PipelineSpecification("tokenizer", StubTextProcessor.class.getName());
        manager.updateConfigurationSetting(Labels.AnnotatedText.toString(), "TextAnnotation");
        try (Transaction tx = getDatabase().beginTx()) {
            Node annotatedText = manager.annotateTextAndPersist(
                    "hello my name is John.",
                    "123",
                    specification,
                    false);

            Iterator<Node> it = getDatabase().findNodes(Label.label("Tag"));
            assertTrue(it.hasNext());
            while (it.hasNext()) {
                Node node = it.next();
                assertTrue(node.hasLabel(Label.label("NER_Test")));
            }
            tx.success();
        }
    }

    private void clearDatabase() {
        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().execute("MATCH (n) DETACH DELETE n");
            tx.success();
        }
    }

}
