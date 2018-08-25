package com.graphaware.nlp.dsl;

import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.stub.StubTextProcessor;
import com.graphaware.nlp.util.ImportUtils;
import com.graphaware.nlp.util.TestNLPGraph;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

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
    public void testTextRankComputeOnly() throws Exception {
        clearDb();
        createGraph();
        executeInTransaction("MATCH (n:AnnotatedText) CALL ga.nlp.ml.textRank.compute({annotatedText: n}) YIELD value, relevance RETURN value, relevance", (result -> {
            assertTrue("ga.nlp.ml.textRank() procedure failed.", result.hasNext());
            while (result.hasNext()) {
                Map<String, Object> record = result.next();
                assertNotNull(record.get("value"));
                assertNotNull(record.get("relevance"));
                assertTrue(record.get("relevance") instanceof Double);
            }
        }));
    }

    @Test
    public void testTextRankWithCustomSettings() throws Exception {
        clearDb();
        createGraph();
        executeInTransaction("MATCH (n:AnnotatedText) CALL ga.nlp.ml.textRank({annotatedText: n, iterations: 30, damp: 0.85, threshold: 0.0001}) YIELD result RETURN result", (result -> {
            assertTrue("ga.nlp.ml.textRank() procedure failed.", result.hasNext());
        }));

        executeInTransaction("MATCH (n:Keyword)-[:DESCRIBES]->(at) RETURN n, at", (result -> {
            assertTrue("No Keyword nodes found.", result.hasNext());
        }));
    }

    @Test
    public void testTextRankPostProcess() throws Exception {
        clearDb();
        createGraph();
        executeInTransaction("MATCH (n:AnnotatedText) CALL ga.nlp.ml.textRank({annotatedText: n, iterations: 30, damp: 0.85, threshold: 0.0001}) YIELD result RETURN result", (result -> {
            assertTrue("ga.nlp.ml.textRank() procedure failed.", result.hasNext());
        }));

        executeInTransaction("CALL ga.nlp.ml.textRank.postprocess({method: 'subgroups'})", emptyConsumer());
        executeInTransaction("MATCH (n:Keyword)-[:HAS_SUBGROUP]->(x) RETURN n.value AS v, x.value AS child", (result -> {
            assertTrue(result.hasNext());
            while (result.hasNext()) {
                Map<String, Object> record = result.next();
                System.out.println(record.get("v"));
                System.out.println(record.get("child"));
            }
        }));
    }

    @Test
    public void testTextRankPostProcessWithCustomInput() throws Exception {
        clearDb();
        createGraph();
        executeInTransaction("MATCH (n:AnnotatedText) CALL ga.nlp.ml.textRank({annotatedText: n, iterations: 30, damp: 0.85, threshold: 0.0001}) YIELD result RETURN result", (result -> {
            assertTrue("ga.nlp.ml.textRank() procedure failed.", result.hasNext());
        }));

        executeInTransaction("MATCH (a:AnnotatedText)<-[:DESCRIBES]-(k:Keyword) WHERE k.value CONTAINS 'space shuttle' CALL ga.nlp.ml.textRank.postprocess({method:'subgroups', annotatedText: a}) YIELD result RETURN count(*)", emptyConsumer());
        TestNLPGraph testNLPGraph = new TestNLPGraph(getDatabase());
        testNLPGraph.debugKeywords();
        executeInTransaction("MATCH (n:Keyword)-[:HAS_SUBGROUP]->(x) RETURN n.value AS v", (result -> {
            assertTrue(result.hasNext());
            while (result.hasNext()) {
                System.out.println(result.next().get("v"));
            }
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
