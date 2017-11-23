package com.graphaware.nlp.enrich;

import com.graphaware.nlp.NLPIntegrationTest;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

abstract public class EnricherAbstractTest extends NLPIntegrationTest {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        createTagConstraint();
    }

    protected void createTagConstraint() {
        executeInTransaction("CREATE CONSTRAINT ON (t:Tag) ASSERT t.id IS UNIQUE;", (result -> {
        }));
    }

    protected void debugTagsRelations() {
        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().findNodes(Label.label("Tag")).forEachRemaining(node -> {
                node.getRelationships(RelationshipType.withName("IS_RELATED_TO")).forEach(relationship -> {
                    System.out.println(node.getAllProperties());
                    System.out.println(relationship.getOtherNode(node).getAllProperties());
                    System.out.println(relationship.getProperty("type"));
                });

            });

            tx.success();
        }
    }
}
