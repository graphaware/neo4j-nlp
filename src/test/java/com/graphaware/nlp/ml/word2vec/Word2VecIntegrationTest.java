package com.graphaware.nlp.ml.word2vec;

import com.graphaware.nlp.NLPIntegrationTest;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class Word2VecIntegrationTest extends NLPIntegrationTest {

    @Test
    public void testModelsCanBeAddedFromCustomPath() {
        String w2vSourcePath = getClass().getClassLoader().getResource("").getPath() + "import/word2vecSource";
        String w2vDestinPath = System.getProperty("java.io.tmpdir") + File.separator + "word2VecIndex_" + System.currentTimeMillis();
        getWord2VecProcessor().getWord2VecModel().createModelFromPaths(w2vSourcePath, w2vDestinPath, "numberbatch");
        assertTrue(getWord2VecProcessor().getWord2VecModel().getModels().containsKey("numberbatch"));
        assertEquals(-0.0129d, getWord2VecProcessor().getWord2VecModel().getModels().get("numberbatch").searchIndex("agriculturist")[2], 1.0d);
    }

    @Test
    public void testModelsWithNewMultilingualVersionCanBeAdded() {
        String w2vSourcePath = getClass().getClassLoader().getResource("").getPath() + "import/word2vecSource1706";
        String w2vDestinPath = System.getProperty("java.io.tmpdir") + File.separator + "word2VecIndex_" + System.currentTimeMillis();
        getWord2VecProcessor().getWord2VecModel().createModelFromPaths(w2vSourcePath, w2vDestinPath, "numberbatch1706");
        assertTrue(getWord2VecProcessor().getWord2VecModel().getModels().containsKey("numberbatch1706"));
        assertNotNull(getWord2VecProcessor().getWord2Vec("astronaut", "numberbatch1706"));
    }
}
