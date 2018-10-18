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

import com.graphaware.nlp.NLPIntegrationTest;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import com.graphaware.nlp.stub.StubTextProcessor;
import com.graphaware.nlp.util.ImportUtils;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.junit.Test;

import static org.junit.Assert.*;

public class TextRankTest extends NLPIntegrationTest {

    private static final List<String> expectedKeywords = Arrays.asList("flight failure", "speed brake", "space shuttle", "ground operation", "actuator", "installation", "flight", "gear", "shuttle", "brake", "speed", "failure", "unusual", "design");
    private static final String TEXT1 = "On 8 May 2013, "
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

    /**
     * Test of TextRank procedure, class TextRank.
     */
    @Test
    public void testTextRank() throws Exception {
        clearDb();
        createGraph("exported.cypher");

        // Run TextRank
        try (Transaction tx = getDatabase().beginTx()) {
            Result result = getDatabase().execute("match (a:AnnotatedText) return a");
            assertTrue("TextRank: didn't find AnnotatedText (error in graph initialization).", result.hasNext());
            if (!result.hasNext()) {
                return;
            }
            Node annText = (Node) result.next().get("a");
            TextRank textrank = new TextRank.Builder(getDatabase(), getNLPManager().getConfiguration())
                    .setTopXTags(1.0f / 3)
                    .build();
            assertNotNull("AnnotatedText not found.", annText);
            assertNotNull("TextRank.Builder failed: textrank is null", textrank);
            TextRankResult res = textrank.evaluate(Arrays.asList(new Node[] {annText}), 30, 0.85, 0.0001);

            // Store TextRank result
            TextRankPersister persister = new TextRankPersister(Label.label("Keyword"));
            persister.peristKeywords(res.getResult(), annText);
            assertTrue("TextRank failed, returned false.", res.getStatus().equals(TextRankResult.TextRankStatus.SUCCESS));
            tx.success();
        }

        // evaluate results
        try (Transaction tx = getDatabase().beginTx()) {
            Result result = getDatabase().execute(
                    "MATCH (k:Keyword)-[:DESCRIBES]->(a:AnnotatedText)\n"
                    + "RETURN k.id AS id, k.value AS value\n");
            int totCount = 0;
            int totCount_phrases = 0;
            int trueCount = 0;
            while (result != null && result.hasNext()) {
                Map<String, Object> next = result.next();
                String tag = next.get("value").toString();
                totCount++;
                if (tag.split(" ").length > 1) {
                    totCount_phrases++;
                }
                if (expectedKeywords.contains(tag)) {
                    trueCount++;
                }
            }
            assertTrue("TextRank evaluation: didn't find any keyword!", totCount > 0);
            assertTrue("TextRank evaluation: didn't find any keyphrase!", totCount_phrases > 0);
            assertTrue("TextRank evaluation: didn't find any expected keyword!", trueCount > 0);
            tx.success();
        } catch (Exception e) {
            assertTrue("Evaluation of TextRank results failed: " + e.getMessage(), true);
            return;
        }

    }

    @Test
    public void testTextRankWithArticle() throws Exception {
        clearDb();
        createGraph("textrank-article.cypher");

        // Run TextRank
        try (Transaction tx = getDatabase().beginTx()) {
            Result result = getDatabase().execute("match (a:AnnotatedText) return a");
            assertTrue("TextRank: didn't find AnnotatedText (error in graph initialization).", result.hasNext());
            if (!result.hasNext()) {
                return;
            }
            Node annText = (Node) result.next().get("a");
            TextRank textrank = new TextRank.Builder(getDatabase(), getNLPManager().getConfiguration())
                    .setTopXTags(1.0f / 3)
                    .build();
            assertNotNull("AnnotatedText not found.", annText);
            assertNotNull("TextRank.Builder failed: textrank is null", textrank);
            TextRankResult res = textrank.evaluate(Arrays.asList(new Node[] {annText}), 30, 0.85, 0.0001);
            assertTrue("TextRank failed, returned false.", res.getStatus().equals(TextRankResult.TextRankStatus.SUCCESS));
            tx.success();
        }

        // evaluate results
        Set<String> keywords = new HashSet<>();
        try (Transaction tx = getDatabase().beginTx()) {
            Result result = getDatabase().execute(
                    "MATCH (k:Keyword)-[:DESCRIBES]->(a:AnnotatedText)\n"
                            + "RETURN k.id AS id, k.value AS value\n");
            while (result != null && result.hasNext()) {
                Map<String, Object> next = result.next();
                String tag = next.get("value").toString();
                System.out.println(tag);
                keywords.add(tag);
            }
            tx.success();
        }

        assertFalse(keywords.contains("say"));
        assertFalse(keywords.contains("of"));
        assertFalse(keywords.contains("after"));
        assertFalse(keywords.contains("who"));
    }

    @Test
    public void testTextRankWithPreviousAnnotationDefaulted() {
        createStubPipelineAndSetDefault("default");
        executeInTransaction("CREATE (n:Document) SET n.text = 'John and I went to IBM today, it was great fun and we learned a lot about computers' WITH n " +
                "CALL ga.nlp.annotate({text: n.text, id: id(n), checkLanguage: false}) YIELD result MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result)", emptyConsumer());
        executeInTransaction("MATCH (n:AnnotatedText) CALL ga.nlp.ml.textRank({annotatedText:n}) YIELD result RETURN count(*)", emptyConsumer());
    }

    @Test
    public void testTextRankWithPreviousAnnotationNonDefault() {
        createPipeline(StubTextProcessor.class.getName(), "default");
        executeInTransaction("CREATE (n:Document) SET n.text = {p0} WITH n " +
                "CALL ga.nlp.annotate({text: n.text, id: id(n), checkLanguage: false, pipeline:'default'}) YIELD result MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result)", buildSeqParameters(TEXT1), emptyConsumer());
        executeInTransaction("MATCH (n:AnnotatedText) CALL ga.nlp.ml.textRank({annotatedText:n}) YIELD result RETURN count(*)", emptyConsumer());
    }

    @Test
    public void testCreate() throws Exception {
        createGraph("exported.cypher");
    }

    private void createGraph(String filename) throws Exception {
        // clean database before creating our own graph
        getDatabase().execute("MATCH (n) DETACH DELETE n");

        String content = new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource(filename).toURI())));
        List<String> queries = ImportUtils.getImportQueriesFromApocExport(content);
        queries.forEach(q -> {
            executeInTransaction(q, (result -> {
            }));
        });
    }

}
