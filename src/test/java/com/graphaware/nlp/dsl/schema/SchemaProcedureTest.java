package com.graphaware.nlp.dsl.schema;

import com.graphaware.nlp.NLPIntegrationTest;
import org.junit.Test;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.IndexDefinition;

import static org.junit.Assert.*;
import static com.graphaware.nlp.persistence.constants.Labels.*;

public class SchemaProcedureTest extends NLPIntegrationTest {

    private static final String UNIQUE = "UNIQUE";
    private static final String ID = "id";
    private static final String VALUE = "value";

    @Test
    public void testSchemaIsCreated() {
        executeInTransaction("CALL ga.nlp.createSchema", emptyConsumer());
        schemaAssert(AnnotatedText, ID, UNIQUE);
        schemaAssert(Sentence, ID, UNIQUE);
        schemaAssert(Tag, ID, UNIQUE);
        schemaAssert(Keyword, ID, UNIQUE);
        schemaAssert(Tag, VALUE, "");

    }

    private void schemaAssert(Label label, String s, String type) {
        boolean exist = false;
        try (Transaction tx = getDatabase().beginTx()) {
            if (type.equals(UNIQUE)) {
                for (ConstraintDefinition constraintDefinition : getDatabase().schema().getConstraints(label)) {
                    for (String p : constraintDefinition.getPropertyKeys()) {
                        if (p.equals(s)) {
                            exist = true;
                        }
                    }
                }
            } else {
                for (IndexDefinition indexDefinition : getDatabase().schema().getIndexes()) {
                    for (String p : indexDefinition.getPropertyKeys()) {
                        if (p.equals(s)) {
                            exist = true;
                        }
                    }
                }
            }
            tx.success();
        }

        assertTrue(exist);
    }
}
