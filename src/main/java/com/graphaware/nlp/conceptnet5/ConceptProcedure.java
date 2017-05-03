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
import com.graphaware.nlp.module.NLPModule;
import com.graphaware.nlp.procedure.NLPProcedure;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.processor.TextProcessorsManager;
import com.graphaware.runtime.GraphAwareRuntime;
import com.graphaware.runtime.RuntimeRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.neo4j.collection.RawIterator;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.helpers.collection.Iterators;
import org.neo4j.kernel.api.exceptions.ProcedureException;
import org.neo4j.kernel.api.proc.CallableProcedure;
import org.neo4j.kernel.api.proc.Context;
import org.neo4j.kernel.api.proc.Neo4jTypes;
import org.neo4j.procedure.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.neo4j.kernel.api.proc.ProcedureSignature.procedureSignature;

public class ConceptProcedure extends NLPProcedure {

    private static final Logger LOG = LoggerFactory.getLogger(ConceptProcedure.class);

    private final TextProcessor textProcessor;
    private ConceptNet5Importer conceptnet5Importer;
    private final GraphDatabaseService database;

    private static final String PARAMETER_NAME_ANNOTATED_TEXT = "node";
    private static final String PARAMETER_NAME_TAG = "tag";
    private static final String PARAMETER_NAME_DEPTH = "depth";
    private static final String PARAMETER_NAME_LANG = "lang";
    private static final String PARAMETER_NAME_SPLIT_TAG = "splitTag";
    private static final String PARAMETER_NAME_ADMITTED_RELATIONSHIPS = "admittedRelationships";

    public ConceptProcedure(GraphDatabaseService database, TextProcessorsManager processorManager) {
        this.database = database;
        this.textProcessor = processorManager.getDefaultProcessor();

    }

    public ConceptNet5Importer getImporter() {
        if (conceptnet5Importer == null) {
            GraphAwareRuntime runtime = RuntimeRegistry.getStartedRuntime(database);
            LOG.error(">>>>>>>: " + runtime.getModule("NLP", NLPModule.class).getClass().toString());
            String url = runtime.getModule(NLPModule.class).getNlpMLConfiguration().getConceptNetUrl();
//        this.conceptnet5Importer = new ConceptNet5Importer.Builder("http://api.localhost", textProcessor)
            this.conceptnet5Importer = new ConceptNet5Importer.Builder(url, textProcessor).build();
        }
        return conceptnet5Importer;
    }

    public CallableProcedure.BasicProcedure concept() {
        return new CallableProcedure.BasicProcedure(procedureSignature(getProcedureName("concept"))
                .mode(Mode.WRITE)
                .in(PARAMETER_NAME_INPUT, Neo4jTypes.NTMap)
                .out(PARAMETER_NAME_INPUT_OUTPUT, Neo4jTypes.NTNode).build()) {

            @Override
            public RawIterator<Object[], ProcedureException> apply(Context ctx, Object[] input) throws ProcedureException {
                try {
                    checkIsMap(input[0]);
                    List<Tag> conceptTags = new ArrayList<>();
                    Map<String, Object> inputParams = (Map) input[0];
                    Node annotatedNode = (Node) inputParams.get(PARAMETER_NAME_ANNOTATED_TEXT);
                    Node tagToBeAnnotated = null;
                    if (annotatedNode == null) {
                        tagToBeAnnotated = (Node) inputParams.get(PARAMETER_NAME_TAG);
                    }
                    int depth = ((Long) inputParams.getOrDefault(PARAMETER_NAME_DEPTH, 2)).intValue();
                    String lang = (String) inputParams.getOrDefault(PARAMETER_NAME_LANG, DEFAULT_LANGUAGE);
                    Boolean splitTags = (Boolean) inputParams.getOrDefault(PARAMETER_NAME_SPLIT_TAG, false);
                    List<String> admittedRelationships = (List<String>) inputParams.getOrDefault(PARAMETER_NAME_ADMITTED_RELATIONSHIPS, Arrays.asList(DEFAULT_ADMITTED_RELATIONSHIP));
                    //try (Transaction beginTx = database.beginTx()) {
                    Iterator<Node> tagsIterator;
                    if (annotatedNode != null) {
                        tagsIterator = getAnnotatedTextTags(annotatedNode);
                    } else if (tagToBeAnnotated != null) {
                        List<Node> proc = new ArrayList<>();
                        proc.add(tagToBeAnnotated);
                        tagsIterator = proc.iterator();
                    } else {
                        throw new RuntimeException("You need to specify or an annotated text or a list of tags");
                    }

                    List<Tag> tags = new ArrayList<>();
                    while (tagsIterator.hasNext()) {
                        Tag tag = Tag.createTag(tagsIterator.next());
                        if (splitTags) {
                            List<Tag> annotateTags = textProcessor.annotateTags(tag.getLemma(), lang);
                            if (annotateTags.size() == 1 && annotateTags.get(0).getLemma().equalsIgnoreCase(tag.getLemma())) {
                                tags.add(tag);
                            } else {
                                annotateTags.forEach((newTag) -> {
                                    tags.add(newTag); 
                                    tag.addParent("subTag", newTag, 0.0f);
                                });
                                conceptTags.add(tag);
                            }
                        } else {
                            tags.add(tag);                            
                        }

                    }
                    tags.parallelStream().forEach((tag) -> {
                        conceptTags.addAll(getImporter().importHierarchy(tag, lang, depth, admittedRelationships));
                        conceptTags.add(tag);
                    });

                    conceptTags.stream().forEach((newTag) -> {
                        newTag.storeOnGraph(database, false);
                    });
                    if (annotatedNode != null) {
                        return Iterators.asRawIterator(Collections.<Object[]>singleton(new Object[]{annotatedNode}).iterator());
                    } else {
                        Set<Object[]> result = new HashSet<>();
                        conceptTags.stream().forEach((item) -> {
                            result.add(new Object[]{item});
                        });
                        return Iterators.asRawIterator(result.iterator());
                    }
                } catch (Exception ex) {
                    LOG.error("error!!!! ", ex);
                    throw new RuntimeException("Error", ex);
                }
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
