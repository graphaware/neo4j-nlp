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

import com.graphaware.test.integration.GraphAwareIntegrationTest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

public class ProcedureTest extends GraphAwareIntegrationTest {

    private static final String TEXT = "On 8 May 2013, "
            + "one week before the Pakistani election, the third author, "
            + "in his keynote address at the Sentiment Analysis Symposium, "
            + "forecast the winner of the Pakistani election. The chart "
            + "in Figure 1 shows varying sentiment on the candidates for "
            + "prime minister of Pakistan in that election. The next day, "
            + "the BBC’s Owen Bennett Jones, reporting from Islamabad, wrote "
            + "an article titled “Pakistan Elections: Five Reasons Why the "
            + "Vote is Unpredictable,”1 in which he claimed that the election "
            + "was too close to call. It was not, and despite his being in Pakistan, "
            + "the outcome of the election was exactly as we predicted.";
    
    @Test
    public void testAnnotatedText() {
        try (Transaction tx = getDatabase().beginTx()) {
            String id = "id1";
            Map<String, Object> params = new HashMap<>();
            params.put("value", TEXT);
            params.put("id", id);
            Result news = getDatabase().execute("MERGE (n:News {text: {value}}) WITH n\n"
                    + "CALL ga.nlp.annotate({text:n.text, id: {id}}) YIELD result\n"
                    + "MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result)\n"
                    + "return result", params);
            ResourceIterator<Object> rowIterator = news.columnAs("result");
            assertTrue(rowIterator.hasNext());
            Node resultNode = (Node) rowIterator.next();
            assertEquals(resultNode.getProperty("id"), id);
            params.clear();
            params.put("id", id);
            Result tags = getDatabase().execute("MATCH (a:AnnotatedText {id: {id}})-[:CONTAINS_SENTENCE]->(s:Sentence)-[:HAS_TAG]->(result:Tag) RETURN result", params);
            rowIterator = tags.columnAs("result");
            assertTrue(rowIterator.hasNext());
            
            
            Result sentences = getDatabase().execute("MATCH (a:AnnotatedText {id: {id}})-[:CONTAINS_SENTENCE]->(s:Sentence) RETURN labels(s) as result", params);
            rowIterator = sentences.columnAs("result");
            assertTrue(rowIterator.hasNext());
            while (rowIterator.hasNext()) {
                List<Object> next = (List)rowIterator.next();
                assertEquals(next.size(), 1);
            }
            tx.success();
        }
    }
    
    @Test
    public void testAnnotatedTextWithSentiment() {
        try (Transaction tx = getDatabase().beginTx()) {
            String id = "id1";
            Map<String, Object> params = new HashMap<>();
            params.put("value", TEXT);
            params.put("id", id);
            Result news = getDatabase().execute("MERGE (n:News {text: {value}}) WITH n\n"
                    + "CALL ga.nlp.annotate({text:n.text, id: {id}, sentiment: true}) YIELD result\n"
                    + "MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result)\n"
                    + "return result", params);
            ResourceIterator<Object> rowIterator = news.columnAs("result");
            assertTrue(rowIterator.hasNext());
            Node resultNode = (Node) rowIterator.next();
            assertEquals(resultNode.getProperty("id"), id);
            params.clear();
            params.put("id", id);
            Result sentences = getDatabase().execute("MATCH (a:AnnotatedText {id: {id}})-[:CONTAINS_SENTENCE]->(s:Sentence) RETURN labels(s) as result", params);
            rowIterator = sentences.columnAs("result");
            assertTrue(rowIterator.hasNext());
            while (rowIterator.hasNext()) {
                List<Object> next = (List)rowIterator.next();
                assertEquals(next.size(), 2);
            }
            tx.success();
        }
    }
    
}
