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
package com.graphaware.nlp.application.search;

import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.procedure.NLPProcedure;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.processor.stanford.StanfordTextProcessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.neo4j.collection.RawIterator;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.helpers.collection.Iterators;
import org.neo4j.kernel.api.exceptions.ProcedureException;
import org.neo4j.kernel.api.proc.CallableProcedure;
import org.neo4j.kernel.api.proc.Neo4jTypes;
import org.neo4j.kernel.api.proc.ProcedureSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.neo4j.kernel.api.proc.ProcedureSignature.procedureSignature;

public class SearchProcedure extends NLPProcedure {

    private static final Logger LOG = LoggerFactory.getLogger(SearchProcedure.class);

    private final TextProcessor textProcessor;
    private final GraphDatabaseService database;

    private static final String PARAMETER_NAME_SCORE = "score";

    public SearchProcedure(GraphDatabaseService database) {
        this.database = database;
        this.textProcessor = new StanfordTextProcessor();
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
                AnnotatedText annotateText = textProcessor.annotateText(text, 0, 0, false);
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
}
