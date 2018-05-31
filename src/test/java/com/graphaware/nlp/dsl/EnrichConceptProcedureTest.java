package com.graphaware.nlp.dsl;

import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.dsl.request.ConceptRequest;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.stub.StubTextProcessor;
import com.graphaware.nlp.util.NodeProxy;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class EnrichConceptProcedureTest extends NLPIntegrationTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        createPipeline(StubTextProcessor.class.getName(), TextProcessor.DEFAULT_PIPELINE);
        executeInTransaction("CALL ga.nlp.processor.pipeline.default({p0})", buildSeqParameters("tokenizer"), emptyConsumer());
    }

    @Test
    public void testConceptProcedureWithAnnotatedNode() {
        executeInTransaction("CALL ga.nlp.annotate({text:'John and Adam planned to kill the cat', id: '123', textProcessor:'com.graphaware.nlp.stub.StubTextProcessor'})", (result -> {
            assertTrue(result.hasNext());
        }));

        executeInTransaction("MATCH (n:AnnotatedText) CALL ga.nlp.enrich.concept({node: n, depth: 1, language:'en'}) YIELD result RETURN result", (result -> {
            assertTrue(result.hasNext());
        }));
    }

    @Test
    @Ignore
    public void testConceptProcedureWithNonDefaultEnricher() {
        executeInTransaction("CALL ga.nlp.annotate({text:'John and Adam planned to kill the cat', id: '123', textProcessor:'com.graphaware.nlp.stub.StubTextProcessor'})", (result -> {
            assertTrue(result.hasNext());
        }));

        executeInTransaction("MATCH (n:AnnotatedText) CALL ga.nlp.enrich.concept({enricher: 'microsoft', node: n, depth: 1, language:'en'}) YIELD result RETURN result", (result -> {
            assertTrue(result.hasNext());
        }));
    }

    @Test
    public void testConceptEnrichersCanBeListed() {
        final List<String> aliases = new ArrayList<>();
        executeInTransaction("CALL ga.nlp.enrichers.list", (result -> {
            while (result.hasNext()) {
                aliases.add(result.next().get("alias").toString());
            }
        }));
        assertTrue(aliases.contains("microsoft"));
        assertTrue(aliases.contains("conceptnet5"));
    }

    @Test(expected = RuntimeException.class)
    public void testNonRegisteredEnricherThrowsException() {
        executeInTransaction("CALL ga.nlp.annotate({text:'John and Adam planned to kill the cat', id: '123', textProcessor:'com.graphaware.nlp.stub.StubTextProcessor'})", (result -> {
            assertTrue(result.hasNext());
        }));

        executeInTransaction("MATCH (n:AnnotatedText) CALL ga.nlp.enrich.concept({enricher: 'UNK', node: n, depth: 1, language:'en'}) YIELD result RETURN result", (result -> {
            assertTrue(result.hasNext());
        }));
    }

    @Test
    public void testEnrichmentTakeMinWeightIntoAccount() {
        clearDb();
        executeInTransaction("CALL ga.nlp.annotate({text:'John and Adam went to college.', id: '123', textProcessor:'com.graphaware.nlp.stub.StubTextProcessor'})", (result -> {
            assertTrue(result.hasNext());
        }));

        executeInTransaction("MATCH (n:AnnotatedText) CALL ga.nlp.enrich.concept({enricher: 'conceptnet5', node: n, depth: 1, language:'en', minWeight:100.0}) YIELD result RETURN result", (result -> {
            assertTrue(result.hasNext());
        }));

        executeInTransaction("MATCH (n:Tag {value:'college'})-[:IS_RELATED_TO]->(x) RETURN x", (result -> {
            assertFalse(result.hasNext());
        }));
    }
}
