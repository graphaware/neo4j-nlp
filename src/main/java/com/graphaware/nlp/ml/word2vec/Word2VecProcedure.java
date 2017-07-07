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
package com.graphaware.nlp.ml.word2vec;

import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.ml.similarity.*;
import com.graphaware.nlp.procedure.NLPProcedure;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.processor.TextProcessorsManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.neo4j.collection.RawIterator;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.helpers.collection.Iterators;
import org.neo4j.kernel.api.exceptions.ProcedureException;
import org.neo4j.kernel.api.proc.CallableProcedure;
import org.neo4j.kernel.api.proc.Neo4jTypes;
import org.neo4j.kernel.api.proc.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.neo4j.kernel.api.proc.ProcedureSignature.procedureSignature;
import org.neo4j.procedure.Mode;

public class Word2VecProcedure extends NLPProcedure {

    private static final String PARAMETER_NAME_ANNOTATED_TEXT = "node";
    private static final String PARAMETER_NAME_TAG = "tag";
    private static final String PARAMETER_NAME_SPLIT_TAG = "splitTag";
    private static final String PARAMETER_NAME_FILTER_LANG = "filterLang";
    private static final String PARAMETER_NAME_LANG = "lang";
    private static final String PARAMETER_MODEL_NAME = "modelName";
    private static final String PARAMETER_PROPERTY_NAME = "propertyName";
    private static final String PARAMETER_PROPERTY_QUERY = "query";

    private static final String DEFAULT_PROPERTY_NAME = "word2vec";

    private static final String RELATIONSHIP_IS_RELATED_TO_SUB_TAG = "subTag";

    private final TextProcessorsManager processorManager;

    private static final Logger LOG = LoggerFactory.getLogger(Word2VecProcedure.class);
    private final Word2VecModel word2VecModel;
    private final GraphDatabaseService database;

    public Word2VecProcedure(GraphDatabaseService database, Word2VecModel word2VecModel, TextProcessorsManager processorManager) {
        this.word2VecModel = word2VecModel;
        this.database = database;
        this.processorManager = processorManager;
    }

    public CallableProcedure.BasicProcedure attachAll() {
        return new CallableProcedure.BasicProcedure(procedureSignature(getProcedureName("ml", "word2vec", "attach"))
                .mode(Mode.WRITE)
                .in(PARAMETER_NAME_INPUT, Neo4jTypes.NTMap)
                .out(PARAMETER_NAME_INPUT_OUTPUT, Neo4jTypes.NTInteger).build()) {

            @Override
            public RawIterator<Object[], ProcedureException> apply(Context ctx, Object[] input) throws ProcedureException {
                try {
                    checkIsMap(input[0]);
                    Map<String, Object> inputParams = (Map) input[0];
                    Node annotatedNode = (Node) inputParams.get(PARAMETER_NAME_ANNOTATED_TEXT);
                    Boolean splitTags = (Boolean) inputParams.getOrDefault(PARAMETER_NAME_SPLIT_TAG, false);
                    Boolean filterByLang = (Boolean) inputParams.getOrDefault(PARAMETER_NAME_FILTER_LANG, true);
                    String lang = (String) inputParams.getOrDefault(PARAMETER_NAME_LANG, DEFAULT_LANGUAGE);
                    String query = (String) inputParams.get(PARAMETER_PROPERTY_QUERY);
                    String modelName = (String) inputParams.get(PARAMETER_MODEL_NAME);
                    String propertyName = (String) inputParams.getOrDefault(PARAMETER_PROPERTY_NAME, DEFAULT_PROPERTY_NAME);
                    Node tagToBeAnnotated = null;
                    if (annotatedNode == null) {
                        tagToBeAnnotated = (Node) inputParams.get(PARAMETER_NAME_TAG);
                    }
                    Iterator<Node> tagsIterator;
                    if (annotatedNode != null) {
                        tagsIterator = getAnnotatedTextTags(annotatedNode);
                    } else if (tagToBeAnnotated != null) {
                        List<Node> proc = new ArrayList<>();
                        proc.add(tagToBeAnnotated);
                        tagsIterator = proc.iterator();
                    } else if (query != null) {
                        tagsIterator = getByQuery(query);
                    } else {
                        throw new RuntimeException("You need to specify or an annotated text or a list of tags");
                    }
                    TextProcessor processor = getProcessor(inputParams);
                    List<Tag> tags = new ArrayList<>();
                    while (tagsIterator.hasNext()) {
                        Tag tag = Tag.createTag(tagsIterator.next());
                        if (splitTags) {
                            List<Tag> annotateTags = processor.annotateTags(tag.getLemma(), lang);
                            if (annotateTags.size() == 1 && annotateTags.get(0).getLemma().equalsIgnoreCase(tag.getLemma())) {
                                tags.add(tag);
                            } else {
                                annotateTags.forEach((newTag) -> {
                                    tags.add(newTag);
                                    tag.addParent(RELATIONSHIP_IS_RELATED_TO_SUB_TAG, newTag, 0.0f);
                                });
                            }
                        } else {
                            tags.add(tag);
                        }
                    }
                    List<Tag> extendedTags = new ArrayList<>();
                    tags.stream().forEach((tag) -> {
                        LOG.info("Searching for: " + tag.getLemma().toLowerCase());
                        double[] vector = word2VecModel.getWordToVec(tag.getLemma().toLowerCase(), modelName);
                        if (vector != null) {
                            tag.addProperties(propertyName, vector);
                            extendedTags.add(tag);
                        }
                    });
                    AtomicInteger affectedTag = new AtomicInteger(0);
                    extendedTags.stream().forEach((newTag) -> {
                        if (newTag != null) {
                            newTag.storeOnGraph(database, true);
                            affectedTag.incrementAndGet();
                        }
                    });
                    return Iterators.asRawIterator(Collections.<Object[]>singleton(new Object[]{affectedTag.get()}).iterator());
                } catch (Exception ex) {
                    LOG.error("Error!!!! ", ex);
                    throw new RuntimeException("Error", ex);
                }

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

    private TextProcessor getProcessor(Map<String, Object> inputParams) throws RuntimeException {
        String processor = ((String) inputParams.getOrDefault(PARAMETER_NAME_TEXT_PROCESSOR, ""));
        if (processor.length() > 0) {
            TextProcessor textProcessorInstance = processorManager.getTextProcessor(processor);
            if (textProcessorInstance == null) {
                throw new RuntimeException("Text processor " + processor + " doesn't exist");
            }
            return textProcessorInstance;
        }
        return processorManager.getDefaultProcessor();
    }

    private ResourceIterator<Node> getAnnotatedTextTags(Node annotatedNode) throws QueryExecutionException {
        Map<String, Object> params = new HashMap<>();
        params.put("id", annotatedNode.getId());
        Result queryRes = database.execute("MATCH (n:AnnotatedText)-[*..2]->(t:Tag) where id(n) = {id} return distinct t", params);
        ResourceIterator<Node> tags = queryRes.columnAs("t");
        return tags;
    }

    private ResourceIterator<Node> getByQuery(String query) throws QueryExecutionException {
        Map<String, Object> params = new HashMap<>();
        Result queryRes = database.execute(query, params);
        ResourceIterator<Node> tags = queryRes.columnAs("t");
        return tags;
    }
}
