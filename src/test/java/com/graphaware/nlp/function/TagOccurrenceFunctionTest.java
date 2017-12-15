package com.graphaware.nlp.function;

import com.graphaware.nlp.dsl.function.TagOccurrenceFunctions;
import com.graphaware.test.integration.EmbeddedDatabaseIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.impl.proc.Procedures;
import org.neo4j.kernel.internal.GraphDatabaseAPI;

import static org.junit.Assert.*;

public class TagOccurrenceFunctionTest extends EmbeddedDatabaseIntegrationTest {

    @Before
    public void registerFunction() throws Exception {
        ((GraphDatabaseAPI) getDatabase()).getDependencyResolver().resolveDependency(Procedures.class).registerFunction(TagOccurrenceFunctions.class);
    }

    @Test
    public void testTagIsRetrieved() throws Exception {
        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().execute("MATCH (n) DETACH DELETE n");
            String query = "CREATE (n:TagOccurrence) SET n.startPosition = 1, n.endPosition = 5 " +
                    "CREATE (t:Tag) SET t.value = 'hello', t.ne = ['NN'] " +
                    "MERGE (n)-[:TAG_OCCURRENCE_TAG]->(t)";
            getDatabase().execute(query);
            tx.success();
        }

        try (Transaction tx = getDatabase().beginTx()) {
            Result result = getDatabase().execute("MATCH (n:TagOccurrence) RETURN ga.nlp.tagForOccurrence(n) AS tag");
            assertTrue(result.hasNext());
            Node node = (Node) result.next().get("tag");
            assertEquals("hello", node.getProperty("value"));
            tx.success();
        }
    }
}
