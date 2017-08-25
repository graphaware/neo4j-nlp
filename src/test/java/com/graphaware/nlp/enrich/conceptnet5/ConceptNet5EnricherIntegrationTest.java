package com.graphaware.nlp.enrich.conceptnet5;

import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.persistence.PersistenceRegistry;
import com.graphaware.nlp.processor.TextProcessorsManager;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConceptNet5EnricherIntegrationTest extends NLPIntegrationTest {

    @Test
    public void testConceptNetUrlIsConfigurable() {
        DynamicConfiguration configuration = new DynamicConfiguration(getDatabase());
        PersistenceRegistry registry = new PersistenceRegistry(getDatabase(), configuration);
        ConceptNet5Enricher enricher = new ConceptNet5Enricher(getDatabase(), registry, configuration, new TextProcessorsManager(getDatabase()));
        assertEquals("http://api.conceptnet.io", enricher.getConceptNetUrl());

        configuration.updateInternalSetting("CONCEPT_NET_5_URL", "http://localhost:8001");
        assertEquals("http://localhost:8001", enricher.getConceptNetUrl());
    }

}
