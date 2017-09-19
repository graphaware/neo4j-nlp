package com.graphaware.nlp.ml.textrank;

import com.graphaware.nlp.NLPIntegrationTest;
import java.util.Map;
import java.util.HashMap;

import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import org.junit.Test;
import static org.junit.Assert.*;



public class PageRankTest extends NLPIntegrationTest {

    private final static double damp = 0.85;
    private Map<String, Double> expectedPRs;

    public PageRankTest() {
        this.expectedPRs = new HashMap<>();
        this.expectedPRs.put("A", 3.3);
        this.expectedPRs.put("B", 38.4);
        this.expectedPRs.put("C", 34.3);
        this.expectedPRs.put("D", 3.9);
        this.expectedPRs.put("E", 8.1);
        this.expectedPRs.put("F", 3.9);
        this.expectedPRs.put("G", 1.6);
        this.expectedPRs.put("H", 1.6);
        this.expectedPRs.put("I", 1.6);
        this.expectedPRs.put("J", 1.6);
        this.expectedPRs.put("K", 1.6);
    }

    @Test
    public void testPageRank() {
        createGraph();

        // run PageRank
        try (Transaction tx = getDatabase().beginTx()) {
            Result result = getDatabase().execute(
                    "match (a:AnnotatedText) where a.id=\"test118\"\n"
                            + "call ga.nlp.ml.pageRank({relationshipType: \"Related_to\", "
                            + "nodeType: \"Test\", damp: " + damp + "}) yield result\n"
                                    + "return result\n"
            );
            assertTrue(result.hasNext());
        } catch (Exception e) {
            assertTrue("PageRank failed: " + e.getMessage(), false);
            return;
        }

        // evaluate results
        //double minPR = (1 - damp) / expectedPRs.size();
        try (Transaction tx = getDatabase().beginTx()) {
            Result result = getDatabase().execute("match (t:Test) return t.value as tag, t.pagerank as pr");
            while (result!=null && result.hasNext()) {
                Map<String, Object> next = result.next();
                String tag = (String) next.get("tag");
                Double pr  = (Double) next.get("pr");
                assertTrue("PageRank " + pr + "% of node " + tag + " doesn't match expected value " + expectedPRs.get(tag) + "%",
                    Math.abs(expectedPRs.get(tag) - pr) < 0.1 * expectedPRs.get(tag));
            }
        } catch (Exception e) {
            assertTrue("PageRank evaluation failed: " + e.getMessage(), true);
        }
    }

    private void createGraph() {
        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().execute(
                "create (at:AnnotatedText {id: \"test118\"})-[:TestRel]->(d:Test {value: \"D\"})\n"
                + "merge (d)-[:Related_to]->(a:Test {value: \"A\"})<-[:TestRel]-(at)\n"
                + "merge (d)-[:Related_to]->(b:Test {value: \"B\"})<-[:TestRel]-(at)\n"
                + "merge (at)-[:TestRel]->(e:Test {value: \"E\"})-[:Related_to]->(b)\n"
                + "merge (e)-[:Related_to]->(d)\n"
                + "merge (e)-[:Related_to]->(f:Test {value: \"F\"})<-[:TestRel]-(at)\n"
                + "merge (f)-[:Related_to]->(e)\n"
                + "merge (f)-[:Related_to]->(b)\n"
                + "merge (b)-[:Related_to]->(c:Test {value: \"C\"})<-[:TestRel]-(at)\n"
                + "merge (c)-[:Related_to]->(b)\n"
                + "merge (at)-[:TestRel]->(g:Test {value: \"G\"})-[:Related_to]->(b)\n"
                + "merge (at)-[:TestRel]->(h:Test {value: \"H\"})-[:Related_to]->(b)\n"
                + "merge (at)-[:TestRel]->(i:Test {value: \"I\"})-[:Related_to]->(b)\n"
                + "merge (g)-[:Related_to]->(e)\n"
                + "merge (h)-[:Related_to]->(e)\n"
                + "merge (i)-[:Related_to]->(e)\n"
                + "merge (at)-[:TestRel]->(j:Test {value: \"J\"})-[:Related_to]->(e)\n"
                + "merge (at)-[:TestRel]->(k:Test {value: \"K\"})-[:Related_to]->(e)\n"
            );
            tx.success();
        } catch (Exception e) {
            assertTrue("PageRankTest: error while initialising graph", true);
        }
    }

}
