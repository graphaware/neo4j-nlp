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

    private static final String SHORT_TEXT_1 = "You knew China's cities were growing. But the real numbers are stunning http://wef.ch/29IxY7w  #China";
    private static final String SHORT_TEXT_2 = "Globalization for the 99%: can we make it work for all?";
    private static final String SHORT_TEXT_3 = "This organisation increased productivity, happiness and trust with just one change http://wef.ch/29PeKxF ";
    private static final String SHORT_TEXT_4 = "In pictures: The high-tech villages that live off the grid http://wef.ch/29xuRh8 ";
    private static final String SHORT_TEXT_5 = "The 10 countries best prepared for the new digital economy http://wef.ch/2a8DNug ";
    private static final String SHORT_TEXT_6 = "This is how to limit damage to the #euro after #Brexit, say economists http://wef.ch/29GGVzG ";
    private static final String SHORT_TEXT_7 = "The office jobs that could see you earning nearly 50% less than some of your co-workers http://wef.ch/29P9biE ";
    private static final String SHORT_TEXT_8 = "Which nationalities have the best quality of life? http://wef.ch/29uDfwV";
    private static final String SHORT_TEXT_9 = "It’s 9,000km away, but #Brexit has hit #Japan hard http://wef.ch/29P92eQ  #economics";
    private static final String SHORT_TEXT_10 = "Which is the world’s fastest-growing large economy? Clue: it’s not #China http://wef.ch/29xuXFd  #economics";

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
                List<Object> next = (List) rowIterator.next();
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
                List<Object> next = (List) rowIterator.next();
                assertEquals(next.size(), 2);
            }
            tx.success();
        }
    }

    @Test
    public void testAnnotatedTextAndSentiment() {
        try (Transaction tx = getDatabase().beginTx()) {
            String id = "id1";
            Map<String, Object> params = new HashMap<>();
            params.put("value", TEXT);
            params.put("id", id);
            Result news = getDatabase().execute("MERGE (n:News {text: {value}}) WITH n\n"
                    + "CALL ga.nlp.annotate({text:n.text, id: {id}, store: true}) YIELD result\n"
                    + "MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result)\n"
                    + "return result", params);
            ResourceIterator<Object> rowIterator = news.columnAs("result");
            assertTrue(rowIterator.hasNext());
            Node resultNode = (Node) rowIterator.next();
            assertEquals(resultNode.getProperty("id"), id);
            params.clear();
            params.put("id", id);
            Result sentences = getDatabase().execute("MATCH (a:AnnotatedText {id: {id}}) WITH a "
                    + "CALL ga.nlp.sentiment({node:a}) YIELD result "
                    + "MATCH (result)-[:CONTAINS_SENTENCE]->(s:Sentence) "
                    + "return labels(s) as labels", params);
            rowIterator = sentences.columnAs("labels");
            assertTrue(rowIterator.hasNext());
            int i = 0;
            while (rowIterator.hasNext()) {
                List<Object> next = (List) rowIterator.next();
                assertEquals(next.size(), 2);
                i++;
            }
            assertEquals(4, i);
            //Execute again for checking the number of senteces
            sentences = getDatabase().execute("MATCH (a:AnnotatedText {id: {id}}) WITH a "
                    + "CALL ga.nlp.sentiment({node:a}) YIELD result "
                    + "MATCH (result)-[:CONTAINS_SENTENCE]->(s:Sentence) "
                    + "return labels(s) as labels", params);
            rowIterator = sentences.columnAs("labels");
            assertTrue(rowIterator.hasNext());
            i = 0;
            while (rowIterator.hasNext()) {
                List<Object> next = (List) rowIterator.next();
                assertEquals(next.size(), 2);
                i++;
            }
            assertEquals(4, i);
            tx.success();
        }
    }

    @Test
    public void testAnnotatedTextOnMultiple() {
        try (Transaction tx = getDatabase().beginTx()) {
            String id = "id1";
            Map<String, Object> params = new HashMap<>();
            params.put("value", SHORT_TEXT_1);
            getDatabase().execute("MERGE (n:Tweet {text: {value}})", params);

            params.put("value", SHORT_TEXT_2);
            getDatabase().execute("MERGE (n:Tweet {text: {value}})", params);

            params.put("value", SHORT_TEXT_3);
            getDatabase().execute("MERGE (n:Tweet {text: {value}})", params);

            params.put("value", SHORT_TEXT_4);
            getDatabase().execute("MERGE (n:Tweet {text: {value}})", params);

            params.put("value", SHORT_TEXT_5);
            getDatabase().execute("MERGE (n:Tweet {text: {value}})", params);

            params.put("value", SHORT_TEXT_6);
            getDatabase().execute("MERGE (n:Tweet {text: {value}})", params);

            params.put("value", SHORT_TEXT_7);
            getDatabase().execute("MERGE (n:Tweet {text: {value}})", params);

            params.put("value", SHORT_TEXT_8);
            getDatabase().execute("MERGE (n:Tweet {text: {value}})", params);

            params.put("value", SHORT_TEXT_9);
            getDatabase().execute("MERGE (n:Tweet {text: {value}})", params);

            params.put("value", SHORT_TEXT_10);
            getDatabase().execute("MERGE (n:Tweet {text: {value}})", params);

            getDatabase().execute("MERGE (n:Tweet {id:1})", params);

            Result sentences = getDatabase().execute("MATCH (a:Tweet) WITH a\n"
                    + "WITH collect(a) AS aa\n"
                    + "UNWIND aa AS a\n"
                    + "CALL ga.nlp.annotate({text:a.text, id: id(a)}) YIELD result WITH result as at "
                    + "MERGE (a)-[:HAS_ANNOTATED_TEXT]->(at) WITH at "
                    + "MATCH (at)-[:CONTAINS_SENTENCE]->(result) "
                    + "RETURN result", params);
            ResourceIterator<Object> rowIterator = sentences.columnAs("result");
            assertTrue(rowIterator.hasNext());
            int i = 0;
            while (rowIterator.hasNext()) {
                rowIterator.next();
                i++;
            }
            assertEquals(13, i);
            tx.success();
        }
    }

    @Test
    public void testConceptText() {
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
            Result tags = getDatabase().execute(
                    "MATCH (a:AnnotatedText) "
                    + "CALL ga.nlp.concept({node:a, depth: 2}) YIELD result\n"
                    + "return result;", params);
            rowIterator = tags.columnAs("result");
            assertTrue(rowIterator.hasNext());
            tx.success();
        }
    }
}
