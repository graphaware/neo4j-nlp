package com.graphaware.nlp.integration;

import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.module.NLPConfiguration;
import com.graphaware.nlp.persistence.Labels;
import com.graphaware.nlp.persistence.Relationships;
import com.graphaware.nlp.processor.PipelineSpecification;
import com.graphaware.nlp.stub.StubTextProcessor;
import com.graphaware.nlp.util.TestNLPGraph;
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

    @Test
    public void testMultipleSentencesPersistence() {
        NLPManager manager = new NLPManager(getDatabase(), NLPConfiguration.defaultConfiguration());
        PipelineSpecification specification = new PipelineSpecification("tokenizer", StubTextProcessor.class.getName());
        try (Transaction tx = getDatabase().beginTx()) {
            manager.annotateTextAndPersist(
                    "hello my name is John. I am working for IBM. I live in Italy",
                    "123",
                    specification,
                    false);
            tx.success();
        }

        TestNLPGraph testNLPGraph = new TestNLPGraph(getDatabase());
        testNLPGraph.assertAnnotatedTextNodesCount(1);
        testNLPGraph.assertSentenceNodesCount(3);
        testNLPGraph.assertTagNodesCount(14);
        testNLPGraph.assertTagWithValueExist("hello");
        testNLPGraph.assertTagWithValueExist("IBM");
        testNLPGraph.assertTagWithValueHasNERLabel("name", "NER_Test");
        testNLPGraph.assertPhraseWithTextExist("hello my name is John");
        testNLPGraph.assertPhraseOccurrenceForTextHasStartAndEndPosition("hello my name is John", 0, "hello my name is John".length());
        testNLPGraph.assertSentenceWithIdHasPhraseOccurrenceCount("123_0", 1);
        testNLPGraph.assertSentenceWithIdHasPhraseWithText("123_0", "hello my name is John");
    }

    private void clearDatabase() {
        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().execute("MATCH (n) DETACH DELETE n");
            tx.success();
        }
    }

}
