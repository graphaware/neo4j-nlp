package com.graphaware.nlp.dsl;

import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.stub.StubTextProcessor;
import com.graphaware.nlp.util.ImportUtils;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.*;

public class TextRankProcedureTest extends NLPIntegrationTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        createPipeline(StubTextProcessor.class.getName(), TextProcessor.DEFAULT_PIPELINE);
        executeInTransaction("CALL ga.nlp.processor.pipeline.default({p0})", buildSeqParameters("tokenizer"), emptyConsumer());
    }

    @Test
    public void testTextRankWithDefaults() throws Exception {
        clearDb();
        createGraph();
        executeInTransaction("MATCH (n:AnnotatedText) CALL ga.nlp.ml.textRank({annotatedText: n}) YIELD result RETURN result", (result -> {
            assertTrue("ga.nlp.ml.textRank() procedure failed.", result.hasNext());
        }));

        executeInTransaction("MATCH (n:Keyword)-[:DESCRIBES]->(at) RETURN n, at", (result -> {
            assertTrue("No Keyword nodes found.", result.hasNext());
        }));

    }

    @Test
    public void testTextRankWithCustomSettings() throws Exception {
        clearDb();
        createGraph();
        executeInTransaction("MATCH (n:AnnotatedText) CALL ga.nlp.ml.textRank({annotatedText: n, iterations: 30, damp: 0.85, treshold: 0.0001}) YIELD result RETURN result", (result -> {
            assertTrue("ga.nlp.ml.textRank() procedure failed.", result.hasNext());
        }));

        executeInTransaction("MATCH (n:Keyword)-[:DESCRIBES]->(at) RETURN n, at", (result -> {
            assertTrue("No Keyword nodes found.", result.hasNext());
        }));
    }

    private void createGraph() throws Exception {
        String content = new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource("exported.cypher").toURI())));
        List<String> queries = ImportUtils.getImportQueriesFromApocExport(content);
        queries.forEach(q -> {
            executeInTransaction(q, (result -> {
                //
            }));
        });
    }
}
