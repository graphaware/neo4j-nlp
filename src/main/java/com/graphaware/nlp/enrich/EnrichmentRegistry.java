/*
 * Copyright (c) 2013-2017 GraphAware
 *
 * This file is part of the GraphAware Framework.
 *
 * GraphAware Framework is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package com.graphaware.nlp.enrich;

import java.util.HashMap;
import java.util.Map;

public class EnrichmentRegistry {

    private final Map<String, Enricher> enrichers = new HashMap<>();

    private final Map<String, Enricher> enrichersByAlias = new HashMap<>();

    public void register(Enricher enricher) {
        enrichers.put(enricher.getName(), enricher);
        enrichersByAlias.put(enricher.getAlias(), enricher);
    }

    public Enricher get(String name) {
        return enrichers.get(name);
    }

    public Enricher resolve(String key) {
        if (enrichersByAlias.containsKey(key)) {
            return enrichersByAlias.get(key);
        }

        if (enrichers.containsKey(key)) {
            return enrichers.get(key);
        }

        throw new RuntimeException("Unknown enricher : " + key);
    }

    public Map<String, Enricher> getEnrichers() {
        return enrichers;
    }
}
