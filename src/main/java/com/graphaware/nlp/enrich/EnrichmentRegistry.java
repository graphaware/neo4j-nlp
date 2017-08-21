package com.graphaware.nlp.enrich;

import java.util.HashMap;
import java.util.Map;

public class EnrichmentRegistry {

    private final Map<String, Enricher> enrichers = new HashMap<>();

    public void register(Enricher enricher) {
        enrichers.put(enricher.getName(), enricher);
    }

    public Enricher get(String name) {
        return enrichers.get(name);
    }

}
