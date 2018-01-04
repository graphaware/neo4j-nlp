package com.graphaware.nlp.persistence;

import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.domain.Sentence;
import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.util.TestNLPGraph;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class AnnotatedTextPersistenceTest extends NLPIntegrationTest {

    @Test
    public void testTagsHavingTwoDifferentPOSInDifferentSentencesShouldReflectBothPOS() {
        String text = "The discipline of preparing and peer reviewing formal engineering reports leads to a high degree of accuracy and technical rigor.";
        String text2 = "During this effort to establish accurate crack information, it was discovered that several cracks were kinked rather than extending in a self-similar crack growth direction as was implied by the sketches and analyses reports in the briefing charts.";
        TestNLPGraph test = new TestNLPGraph(getDatabase());
        AnnotatedText at1 = createAnnotatedTextFor(text, "reports", "VGB");
        try (Transaction tx = getDatabase().beginTx()) {
            getNLPManager().getPersister(AnnotatedText.class).persist(at1, "test-a", "1");
            tx.success();
        }
        test.assertTagWithValueHasPos("reports", "VGB");
        AnnotatedText at2 = createAnnotatedTextFor(text2, "reports", "NNS");
        try (Transaction tx = getDatabase().beginTx()) {
            getNLPManager().getPersister(AnnotatedText.class).persist(at2, "test-b", String.valueOf(System.currentTimeMillis()));
            tx.success();
        }
        test.assertTagWithValueHasPos("reports", "VGB");
        test.assertTagWithValueHasPos("reports", "NNS");
    }

    @Test
    public void testAnnotatedTextWithSameTagInSameTextGotDifferentPOS() {
        clearDb();
        TestNLPGraph test = new TestNLPGraph(getDatabase());
        AnnotatedText annotatedText = createAnnotatedTextWithSameTagInSameTextWithDifferentPos();
        try (Transaction tx = getDatabase().beginTx()) {
            getNLPManager().getPersister(AnnotatedText.class).persist(annotatedText, "test", "1");
            tx.success();
        }
        test.assertTagWithValueHasPos("cool", "cool0");
        test.assertTagWithValueHasPos("cool", "cool1");
        test.assertTagWithValueHasNE("cool", "NER_Cool0");
        test.assertTagWithValueHasNE("cool", "NER_Cool1");
    }

    @Test
    public void testTagOccurrenceGetAValue() {
        clearDb();
        AnnotatedText annotatedText = createAnnotatedTextWithSameTagInSameTextWithDifferentPos();
        TestNLPGraph test = new TestNLPGraph(getDatabase());
        try (Transaction tx = getDatabase().beginTx()) {
            getNLPManager().getPersister(AnnotatedText.class).persist(annotatedText, "test", "1");
            tx.success();
        }
        test.assertTagOccurrenceWithValueExist("cool");
        executeInTransaction("MATCH (n:TagOccurrence) WHERE n.value = 'cool' RETURN n", (result -> {
            assertTrue(result.hasNext());
            Node n = (Node) result.next().get("n");
            String[] ners = (String[]) n.getProperty("ne");
            assertTrue(Arrays.asList(ners).contains("NER_Cool0"));
        }));
    }

    @Test
    public void testTagOccurrenceGetANERProperty() {
        clearDb();
        AnnotatedText annotatedText = createAnnotatedTextWithSameTagInSameTextWithDifferentPos();
        try (Transaction tx = getDatabase().beginTx()) {
            getNLPManager().getPersister(AnnotatedText.class).persist(annotatedText, "test", "1");
            tx.success();
        }
        executeInTransaction("MATCH (n:TagOccurrence) WHERE n.value = 'cool' RETURN n", (result -> {
            assertTrue(result.hasNext());
            Node n = (Node) result.next().get("n");
            String[] ners = (String[]) n.getProperty("ne");
            assertTrue(Arrays.asList(ners).contains("NER_Cool0"));
        }));
    }

    @Test
    public void testTagOccurrenceGetAPOSProperty() {
        clearDb();
        AnnotatedText annotatedText = createAnnotatedTextWithSameTagInSameTextWithDifferentPos();
        try (Transaction tx = getDatabase().beginTx()) {
            getNLPManager().getPersister(AnnotatedText.class).persist(annotatedText, "test", "1");
            tx.success();
        }
        executeInTransaction("MATCH (n:TagOccurrence) WHERE n.value = 'cool' RETURN n", (result -> {
            assertTrue(result.hasNext());
            Node n = (Node) result.next().get("n");
            String[] ners = (String[]) n.getProperty("pos");
            assertTrue(Arrays.asList(ners).contains("cool0"));
        }));
    }

    private AnnotatedText createAnnotatedTextFor(String text, String expectedTokenForPOS, String expectedPOS) {
        AnnotatedText annotatedText = new AnnotatedText();
        annotatedText.setText(text);
        AtomicInteger inc = new AtomicInteger();
        for (String s : text.split("\\.")) {
            Sentence sentence = new Sentence(s, inc.get());
            for (String token : s.split(" ")) {
                Tag tag = new Tag(token, "en");
                if (token.equals(expectedTokenForPOS)) {
                    tag.setPos(Collections.singletonList(expectedPOS));
                }
                sentence.addTagOccurrence(0, 20, token, sentence.addTag(tag));
            }
            inc.incrementAndGet();
            annotatedText.addSentence(sentence);
        }

        return annotatedText;
    }

    private AnnotatedText createAnnotatedTextWithSameTagInSameTextWithDifferentPos() {
        AnnotatedText annotatedText = new AnnotatedText();
        AtomicInteger inc = new AtomicInteger();
        for (String s : "Hello my name is cool. And I am cool.".split("\\.")) {
            Sentence sentence = new Sentence(s, inc.get());
            for (String token : s.split(" ")) {
                Tag tag = new Tag(token, "en");
                if (token.equals("cool")) {
                    int v = inc.get();
                    tag.setPos(Collections.singletonList("cool" + v));
                    tag.setNe(Collections.singletonList("NER_Cool" + v));
                }
                sentence.addTagOccurrence(0, 20, token, sentence.addTag(tag));
            }
            inc.incrementAndGet();
            annotatedText.addSentence(sentence);
        }
        return annotatedText;
    }
}
