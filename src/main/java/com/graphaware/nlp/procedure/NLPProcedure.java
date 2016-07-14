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
package com.graphaware.nlp.procedure;

import com.graphaware.nlp.conceptnet5.ConceptNet5Importer;
import static com.graphaware.nlp.conceptnet5.ConceptNet5Importer.DEFAULT_ADMITTED_RELATIONSHIP;
import static com.graphaware.nlp.conceptnet5.ConceptNet5Importer.DEFAULT_LANGUAGE;
import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.domain.Labels;
import com.graphaware.nlp.domain.Properties;
import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.logic.FeatureBasedProcessLogic;
import com.graphaware.nlp.processor.TextProcessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import static org.neo4j.kernel.api.proc.ProcedureSignature.procedureName;
import static org.neo4j.kernel.api.proc.ProcedureSignature.procedureSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NLPProcedure {
    private static final Logger LOG = LoggerFactory.getLogger(TextProcessor.class);

    private final TextProcessor textProcessor;
    private final ConceptNet5Importer conceptnet5Importer;
    private final GraphDatabaseService database;

    private static final String PARAMETER_NAME_INPUT = "input";
    private static final String PARAMETER_NAME_TEXT = "text";
    private static final String PARAMETER_NAME_ANNOTATED_TEXT = "node";
    private static final String PARAMETER_NAME_DEPTH = "depth";
    private static final String PARAMETER_NAME_LANG = "lang";
    private static final String PARAMETER_NAME_ADMITTED_RELATIONSHIPS = "admittedRelationships";
    private static final String PARAMETER_NAME_ID = "id";
    private static final String PARAMETER_NAME_SENTIMENT = "sentiment";
    private static final String PARAMETER_NAME_STORE_TEXT = "store";
    private static final String PARAMETER_NAME_INPUT_OUTPUT = "result";
    private static final String PARAMETER_NAME_SCORE = "score";

    private final FeatureBasedProcessLogic featureBusinessLogic;

    public NLPProcedure(GraphDatabaseService database, FeatureBasedProcessLogic featureBusinessLogic) {
        this.database = database;
        this.textProcessor = new TextProcessor();
        this.conceptnet5Importer = new ConceptNet5Importer.Builder("http://conceptnet5.media.mit.edu/data/5.4", textProcessor)
                .build();
        this.featureBusinessLogic = featureBusinessLogic;
    }

    public CallableProcedure.BasicProcedure annotate() {
        return new CallableProcedure.BasicProcedure(procedureSignature(getProcedureName("annotate"))
                .mode(ProcedureSignature.Mode.READ_WRITE)
                .in(PARAMETER_NAME_INPUT, Neo4jTypes.NTMap)
                .out(PARAMETER_NAME_INPUT_OUTPUT, Neo4jTypes.NTNode).build()) {

            @Override
            public RawIterator<Object[], ProcedureException> apply(Context ctx, Object[] input) throws ProcedureException {
                
                try {
                    checkIsMap(input[0]);
                    Map<String, Object> inputParams = (Map) input[0];
                    String text = (String) inputParams.get(PARAMETER_NAME_TEXT);
                    LOG.warn("Text: " + text);
                    Object id = inputParams.get(PARAMETER_NAME_ID);
                    boolean sentiment = (Boolean) inputParams.getOrDefault(PARAMETER_NAME_SENTIMENT, false);
                    boolean store = (Boolean) inputParams.getOrDefault(PARAMETER_NAME_STORE_TEXT, true);
                    Node annotatedText = checkIfExist(id);
                    if (annotatedText == null) {
                        AnnotatedText annotateText = textProcessor.annotateText(text, id, sentiment, store);
                        annotatedText = annotateText.storeOnGraph(database);
                    }
                    return Iterators.asRawIterator(Collections.<Object[]>singleton(new Object[]{annotatedText}).iterator());
                }
                catch (Exception ex) {
                    LOG.error("Error while annotating", ex);
                    throw ex;
                }                
            }

            private Node checkIfExist(Object id) {
                if (id != null) {
                    ResourceIterator<Node> findNodes = database.findNodes(Labels.AnnotatedText, Properties.PROPERTY_ID, id);
                    if (findNodes.hasNext()) {
                        return findNodes.next();
                    }
                }
                return null;
            }
        };
    }

    public CallableProcedure.BasicProcedure sentiment() {
        return new CallableProcedure.BasicProcedure(procedureSignature(getProcedureName("sentiment"))
                .mode(ProcedureSignature.Mode.READ_WRITE)
                .in(PARAMETER_NAME_INPUT, Neo4jTypes.NTMap)
                .out(PARAMETER_NAME_INPUT_OUTPUT, Neo4jTypes.NTNode).build()) {

            @Override
            public RawIterator<Object[], ProcedureException> apply(CallableProcedure.Context ctx, Object[] input) throws ProcedureException {
                checkIsMap(input[0]);
                Map<String, Object> inputParams = (Map) input[0];
                Node annotatedNode = (Node) inputParams.get(PARAMETER_NAME_ANNOTATED_TEXT);
                AnnotatedText annotatedText = AnnotatedText.load(annotatedNode);
                annotatedText = textProcessor.sentiment(annotatedText);
                annotatedText.storeOnGraph(database);
                return Iterators.asRawIterator(Collections.<Object[]>singleton(new Object[]{annotatedNode}).iterator());
            }
        };
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
                    ResourceIterator<Node> tags = getAnnotatedTextTags(annotatedNode);
                    while (tags.hasNext()) {
                        final Tag tag = Tag.createTag(tags.next());
                        List<Tag> conceptTags = conceptnet5Importer.importHierarchy(tag, lang, depth, admittedRelationships);
                        conceptTags.stream().forEach((newTag) -> {
                            newTag.storeOnGraph(database);
                        });
                        tag.storeOnGraph(database);
                    }
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

    public CallableProcedure.BasicProcedure computeAll() {
        return new CallableProcedure.BasicProcedure(procedureSignature(getProcedureName("cosine", "compute"))
                .mode(ProcedureSignature.Mode.READ_WRITE)
                .in(PARAMETER_NAME_INPUT, Neo4jTypes.NTAny)
                .out(PARAMETER_NAME_INPUT_OUTPUT, Neo4jTypes.NTInteger).build()) {

            @Override
            public RawIterator<Object[], ProcedureException> apply(CallableProcedure.Context ctx, Object[] input) throws ProcedureException {
                int processed = 0;
                List<Long> firstNodeIds = getNodesFromInput(input);
                processed = featureBusinessLogic.computeFeatureSimilarityForNodes(firstNodeIds);
                return Iterators.asRawIterator(Collections.<Object[]>singleton(new Integer[]{processed}).iterator());
            }
        };
    }

    public CallableProcedure.BasicProcedure search() {
        return new CallableProcedure.BasicProcedure(procedureSignature(getProcedureName("search"))
                .mode(ProcedureSignature.Mode.READ_WRITE)
                .in(PARAMETER_NAME_INPUT, Neo4jTypes.NTString)
                .out(PARAMETER_NAME_INPUT_OUTPUT, Neo4jTypes.NTNode)
                .out(PARAMETER_NAME_SCORE, Neo4jTypes.NTFloat).build()) {

            @Override
            public RawIterator<Object[], ProcedureException> apply(CallableProcedure.Context ctx, Object[] input) throws ProcedureException {
                String text = (String) input[0];
                AnnotatedText annotateText = textProcessor.annotateText(text, 0, false, false);
                List<String> tokens = annotateText.getTokens();
                Map<String, Object> params = new HashMap<>();
                params.put("tokens", tokens);
                params.put("querySize", tokens.size());
                Result queryResult = database.execute("MATCH (doc:AnnotatedText)\n"
                        + "WITH count(doc) as documentsCount\n"
                        + "MATCH (t:Tag)\n"
                        + "WHERE t.value IN {tokens}\n"
                        + "WITH t, documentsCount, {querySize} as queryTagsCount\n"
                        + "MATCH (t)<-[:HAS_TAG]-(:Sentence)<-[]-(document:AnnotatedText)\n"
                        + "WITH t, count(distinct document) as documentsCountForTag, documentsCount, queryTagsCount\n"
                        + "MATCH (t)<-[ht:HAS_TAG]-(sentence:Sentence)<-[]-(at:AnnotatedText)\n"
                        + "WITH DISTINCT at, t.value as value, sum(ht.tf)*(1 + log((1.0f*documentsCount)/(documentsCountForTag + 1)))* (1.0f/at.numTerms^0.5f) as sum, queryTagsCount\n"
                        + "RETURN at, (1.0f*size(collect(value))/queryTagsCount)*(sum(sum)) as score\n", params);
                Set<Object[]> result = new HashSet<>();
                while (queryResult.hasNext()) {
                    Map<String, Object> row = queryResult.next();
                    result.add(new Object[]{row.get("at"), row.get("score")});
                }
                return Iterators.asRawIterator(result.iterator());
            }
        };
    }

    protected List<Long> getNodesFromInput(Object[] input) {
        List<Long> firstNodeIds = new ArrayList<>();
        if (input[0] == null) {
            return null;
        } else if (input[0] instanceof Node) {
            firstNodeIds.add(((Node) input[0]).getId());
            return firstNodeIds;
        } else if (input[0] instanceof Map) {
            Map<String, Object> nodesMap = (Map) input[0];
            nodesMap.values().stream().filter((element) -> (element instanceof Node)).forEach((element) -> {
                firstNodeIds.add(((Node) element).getId());
            });
            if (!firstNodeIds.isEmpty()) {
                return firstNodeIds;
            } else {
                return null;
            }
        } else {
            throw new RuntimeException("Invalid input parameters " + input[0]);
        }
    }

    protected static ProcedureSignature.ProcedureName getProcedureName(String... procedureName) {
        String namespace[] = new String[2 + procedureName.length];
        int i = 0;
        namespace[i++] = "ga";
        namespace[i++] = "nlp";

        for (String value : procedureName) {
            namespace[i++] = value;
        }
        return procedureName(namespace);
    }

    protected void checkIsMap(Object object) throws RuntimeException {
        if (!(object instanceof Map)) {
            throw new RuntimeException("Input parameter is not a map");
        }
    }

}
