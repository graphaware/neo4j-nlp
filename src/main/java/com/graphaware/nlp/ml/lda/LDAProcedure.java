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
package com.graphaware.nlp.ml.lda;

import com.graphaware.nlp.ml.client.SparkRestClient;
import com.graphaware.nlp.module.NLPModule;
import com.graphaware.nlp.procedure.NLPProcedure;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.processor.TextProcessorsManager;
import com.graphaware.runtime.GraphAwareRuntime;
import com.graphaware.runtime.RuntimeRegistry;
import java.util.Collections;
import java.util.Map;
import org.neo4j.collection.RawIterator;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.helpers.collection.Iterators;
import org.neo4j.kernel.api.exceptions.ProcedureException;
import org.neo4j.kernel.api.proc.CallableProcedure;
import org.neo4j.kernel.api.proc.Neo4jTypes;
import org.neo4j.kernel.api.proc.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.neo4j.kernel.api.proc.ProcedureSignature.procedureSignature;
import org.neo4j.procedure.Mode;

public class LDAProcedure extends NLPProcedure {

    private static final Logger LOG = LoggerFactory.getLogger(LDAProcedure.class);

    private final GraphDatabaseService database;
    private final TextProcessorsManager processorManager;
    private final TextProcessor textProcessor;
    
    private LDARestClient ldaRestClient; 

    private final static long PARAMETER_DEFAULT_GROUPS = 5l;
    private final static long PARAMETER_DEFAULT_ITERATIONS = 20l;
    private final static long PARAMETER_DEFAULT_TOPICS = 10l;
    private final static boolean PARAMETER_DEFAULT_STORE = true;
    private final static boolean PARAMETER_DEFAULT_CONCEPT = false;
    private final static String PARAMETER_DEFAULT_TOPIC_LABEL = "LDATopic";

    private final static String PARAMETER_NAME_GROUPS = "clusters";
    private final static String PARAMETER_NAME_ITERATIONS = "iterations";
    private final static String PARAMETER_NAME_TOPICS = "topics";
    private final static String PARAMETER_NAME_STORE = "store";
    private final static String PARAMETER_NAME_CONCEPT = "concept";
    private final static String PARAMETER_NAME_TEXT = "text";
    private final static String PARAMETER_NAME_QUERY = "query";
    private final static String PARAMETER_NAME_TOPIC_LABEL = "label";

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
                Integer numberOfTopicGroups = (Integer) inputParams.getOrDefault(PARAMETER_NAME_GROUPS, PARAMETER_DEFAULT_GROUPS);
                Integer maxIterations = (Integer) inputParams.getOrDefault(PARAMETER_NAME_ITERATIONS, PARAMETER_DEFAULT_ITERATIONS);
                Integer numberOfTopics = (Integer) inputParams.getOrDefault(PARAMETER_NAME_TOPICS, PARAMETER_DEFAULT_TOPICS);
                Boolean storeModel = (Boolean) inputParams.getOrDefault(PARAMETER_NAME_STORE, PARAMETER_DEFAULT_STORE);
                Boolean concept = (Boolean) inputParams.getOrDefault(PARAMETER_NAME_CONCEPT, PARAMETER_DEFAULT_CONCEPT);
                String query = (String) inputParams.getOrDefault(PARAMETER_NAME_QUERY, null);
                String topicLabel = (String) inputParams.getOrDefault(PARAMETER_NAME_TOPIC_LABEL, PARAMETER_DEFAULT_TOPIC_LABEL);

                try {
                    LOG.warn("Start extracting topic");
                    if (query != null) {
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
                    }
                    LOG.warn("Start extracting topics");
                    LDARequest request = new LDARequest();
                    request.setQuery(query);
                    request.setClusters(numberOfTopicGroups);
                    request.setItarations(maxIterations);
                    request.setTopicWords(numberOfTopics);
                    request.setStoreModel(storeModel);
                    request.setTopicLabel(topicLabel);
                    LDAResponse process = getLDARestClient().computeLDA(request);
                    return Iterators.asRawIterator(Collections.<Object[]>singleton(new Object[]{process.getProcessId()}).iterator());
                } catch (Exception ex) {
                    LOG.error("Error while annotating", ex);
                    throw new RuntimeException(ex);
                }
            }
        };
    }

//    public CallableProcedure.BasicProcedure topicDistribution() {
//        return new CallableProcedure.BasicProcedure(procedureSignature(getProcedureName("ml", "topic"))
//                .mode(Mode.WRITE)
//                .in(PARAMETER_NAME_INPUT, Neo4jTypes.NTMap)
//                .out(PARAMETER_NAME_INPUT_OUTPUT, Neo4jTypes.NTNode)
//                .build()) {
//
//            @Override
//            public RawIterator<Object[], ProcedureException> apply(Context ctx, Object[] input) throws ProcedureException {
//                checkIsMap(input[0]);
//                Map<String, Object> inputParams = (Map) input[0];
//                String text = (String) inputParams.get(PARAMETER_NAME_TEXT);
//                if (text == null) {
//                    throw new RuntimeException("Missing parameter " + PARAMETER_NAME_TEXT);
//                }
//                Integer numberOfTopics = (Integer) inputParams.getOrDefault(PARAMETER_NAME_TOPICS, PARAMETER_DEFAULT_TOPICS);
//                AnnotatedText annotateText = textProcessor.annotateText(text, 0, 0, "en", false);
//                List<Tag> tags = annotateText.getTags();
//                Tuple2<String, Object>[] tagsArray = new Tuple2[tags.size()];
//                for (int i = 0; i < tags.size(); i++) {
//                    Tag tag = tags.get(i);
//                    tagsArray[i] = new Tuple2<>(tag.getLemma(), tag.getMultiplicity());
//                }
//                try {
//                    LOG.warn("Start extracting topic");
//                    Tuple2<String, Object>[] topics = LDAProcessor.predictTopics(tagsArray, numberOfTopics);
//                    LOG.warn("Completed extracting topic: " + topics);
//                    Node topicNode = getOrCreateTopic(topics);
//                    return Iterators.asRawIterator(Collections.<Object[]>singleton(new Object[]{topicNode}).iterator());
//                } catch (Exception ex) {
//                    LOG.error("Error while annotating", ex);
//                    throw new RuntimeException(ex);
//                }
//            }
//        };
//    }
    
    public LDARestClient getLDARestClient() {
        if (ldaRestClient == null) {
            GraphAwareRuntime runtime = RuntimeRegistry.getStartedRuntime(database);
            LOG.error(">>>>>>>: " + runtime.getModule("NLP", NLPModule.class).getClass().toString());
            String url = runtime.getModule(NLPModule.class).getNlpMLConfiguration().getSparkRestUrl();
            this.ldaRestClient = new LDARestClient(new SparkRestClient(url));
        }
        return ldaRestClient;
    }
}
