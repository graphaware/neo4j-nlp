package com.graphaware.nlp.dsl;

import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.ml.word2vec.Word2VecProcessor;
import com.graphaware.nlp.util.TestNLPGraph;
import org.junit.Test;
import org.neo4j.graphdb.Node;

import java.io.File;
import java.util.Map;

import static org.junit.Assert.*;

public class Word2VecProcedureTest extends NLPIntegrationTest {

    @Test
    public void testAttachVectorsToTagNodes() {
        clearDb();
        String w2vSourcePath = getClass().getClassLoader().getResource("").getPath() + "import/word2vecSource";
        String w2vDestinPath = System.getProperty("java.io.tmpdir") + File.separator + "word2VecIndex_" + System.currentTimeMillis();
        getWord2VecProcessor().getWord2VecModel().createModelFromPaths(w2vSourcePath, w2vDestinPath, "numberbatch");
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

}
