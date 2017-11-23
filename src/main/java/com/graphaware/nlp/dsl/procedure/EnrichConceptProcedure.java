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
package com.graphaware.nlp.dsl.procedure;

import com.graphaware.nlp.dsl.AbstractDSL;
import com.graphaware.nlp.dsl.request.ConceptRequest;
import com.graphaware.nlp.dsl.result.EnricherList;
import com.graphaware.nlp.dsl.result.NodeResult;
import com.graphaware.nlp.enrich.Enricher;
import org.neo4j.graphdb.Node;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class EnrichConceptProcedure extends AbstractDSL {
   
    @Procedure(name = "ga.nlp.enrich.concept", mode = Mode.WRITE)
    @Description("Enrich knowledge concepts by consulting external knowledge bases like ConceptNet5 or Microsoft Concept Graphs")
    public Stream<NodeResult> annotate(@Name("conceptRequest") Map<String, Object> conceptRequest) {
        ConceptRequest request = ConceptRequest.fromMap(conceptRequest);
        Enricher enricher = getNLPManager().getEnricher(request.getEnricherName());
        Node result = enricher.importConcept(request);
        return Stream.of(new NodeResult(result));
    }

    @Procedure(name = "ga.nlp.enrichers.list", mode = Mode.READ)
    @Description("List enrichers available")
    public Stream<EnricherList> list() {
        Map<String, Enricher> enrichers = getNLPManager().getEnrichmentRegistry().getEnrichers();
        List<EnricherList> list = new ArrayList<>();
        enrichers.values().forEach(e -> {
            list.add(new EnricherList(e.getName(), e.getAlias()));
        });

        return list.stream();
    }

}
