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
package com.graphaware.nlp.ml.lda;

import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.procedure.NLPProcedure;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.processor.TextProcessorsManager;
import com.graphaware.spark.ml.lda.LDAProcessor;
import java.util.ArrayList;
import java.util.Collection;
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
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.helpers.collection.Iterators;
import org.neo4j.kernel.api.exceptions.ProcedureException;
import org.neo4j.kernel.api.proc.CallableProcedure;
import org.neo4j.kernel.api.proc.Neo4jTypes;
import org.neo4j.kernel.api.proc.ProcedureSignature;
import org.neo4j.kernel.api.proc.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;
import static org.neo4j.kernel.api.proc.ProcedureSignature.procedureSignature;
import org.neo4j.procedure.Mode;

public class LDAProcedure extends NLPProcedure {

    private static final Logger LOG = LoggerFactory.getLogger(LDAProcedure.class);

    private final GraphDatabaseService database;
    private final TextProcessorsManager processorManager;
    private final TextProcessor textProcessor;

    private final static long PARAMETER_DEFAULT_GROUPS = 5l;
    private final static long PARAMETER_DEFAULT_ITERATIONS = 20l;
    private final static long PARAMETER_DEFAULT_TOPICS = 10l;
    private final static boolean PARAMETER_DEFAULT_STORE = true;
    private final static boolean PARAMETER_DEFAULT_CONCEPT = false;

    private final static String PARAMETER_NAME_GROUPS = "clusters";
    private final static String PARAMETER_NAME_ITERATIONS = "iterations";
    private final static String PARAMETER_NAME_TOPICS = "topics";
    private final static String PARAMETER_NAME_STORE = "store";
    private final static String PARAMETER_NAME_CONCEPT = "concept";
    private final static String PARAMETER_NAME_TEXT = "text";

    public LDAProcedure(GraphDatabaseService database, TextProcessorsManager processorManager) {
        this.database = database;
        this.processorManager = processorManager;
        this.textProcessor = processorManager.getDefaultProcessor();
    }

    public CallableProcedure.BasicProcedure lda() {
        return new CallableProcedure.BasicProcedure(procedureSignature(getProcedureName("ml", "lda"))
                .mode(Mode.WRITE)
                .in(PARAMETER_NAME_INPUT, Neo4jTypes.NTMap)
                .out(PARAMETER_NAME_INPUT_OUTPUT, Neo4jTypes.NTInteger).build()) {

            @Override
            public RawIterator<Object[], ProcedureException> apply(Context ctx, Object[] input) throws ProcedureException {
                checkIsMap(input[0]);
                Map<String, Object> inputParams = (Map) input[0];
                Long numberOfTopicGroups = (Long) inputParams.getOrDefault(PARAMETER_NAME_GROUPS, PARAMETER_DEFAULT_GROUPS);
                Long maxIterations = (Long) inputParams.getOrDefault(PARAMETER_NAME_ITERATIONS, PARAMETER_DEFAULT_ITERATIONS);
                Long numberOfTopics = (Long) inputParams.getOrDefault(PARAMETER_NAME_TOPICS, PARAMETER_DEFAULT_TOPICS);
                Boolean storeModel = (Boolean) inputParams.getOrDefault(PARAMETER_NAME_STORE, PARAMETER_DEFAULT_STORE);
                Boolean concept = (Boolean) inputParams.getOrDefault(PARAMETER_NAME_CONCEPT, PARAMETER_DEFAULT_CONCEPT);

                try {
                    LOG.warn("Start extracting topic");
                    String query;
                    if (!concept) {
                        query = "MATCH (n:AnnotatedText) "
                                + "MATCH (n)-[:CONTAINS_SENTENCE]->(s:Sentence)-[r:HAS_TAG]->(t:Tag) "
                                + "WHERE size(t.value) > 5 "
                                + "return id(n) as docId, sum(r.tf) as tf, t.value as word";
                    } else {
                        query = "MATCH (n:AnnotatedText)\n"
                                + "MATCH (n)-[:CONTAINS_SENTENCE]->(s:Sentence)-[r:HAS_TAG]->(t:Tag)\n"
                                + "OPTIONAL MATCH (t)-[:IS_RELATED]->(rt)\n"
                                + "WHERE size(t.value) > 5\n"
                                + "WITH id(n) as docId, sum(r.tf) as tf, t.value + collect(rt.value) as tags\n"
                                + "UNWIND tags as tag\n"
                                + "WITH docId, tf, tag as word\n"
                                + "where size(word) > 5\n"
                                + "return docId, tf, word";
                    }
                    Tuple2<Object, Tuple2<String, Object>[]>[] topics = LDAProcessor.extract(query, numberOfTopicGroups.intValue(), maxIterations.intValue(), numberOfTopics.intValue(), storeModel);
                    Collection<Node> resultNodes = storeTopics(topics);
                    LOG.warn("Completed extracting topic");
                    Set<Object[]> result = new HashSet<>();
                    resultNodes.stream().forEach((item) -> {
                        result.add(new Object[]{item});
                    });
                    return Iterators.asRawIterator(result.iterator());
                } catch (Exception ex) {
                    LOG.error("Error while annotating", ex);
                    throw new RuntimeException(ex);
                }
            }

            private Collection<Node> storeTopics(Tuple2<Object, Tuple2<String, Object>[]>[] topicsAssociation) {
                Map<Long, Node> topicNodes = new HashMap<>();
                database.execute("MATCH (t:Topic) DETACH DELETE t");

                for (Tuple2<Object, Tuple2<String, Object>[]> document : topicsAssociation) {
                    long docId = (Long) document._1;
                    Tuple2<String, Object>[] topics = document._2;
                    Node topic = getOrCreateTopic(topics);
                    if (topic == null) {
                        throw new RuntimeException("Cannot create new Topic");
                    }
                    Map<String, Object> internalParam = new HashMap<>();
                    internalParam.put("docId", docId);
                    internalParam.put("topicId", topic.getId());
                    database.execute("MATCH (t:Topic) "
                            + "MATCH (a:AnnotatedText) "
                            + "WHERE id(t) = {topicId} AND id(a) = {docId} "
                            + "MERGE (a)<-[:DESCRIBES]-(t) "
                            + "RETURN t",
                            internalParam);
                    if (!topicNodes.containsKey(topic.getId())) {
                        topicNodes.put(topic.getId(), topic);
                    }
                }
                return topicNodes.values();
            }
        };
    }

    public CallableProcedure.BasicProcedure topicDistribution() {
        return new CallableProcedure.BasicProcedure(procedureSignature(getProcedureName("ml", "topic"))
                .mode(Mode.WRITE)
                .in(PARAMETER_NAME_INPUT, Neo4jTypes.NTMap)
                .out(PARAMETER_NAME_INPUT_OUTPUT, Neo4jTypes.NTNode)
                .build()) {

            @Override
            public RawIterator<Object[], ProcedureException> apply(Context ctx, Object[] input) throws ProcedureException {
                checkIsMap(input[0]);
                Map<String, Object> inputParams = (Map) input[0];
                String text = (String) inputParams.get(PARAMETER_NAME_TEXT);
                if (text == null) {
                    throw new RuntimeException("Missing parameter " + PARAMETER_NAME_TEXT);
                }
                Integer numberOfTopics = (Integer) inputParams.getOrDefault(PARAMETER_NAME_TOPICS, PARAMETER_DEFAULT_TOPICS);
                AnnotatedText annotateText = textProcessor.annotateText(text, 0, 0, "en", false);
                List<Tag> tags = annotateText.getTags();
                Tuple2<String, Object>[] tagsArray = new Tuple2[tags.size()];
                for (int i = 0; i < tags.size(); i++) {
                    Tag tag = tags.get(i);
                    tagsArray[i] = new Tuple2<>(tag.getLemma(), tag.getMultiplicity());
                }
                try {
                    LOG.warn("Start extracting topic");
                    Tuple2<String, Object>[] topics = LDAProcessor.predictTopics(tagsArray, numberOfTopics);
                    LOG.warn("Completed extracting topic: " + topics);
                    Node topicNode = getOrCreateTopic(topics);
                    return Iterators.asRawIterator(Collections.<Object[]>singleton(new Object[]{topicNode}).iterator());
                } catch (Exception ex) {
                    LOG.error("Error while annotating", ex);
                    throw new RuntimeException(ex);
                }
            }
        };
    }

    private Node getOrCreateTopic(Tuple2<String, Object>[] topics) {
        List<String> keywords = new ArrayList<>();
        List<Float> values = new ArrayList<>();

        for (Tuple2<String, Object> topic : topics) {
            keywords.add(topic._1);
            values.add(Float.valueOf(String.valueOf(topic._2)));
        }
        Map<String, Object> internalParam = new HashMap<>();
        internalParam.put("keywords", keywords);
        internalParam.put("values", values);
        Result topicResult = database.execute("MERGE (t:Topic {keywords:{keywords}, values:{values}}) \n"
                + "WITH t, range(0, size(t.keywords) - 1) as indexes\n"
                + "UNWIND indexes as i\n"
                + "MERGE (tag:Tag {value: t.keywords[i]}) \n"
                + "MERGE (t)<-[:DESCRIBES {value: t.values[i]}]-(tag) \n"
                + "RETURN t",
                internalParam);
        ResourceIterator<Node> topic = topicResult.columnAs("t");
        if (topic.hasNext()) {
            return topic.next();
        } else {
            return null;
        }
    }
}
