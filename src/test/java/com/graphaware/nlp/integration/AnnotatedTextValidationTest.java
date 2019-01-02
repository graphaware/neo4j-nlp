package com.graphaware.nlp.integration;

import com.graphaware.nlp.NLPEvents;
import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.event.TextAnnotationEvent;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.stub.StubTextProcessor;
import org.junit.Test;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class AnnotatedTextValidationTest extends NLPIntegrationTest {

    @Test
    public void testAnnotatedTextCanBeModifiedWithEvents() {
        NLPManager.getInstance().getEventDispatcher().registerListener(NLPEvents.PRE_ANNOTATION_STORAGE, (event) -> {
            TextAnnotationEvent e = (TextAnnotationEvent) event;
            e.getAnnotatedText().getSentences().forEach(sentence -> {
                sentence.getTagOccurrences().values().forEach(tagOccurrences -> {
                    tagOccurrences.forEach(tagOccurrence -> {
                        if (tagOccurrence.hasNamedEntity()) {
                            if (tagOccurrence.getValue().equalsIgnoreCase("name")) {
                                tagOccurrence.getElement().setNe(new ArrayList<>());
                            }
                        }
                    });
                });
            });
        });

        createPipeline(pipelineSpecification.getTextProcessor(), pipelineSpecification.getName());

        try (Transaction tx = getDatabase().beginTx()) {
            Node annotatedText = getNLPManager().annotateTextAndPersist(
                    "hello my name is John.",
                    "123",
                    pipelineSpecification);
            assertEquals("123", annotatedText.getProperty("id").toString());
            tx.success();
        }

        try (Transaction tx = getDatabase().beginTx()) {
            Result result = getDatabase().execute("MATCH (n:TagOccurrence) WHERE n.value = 'name' AND size(n.ne) = 0 RETURN n");
            assertTrue(result.hasNext());
            Result result2 = getDatabase().execute("MATCH (n:TagOccurrence) WHERE n.value = 'hello' AND n.ne[0] = 'test' RETURN n");
            assertTrue(result2.hasNext());
        }
    }
}
