package com.graphaware.nlp;

import com.graphaware.common.kv.GraphKeyValueStore;
import com.graphaware.test.integration.EmbeddedDatabaseIntegrationTest;
import org.junit.Before;
import org.neo4j.graphdb.Transaction;

public class AbstractEmbeddedTest extends EmbeddedDatabaseIntegrationTest {

    protected GraphKeyValueStore keyValueStore;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        clearDb();
        this.keyValueStore = new GraphKeyValueStore(getDatabase());
    }

    protected void clearDb() {
        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().execute("MATCH (n) DETACH DELETE n");
            tx.success();
        }
    }
}
