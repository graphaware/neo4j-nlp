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
//import com.graphaware.nlp.extension.AbstractExtension;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.graphaware.nlp.util.ImportUtils;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

public class TextRankTest extends NLPIntegrationTest {

    private static final List<String> expectedKeywords = Arrays.asList("flight failure", "speed brake", "space shuttle", "ground operation", "actuator", "installation", "flight", "gear", "shuttle", "brake", "speed", "failure", "unusual", "design");

    /**
     * Test of TextRank procedure, class TextRank.
     */
    @Test
    public void testTextRank() throws Exception {
        createGraph();

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
            boolean res = textrank.evaluate(annText, 30, 0.85, 0.0001);
            assertTrue("TextRank failed, returned false.", res);
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

        // clean after ourselves
        getDatabase().execute("MATCH (n) DETACH DELETE n");

    }

    // this test here is a duplicity of `TextRankProcedureTest.testTextRankWithDefaults()`
    /*@Test
    public void testTextRankViaProcedure() throws Exception {
        createGraph();
        executeInTransaction("MATCH (a:AnnotatedText)\n" +
                "call ga.nlp.ml.textRank({annotatedText: a}) YIELD result RETURN result", (result -> {
                    assertTrue(result.hasNext());
        }));

    }*/

    @Test
    public void testCreate() throws Exception {
        createGraph();
    }

    private void createGraph() throws Exception {
        // clean database before creating our own graph
        getDatabase().execute("MATCH (n) DETACH DELETE n");

        String content = new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource("exported.cypher").toURI())));
        List<String> queries = ImportUtils.getImportQueriesFromApocExport(content);
        queries.forEach(q -> {
            //System.out.println(q);
            executeInTransaction(q, (result -> {

            }));
        });
    }

}
