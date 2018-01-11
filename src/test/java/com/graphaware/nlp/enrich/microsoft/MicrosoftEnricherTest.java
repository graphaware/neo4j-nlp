package com.graphaware.nlp.enrich.microsoft;


import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.dsl.request.ConceptRequest;
import com.graphaware.nlp.enrich.EnricherAbstractTest;
import com.graphaware.nlp.persistence.PersistenceRegistry;
import com.graphaware.nlp.processor.TextProcessorsManager;
import com.graphaware.nlp.stub.StubTextProcessor;
import com.graphaware.nlp.util.TestNLPGraph;
import org.junit.Test;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

import static org.junit.Assert.*;

public class MicrosoftEnricherTest extends EnricherAbstractTest {

    @Test
    public void testCanGetConceptsFromMicrosoft() {
        DynamicConfiguration configuration = new DynamicConfiguration(getDatabase());
        PersistenceRegistry registry = new PersistenceRegistry(getDatabase());
        MicrosoftConceptEnricher enricher = new MicrosoftConceptEnricher(getDatabase(), registry, new TextProcessorsManager());
        clearDb();
        executeInTransaction("CALL ga.nlp.annotate({text: 'kill cats', id: 'test-proc', checkLanguage: false})", emptyConsumer());

        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().findNodes(Label.label("AnnotatedText")).stream().forEach(node -> {
                ConceptRequest request = new ConceptRequest();
                request.setAnnotatedNode(node);
                request.setLanguage("en");
                request.setDepth(1);
                request.setProcessor(StubTextProcessor.class.getName());
                enricher.importConcept(request);

                tx.success();
            });
        }

        debugTagsRelations();

        TestNLPGraph tester = new TestNLPGraph(getDatabase());
        tester.assertTagWithValueExist("pet");
        tester.assertTagWithValueExist("specie");
        tester.assertTagHasRelatedTag("cats", "animal");
        tester.assertTagHasRelatedTag("cats", "pet");
    }

    @Test
    public void testEnricherNameIsSetAsRelationshipProperty() {
        DynamicConfiguration configuration = new DynamicConfiguration(getDatabase());
        PersistenceRegistry registry = new PersistenceRegistry(getDatabase());
        MicrosoftConceptEnricher enricher = new MicrosoftConceptEnricher(getDatabase(), registry, new TextProcessorsManager());
        clearDb();
        executeInTransaction("CALL ga.nlp.annotate({text: 'kill cats', id: 'test-proc', checkLanguage: false})", emptyConsumer());

        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().findNodes(Label.label("AnnotatedText")).stream().forEach(node -> {
                ConceptRequest request = new ConceptRequest();
                request.setAnnotatedNode(node);
                request.setLanguage("en");
                request.setDepth(1);
                request.setProcessor(StubTextProcessor.class.getName());
                enricher.importConcept(request);

                tx.success();
            });
        }

        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().getAllRelationships().stream().forEach(r -> {
                if (r.isType(RelationshipType.withName("IS_RELATED_TO"))) {
                    assertTrue(r.hasProperty("source"));
                    assertEquals(MicrosoftConceptEnricher.ENRICHER_NAME, r.getProperty("source").toString());
                }
            });

            tx.success();
        }
    }
}
