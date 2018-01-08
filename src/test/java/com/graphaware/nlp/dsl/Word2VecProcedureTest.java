package com.graphaware.nlp.dsl;

import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.ml.word2vec.Word2VecProcessor;
import com.graphaware.nlp.util.TestNLPGraph;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class Word2VecProcedureTest extends NLPIntegrationTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        String w2vSourcePath = getClass().getClassLoader().getResource("").getPath() + "import/word2vecSource";
        String w2vDestinPath = System.getProperty("java.io.tmpdir") + File.separator + "word2VecIndex_" + System.currentTimeMillis();
        getWord2VecProcessor().getWord2VecModel().createModelFromPaths(w2vSourcePath, w2vDestinPath, "numberbatch");
        clearDb();
    }

    @Test
    public void testAttachVectorsToTagNodes() {
        executeInTransaction("CALL ga.nlp.annotate({text: 'I met one agriculturist.', id: '123-fff', checkLanguage: false})", emptyConsumer());
        TestNLPGraph tester = new TestNLPGraph(getDatabase());
        tester.assertTagWithValueExist("agriculturist");
        executeInTransaction("CALL ga.nlp.ml.word2vec.attach({query:\"MATCH (t:Tag) return t\", modelName:'numberbatch'}) YIELD result \n" +
                "return result;", (result -> {
                    assertTrue(result.hasNext());
        }));
        executeInTransaction("MATCH (n:Tag {value:'agriculturist'}) RETURN n", (result -> {
            assertTrue(result.hasNext());
            Map<String, Object> record = result.next();
            assertTrue(((Node) record.get("n")).hasProperty("word2vec"));
        }));
    }

    @Test
    public void testListModels() {
        executeInTransaction("CALL ga.nlp.ml.word2vec.listModels()", (result -> {
            assertTrue(result.hasNext());
            while (result.hasNext()) {
                Map<String, Object> record = result.next();
                assertEquals("numberbatch", record.get("name"));
                assertEquals(100, (long) record.get("indexCount"));
            }
        }));
    }

    @Test
    public void testAddingModelWithProcedure() {
        String w2vSourcePath = getClass().getClassLoader().getResource("").getPath() + "import/word2vecSource1706";
        String w2vDestinPath = System.getProperty("java.io.tmpdir") + File.separator + "word2VecIndex_" + System.currentTimeMillis();
        Map<String, Object> params = new HashMap<>();
        params.put("source", w2vSourcePath);
        params.put("dest", w2vDestinPath);
        params.put("name", "numberbatch1706");
        executeInTransaction("CALL ga.nlp.ml.word2vec.addModel({source},{dest},{name})", params, (result -> {
            assertTrue(result.hasNext());
        }));
        assertTrue(getWord2VecProcessor().getWord2VecModel().getModels().containsKey("numberbatch1706"));
        executeInTransaction("CALL ga.nlp.annotate({text: 'I met one astronaut.', id: '123-fff', checkLanguage: false})", emptyConsumer());
        executeInTransaction("CALL ga.nlp.ml.word2vec.attach({query:\"MATCH (t:Tag) return t\", modelName:'numberbatch1706'}) YIELD result \n" +
                "return result;", (result -> {
            assertTrue(result.hasNext());
        }));
        executeInTransaction("MATCH (n:Tag {value:'astronaut'}) RETURN n", (result -> {
            assertTrue(result.hasNext());
            Map<String, Object> record = result.next();
            assertTrue(((Node) record.get("n")).hasProperty("word2vec"));
        }));
    }

    @Test
    public void testGettingVectorForTagWithUserFunction() {
        executeInTransaction("CALL ga.nlp.annotate({text: 'I met one agriculturist.', id: '123-fff', checkLanguage: false})", emptyConsumer());
        executeInTransaction("MATCH (n:Tag) WHERE n.value = 'agriculturist' RETURN ga.nlp.ml.word2vec.vector(n, 'numberbatch') AS vector", (result -> {
            assertTrue(result.hasNext());
            List<Double> vector = (List<Double>) ((Map<String, Object>) result.next()).get("vector");
            assertEquals(-0.0129, vector.get(2), 1.0d);
        }));
    }

    @Test
    public void testGettingWordVectorForNonExistingWordReturnsNull() {
        executeInTransaction("RETURN ga.nlp.ml.word2vec.wordVector('non-value') AS vector", (result -> {
            assertTrue(result.hasNext());
            Map<String, Object> record = (Map<String, Object>) result.next();
            assertNull(record.get("vector"));
        }));
    }

    @Test
    public void testGettingVectorForWordWithUserFunction() {
        executeInTransaction("RETURN ga.nlp.ml.word2vec.wordVector('agriculturist') AS vector", (result -> {
            assertTrue(result.hasNext());
        }));
    }
}
