package com.graphaware.nlp.integration;

import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.configuration.SettingsConstants;
import com.graphaware.nlp.domain.SentimentLabels;
import com.graphaware.nlp.dsl.request.PipelineSpecification;
import com.graphaware.nlp.module.NLPConfiguration;
import com.graphaware.nlp.persistence.constants.Labels;
import com.graphaware.nlp.persistence.constants.Relationships;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.stub.StubTextProcessor;
import com.graphaware.nlp.util.TestNLPGraph;
import java.lang.reflect.Field;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.*;

public class AnnotationPersistenceIntegrationTest extends NLPIntegrationTest {

    private static NLPManager manager;


    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        clearDatabase();
        manager = NLPManager.getInstance();
        manager.init(getDatabase(), new DynamicConfiguration(getDatabase()));
        createPipeline(pipelineSpecification.getTextProcessor(), pipelineSpecification.getName());
    }

    @Test
    public void testAnnotatedTextIsPersisted() {
        try (Transaction tx = getDatabase().beginTx()) {
            Node annotatedText = manager.annotateTextAndPersist(
                    "hello my name is John.",
                    "123",
                    pipelineSpecification
                    );
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
        manager.getConfiguration().update("LABEL_" + Labels.AnnotatedText.toString(), "TextAnnotation");
        try (Transaction tx = getDatabase().beginTx()) {
            Node annotatedText = manager.annotateTextAndPersist(
                    "hello my name is John.",
                    "123",
                    pipelineSpecification);
            assertEquals("123", annotatedText.getProperty("id").toString());
            assertTrue(annotatedText.hasLabel(Label.label("TextAnnotation")));
            tx.success();
        }
    }

    @Test
    public void testLanguageIsCorrectlyDetectedAndStored() {
        try (Transaction tx = getDatabase().beginTx()) {
            Node annotatedText = manager.annotateTextAndPersist(
                    "Barack Obama is born in Hawaii. He is our president.",
                    "123",
                    pipelineSpecification);
            tx.success();
        }

        TestNLPGraph test = new TestNLPGraph(getDatabase());
        test.assertTagWithIdExist("Barack_en");
        test.assertTagWithIdExist("born_en");
    }

    @Test
    public void testLanguageIsDefaultedToENWhenCheckLanguageIsFalseAndLanguageCouldNotBeDetected() {
        try (Transaction tx = getDatabase().beginTx()) {
            Node annotatedText = manager.annotateTextAndPersist(
                    "Barack Obama is born in Hawaii.",
                    "123",
                    pipelineSpecification);
            tx.success();
        }

        TestNLPGraph test = new TestNLPGraph(getDatabase());
        test.assertTagWithIdExist("Barack_en");
        test.assertTagWithIdExist("born_en");
    }

    @Test
    public void testAnnotationWillUseExistentFallbackLanguageIfCheckLanguageIsFalseAndNoLanguageIsDetected() {
        manager.getConfiguration().updateInternalSetting(SettingsConstants.FALLBACK_LANGUAGE, "en");
        try (Transaction tx = getDatabase().beginTx()) {
            Node annotatedText = manager.annotateTextAndPersist(
                    "Barack Obama is born in Hawaii.",
                    "123",
                    pipelineSpecification);
            tx.success();
        }
        TestNLPGraph test = new TestNLPGraph(getDatabase());
        test.assertTagWithIdExist("Barack_en");
        test.assertTagWithIdExist("born_en");
    }

    @Test
    public void testNamedEntityLabelIsAdded() {
        manager.getConfiguration().update("LABEL_" + Labels.AnnotatedText.toString(), "TextAnnotation");
        try (Transaction tx = getDatabase().beginTx()) {
            Node annotatedText = manager.annotateTextAndPersist(
                    "hello my name is John.",
                    "123",
                    pipelineSpecification);

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
    public void testNamedEntityLabelIsAddedOnTagOccurrences() {
        try (Transaction tx = getDatabase().beginTx()) {
            Node annotatedText = manager.annotateTextAndPersist(
                    "hello my name is John.",
                    "123",
                    pipelineSpecification);

            Iterator<Node> it = getDatabase().findNodes(Label.label("TagOccurrence"));
            assertTrue(it.hasNext());
            while (it.hasNext()) {
                Node node = it.next();
                assertTrue(node.hasLabel(Label.label("NE_Test")));
            }
            tx.success();
        }
    }

    @Test
    public void testMultipleSentencesPersistence() {
        PipelineSpecification myPipelineSpecification = new PipelineSpecification(
                TextProcessor.DEFAULT_PIPELINE + "_phrase",
                StubTextProcessor.class.getName());
        createPipeline(myPipelineSpecification.getTextProcessor(), myPipelineSpecification.getName(), "tokenizer", "phrase");

        try (Transaction tx = getDatabase().beginTx()) {
            manager.annotateTextAndPersist(
                    "hello my name is John. I am working for IBM. I live in Italy",
                    "123",
                    myPipelineSpecification.getName());
            tx.success();
        }

        TestNLPGraph tester = new TestNLPGraph(getDatabase());
        tester.assertAnnotatedTextNodesCount(1);
        tester.assertSentenceNodesCount(3);
        tester.assertTagNodesCount(14);
        tester.assertTagWithValueExist("hello");
        tester.assertTagWithValueExist("IBM");
        tester.assertTagWithValueHasNERLabel("name", "NER_Test");
        tester.assertPhraseWithTextExist("hello my name is John");
        tester.assertPhraseOccurrenceForTextHasStartAndEndPosition("hello my name is John", 0, "hello my name is John".length());
        tester.assertSentenceWithIdHasPhraseOccurrenceCount("123_0", 1);
        tester.assertSentenceWithIdHasPhraseWithText("123_0", "hello my name is John");
    }

    @Test
    public void testSentimentLabelCanBeAddedOnExistingSentence() {
        try (Transaction tx = getDatabase().beginTx()) {
            manager.annotateTextAndPersist(
                    "hello my name is John. I am working for IBM. I live in Italy",
                    "123",
                    pipelineSpecification);
            tx.success();
        }

        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().findNodes(Label.label("AnnotatedText")).stream().forEach(node -> {
                manager.applySentiment(node, StubTextProcessor.class.getName());
            });
            tx.success();
        }

        TestNLPGraph tester = new TestNLPGraph(getDatabase());
        tester.assertSentenceWithIdHasSentimentLabel("123_0", SentimentLabels.VeryPositive.toString());
    }

    @Test
    public void testAnnotationRunWithPipelineDefaultFromUserConfig() {
        manager.getConfiguration().updateInternalSetting(SettingsConstants.DEFAULT_PIPELINE, TextProcessor.DEFAULT_PIPELINE);
        try (Transaction tx = getDatabase().beginTx()) {
            manager.annotateTextAndPersist(
                    "hello my name is John. I am working for IBM. I live in Italy",
                    "123",
                    pipelineSpecification);
            tx.success();
        }

        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().findNodes(Label.label("AnnotatedText")).stream().forEach(node -> {
                manager.applySentiment(node, StubTextProcessor.class.getName());
            });
            tx.success();
        }

        TestNLPGraph tester = new TestNLPGraph(getDatabase());
        tester.assertSentenceWithIdHasSentimentLabel("123_0", SentimentLabels.VeryPositive.toString());
        assertEquals("tokenizer",
                ((StubTextProcessor) manager.getTextProcessorsManager().getTextProcessor("com.graphaware.nlp.stub.StubTextProcessor")).getLastPipelineUsed());
    }

    @Test
    public void testAnnotationWithCustomPipeline() {
        Map<String, Object> spec = new HashMap<>();
        spec.put("name", "my-pipeline");
        spec.put("textProcessor", StubTextProcessor.class.getName());
        spec.put("processingSteps", Collections.singletonMap("tokenize", true));
        spec.put("excludedNER", Collections.singletonList("test"));
        PipelineSpecification pipelineSpecification = PipelineSpecification.fromMap(spec);
        getNLPManager().getTextProcessorsManager().addPipeline(pipelineSpecification);
        try (Transaction tx = getDatabase().beginTx()) {
            manager.annotateTextAndPersist(
                    "hello my name is John. I am working for IBM. I live in Italy",
                    "123-fff",
                    pipelineSpecification
            );
            tx.success();
        }

        TestNLPGraph tester = new TestNLPGraph(getDatabase());
        tester.assertNodesCount("test", 0);
    }


    private void clearDatabase() {
        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().execute("MATCH (n) DETACH DELETE n");
            tx.success();
        }
    }

}
