/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.dsl;

import com.graphaware.nlp.NLPIntegrationTest;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

/**
 *
 * @author ale
 */
public class EnrichTest extends NLPIntegrationTest {

    private static final String TEXT = "On 8 May 2013, "
            + "one week before the Pakistani election, the third author, "
            + "in his keynote address at the Sentiment Analysis Symposium, "
            + "forecast the winner of the Pakistani election. The chart "
            + "in Figure 1 shows varying sentiment on the candidates for "
            + "prime minister of Pakistan in that election.";

    private static final String TEXT_1 = "circuit";

    @Test
    public void testConceptText() {
        try (Transaction tx = getDatabase().beginTx()) {
            String id = "id1";
            Map<String, Object> params = new HashMap<>();
            params.put("value", TEXT_1);
            params.put("id", id);
            Result news = getDatabase().execute("MERGE (n:News {text: {value}}) WITH n\n"
                    + "CALL ga.nlp.annotate({text:n.text, id: {id}, checkLanguage:false}) YIELD result\n"
                    + "MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result)\n"
                    + "return result", params);
            ResourceIterator<Object> rowIterator = news.columnAs("result");
            assertTrue(rowIterator.hasNext());
            Node resultNode = (Node) rowIterator.next();
            assertEquals(resultNode.getProperty("id"), id);
            Result tags = getDatabase().execute(
                    "MATCH (a:AnnotatedText) with a "
                    + "CALL ga.nlp.enrich.concept({node:a, depth: 2, admittedRelationships:['IsA']}) YIELD result\n"
                    + "return result;", params);
            assertTrue(tags.hasNext());

            rowIterator = tags.columnAs("result");
            resultNode = (Node) rowIterator.next();
            assertEquals(resultNode.getProperty("id"), id);
            params.clear();
            Result parentTags = getDatabase().execute(
                    "MATCH (a:Tag {value: 'circuit'})-[:IS_RELATED_TO*..2]->(p)\n"
                    + "return distinct p as result;", params);
            assertTrue(parentTags.hasNext());
            rowIterator = parentTags.columnAs("result");
            int count = 0;
            while (rowIterator.hasNext()) {
                rowIterator.next();
                count++;
            }
            assertEquals(22, count);
            tx.success();
        }
    }
}
