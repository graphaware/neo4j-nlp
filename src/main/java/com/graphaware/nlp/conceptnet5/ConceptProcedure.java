/*
 * Copyright (c) 2013-2016 GraphAware
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
package com.graphaware.nlp.conceptnet5;

import static com.graphaware.nlp.conceptnet5.ConceptNet5Importer.DEFAULT_ADMITTED_RELATIONSHIP;
import static com.graphaware.nlp.conceptnet5.ConceptNet5Importer.DEFAULT_LANGUAGE;
import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.procedure.NLPProcedure;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.processor.stanford.StanfordTextProcessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.neo4j.collection.RawIterator;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.Iterators;
import org.neo4j.kernel.api.exceptions.ProcedureException;
import org.neo4j.kernel.api.proc.CallableProcedure;
import org.neo4j.kernel.api.proc.Neo4jTypes;
import org.neo4j.kernel.api.proc.ProcedureSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.neo4j.kernel.api.proc.ProcedureSignature.procedureSignature;

public class ConceptProcedure extends NLPProcedure {

    private static final Logger LOG = LoggerFactory.getLogger(ConceptProcedure.class);

    private final TextProcessor textProcessor;
    private final ConceptNet5Importer conceptnet5Importer;
    private final GraphDatabaseService database;

    private static final String PARAMETER_NAME_ANNOTATED_TEXT = "node";
    private static final String PARAMETER_NAME_DEPTH = "depth";
    private static final String PARAMETER_NAME_LANG = "lang";
    private static final String PARAMETER_NAME_ADMITTED_RELATIONSHIPS = "admittedRelationships";

    public ConceptProcedure(GraphDatabaseService database) {
        this.database = database;
        this.textProcessor = new StanfordTextProcessor();
        this.conceptnet5Importer = new ConceptNet5Importer.Builder("http://conceptnet5.media.mit.edu/data/5.4", textProcessor)
                .build();
    }

    public CallableProcedure.BasicProcedure concept() {
        return new CallableProcedure.BasicProcedure(procedureSignature(getProcedureName("concept"))
                .mode(ProcedureSignature.Mode.READ_WRITE)
                .in(PARAMETER_NAME_INPUT, Neo4jTypes.NTMap)
                .out(PARAMETER_NAME_INPUT_OUTPUT, Neo4jTypes.NTNode).build()) {

            @Override
            public RawIterator<Object[], ProcedureException> apply(CallableProcedure.Context ctx, Object[] input) throws ProcedureException {
                checkIsMap(input[0]);
                Map<String, Object> inputParams = (Map) input[0];
                Node annotatedNode = (Node) inputParams.get(PARAMETER_NAME_ANNOTATED_TEXT);
                int depth = ((Long) inputParams.getOrDefault(PARAMETER_NAME_DEPTH, 2)).intValue();
                String lang = (String) inputParams.getOrDefault(PARAMETER_NAME_LANG, DEFAULT_LANGUAGE);
                List<String> admittedRelationships = (List<String>) inputParams.getOrDefault(PARAMETER_NAME_ADMITTED_RELATIONSHIPS, Arrays.asList(DEFAULT_ADMITTED_RELATIONSHIP));
                try (Transaction beginTx = database.beginTx()) {
                    ResourceIterator<Node> tagsIterator = getAnnotatedTextTags(annotatedNode);
                    List<Tag> tags = new ArrayList<>();
                    while (tagsIterator.hasNext()) {
                        Tag tag = Tag.createTag(tagsIterator.next());
                        tags.add(tag);
                    }
                    List<Tag> conceptTags = new ArrayList<>();
                    tags.parallelStream().forEach((tag) -> {
                        conceptTags.addAll(conceptnet5Importer.importHierarchy(tag, lang, depth, admittedRelationships));
                        conceptTags.add(tag);
                    });

                    conceptTags.stream().forEach((newTag) -> {
                        newTag.storeOnGraph(database);
                    });
                    beginTx.success();
                }
                return Iterators.asRawIterator(Collections.<Object[]>singleton(new Object[]{annotatedNode}).iterator());
            }

            private ResourceIterator<Node> getAnnotatedTextTags(Node annotatedNode) throws QueryExecutionException {
                Map<String, Object> params = new HashMap<>();
                params.put("id", annotatedNode.getId());
                Result queryRes = database.execute("MATCH (n:AnnotatedText)-[*..2]->(t:Tag) where id(n) = {id} return t", params);
                ResourceIterator<Node> tags = queryRes.columnAs("t");
                return tags;
            }
        };
    }
}
