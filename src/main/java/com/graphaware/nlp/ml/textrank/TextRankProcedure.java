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
package com.graphaware.nlp.ml.textrank;

import com.graphaware.nlp.procedure.NLPProcedure;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import org.neo4j.collection.RawIterator;
import org.neo4j.graphdb.Node;
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

public class TextRankProcedure extends NLPProcedure {

    private static final Logger LOG = LoggerFactory.getLogger(TextRankProcedure.class);

    private final static String PARAMETER_NAME_NODE = "node";
    private final static String PARAMETER_NAME_SCORE = "score";
    private final static String PARAMETER_NAME_NODEW = "node_w";

    private final PageRankAlg pagerank;
    private final TextRankAlg textrank;
    //private final static String PARAMETER_NAME_QUERY = "query";
    private final static String PARAMETER_ANNOTATED_ID = "annotatedID";
    //private final static String PARAMETER_NODE_TYPE = "nodeType";
    private final static String PARAMETER_RELATIONSHIP_TYPE = "relationshipType";
    private final static String PARAMETER_RELATIONSHIP_WEIGHT = "relationshipWeight";
    private final static String PARAMETER_ITER = "iter";
    private final static String PARAMETER_DAMPING_FACTOR = "damp";
    private final static String PARAMETER_STOPWORDS = "stopwords";
    private final static String PARAMETER_DO_STOPWORDS = "removeStopWords";

    public TextRankProcedure(GraphDatabaseService database) {
      this.pagerank = new PageRankAlg(database);
      this.textrank = new TextRankAlg(database);
    }

    public CallableProcedure.BasicProcedure computePageRank() {
        return new CallableProcedure.BasicProcedure(procedureSignature(getProcedureName("ml", "textrank", "computePageRank"))
                .mode(Mode.WRITE)
                .in(PARAMETER_NAME_INPUT, Neo4jTypes.NTAny)
                .out(PARAMETER_NAME_NODE, Neo4jTypes.NTNode)
                .out(PARAMETER_NAME_SCORE, Neo4jTypes.NTFloat)
                .out(PARAMETER_NAME_NODEW, Neo4jTypes.NTFloat).build()) {

            @Override
            public RawIterator<Object[], ProcedureException> apply(Context ctx, Object[] input) throws ProcedureException {
                Map<String, Object> inputParams = (Map) input[0];
                if (!inputParams.containsKey(PARAMETER_ANNOTATED_ID)) {
                    LOG.error("Missing parameter \'" + PARAMETER_ANNOTATED_ID + "\'");
                    return Iterators.asRawIterator(Collections.<Object[]>singleton(new String[]{"failure"}).iterator());
                }
                Long annotatedID = (Long) inputParams.get(PARAMETER_ANNOTATED_ID);
                //String nodeType = (String) inputParams.getOrDefault(PARAMETER_NODE_TYPE, "Tag")
                String relType = (String) inputParams.getOrDefault(PARAMETER_RELATIONSHIP_TYPE, "CO_OCCURRENCE");
                String relWeight = (String) inputParams.getOrDefault(PARAMETER_RELATIONSHIP_WEIGHT, "weight");
                int iter = (int) inputParams.getOrDefault(PARAMETER_ITER, 30);
                double damp = (double) inputParams.getOrDefault(PARAMETER_DAMPING_FACTOR, 0.85);

                Set<Object[]> result = pagerank.run(annotatedID, /*nodeType,*/ relType, relWeight, iter, damp);

                return Iterators.asRawIterator(result.iterator());
            }
        };
    }

    public CallableProcedure.BasicProcedure compute() {
        return new CallableProcedure.BasicProcedure(procedureSignature(getProcedureName("ml", "textrank", "compute"))
                .mode(Mode.WRITE)
                .in(PARAMETER_NAME_INPUT, Neo4jTypes.NTAny)
                .out("result", Neo4jTypes.NTString).build()) {

            @Override
            public RawIterator<Object[], ProcedureException> apply(Context ctx, Object[] input) throws ProcedureException {
                Map<String, Object> inputParams = (Map) input[0];
                if (!inputParams.containsKey(PARAMETER_ANNOTATED_ID)) {
                    LOG.error("Missing parameter \'" + PARAMETER_ANNOTATED_ID + "\'");
                    return Iterators.asRawIterator(Collections.<Object[]>singleton(new String[]{"failure"}).iterator());
                }
                Long annotatedID = (Long) inputParams.get(PARAMETER_ANNOTATED_ID);
                int iter = (int) inputParams.getOrDefault(PARAMETER_ITER, 30);
                double damp = (double) inputParams.getOrDefault(PARAMETER_DAMPING_FACTOR, 0.85);
                boolean doStopwords = (boolean) inputParams.getOrDefault(PARAMETER_DO_STOPWORDS, false);
                String relType = "CO_OCCURRENCE_TR2";
                String relWeight = "weight";

                if (inputParams.containsKey(PARAMETER_STOPWORDS))
                    textrank.setStopwords((String) inputParams.get(PARAMETER_STOPWORDS));
                textrank.removeStopWords(doStopwords);

                String resReport = "success";
                boolean res = false;
                if ( textrank.createCooccurrences(annotatedID, relType, relWeight) )
                    res = textrank.evaluate(annotatedID, relType, relWeight, iter, damp);
                boolean res2 = textrank.deleteCooccurrences(annotatedID, relType);

                if (!res || !res2)
                    resReport = "failure";

                LOG.info("AnnotatedText with ID " + annotatedID + " processed.");

                return Iterators.asRawIterator(Collections.<Object[]>singleton(new Object[]{resReport}).iterator());
            }
        };
    }

}
