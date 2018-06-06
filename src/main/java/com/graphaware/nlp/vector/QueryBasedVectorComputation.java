/*
 * Copyright (c) 2013-2018 GraphAware
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
package com.graphaware.nlp.vector;

import com.graphaware.nlp.annotation.NLPVectorComputationProcess;
import static com.graphaware.nlp.util.TypeConverter.getFloatValue;
import java.util.HashMap;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.Result;

@NLPVectorComputationProcess(name = "QueryBasedVectorComputation")
public class QueryBasedVectorComputation implements VectorComputation {
    private static final String TYPE = "query";
    
    private static final String QUERY_PARAMETER = "query";

    private GraphDatabaseService database;

    public final static String DEFAULT_VECTOR_QUERY = "MATCH (doc:AnnotatedText)\n"
            + "WITH count(doc) as documentsCount\n"
            + "MATCH (input:AnnotatedText)-[:CONTAINS_SENTENCE]->(s:Sentence)-[ht:HAS_TAG]->(tag:Tag)\n"
            + "WHERE id(input) = {id}\n"
            + "MATCH (tag)<-[:HAS_TAG]-(:Sentence)<-[:CONTAINS_SENTENCE]-(document:AnnotatedText)\n"
            + "WITH tag, ht.tf as tf, count(distinct document) as documentsCountForTag, documentsCount\n"
            + "RETURN distinct id(tag) as tagId, sum(tf) as tf, (1.0f + 1.0f*documentsCount)/documentsCountForTag as idf";

    @Override
    public VectorHandler computeSparseVector(long node, Map<String, Object> parameters) throws QueryExecutionException {
        String query = (String) parameters.getOrDefault(QUERY_PARAMETER, DEFAULT_VECTOR_QUERY);
        Map<Long, Float> fmap;
        fmap = createFeatureMap(node, query);
        GenericVector vector = SparseVector.fromMap(fmap);
        return new VectorHandler(vector);
    }

    private Map<Long, Float> createFeatureMap(long firstNode, String query) throws QueryExecutionException {
        Map<String, Object> params = new HashMap<>();
        params.put("id", firstNode);
        Result res = database.execute(query, params);
        Map<Long, Float> result = new HashMap<>();
        while (res != null && res.hasNext()) {
            Map<String, Object> next = res.next();
            long id = (long) next.get("tagId");
            float tf = getFloatValue(next.get("tf"));
            float idf = Double.valueOf(Math.log10(Float.valueOf(getFloatValue(next.get("idf"))).doubleValue())).floatValue();
            result.put(id, tf * idf);
        }
        return result;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void setDatabase(GraphDatabaseService database) {
        this.database = database;
    }

    @Override
    public void train(Map<String, Object> parameters) {
        throw new RuntimeException("The selected VectorComputation doesn't support train."); //To change body of generated methods, choose Tools | Templates.
    }
}
