package com.graphaware.nlp.ml.word2vec;

import com.graphaware.common.util.Pair;
import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.ml.similarity.CosineSimilarity;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class Word2VecIntegrationTest extends NLPIntegrationTest {

    @Test
    public void testModelsCanBeAddedFromCustomPath() {
        String w2vSourcePath = getClass().getClassLoader().getResource("").getPath() + "import/word2vecSource";
        String w2vDestinPath = System.getProperty("java.io.tmpdir") + File.separator + "word2VecIndex_" + System.currentTimeMillis();
        getWord2VecProcessor().getWord2VecModel().createModelFromPaths(w2vSourcePath, w2vDestinPath, "numberbatch", "en");
        assertTrue(getWord2VecProcessor().getWord2VecModel().getModels().containsKey("numberbatch"));
        assertEquals(-0.0129d, getWord2VecProcessor().getWord2VecModel().getModels().get("numberbatch").searchIndex("agriculturist")[2], 1.0d);
    }

    @Test
    public void testModelsWithNewMultilingualVersionCanBeAdded() {
        String w2vSourcePath = getClass().getClassLoader().getResource("").getPath() + "import/word2vecSource1706";
        String w2vDestinPath = System.getProperty("java.io.tmpdir") + File.separator + "word2VecIndex_" + System.currentTimeMillis();
        getWord2VecProcessor().getWord2VecModel().createModelFromPaths(w2vSourcePath, w2vDestinPath, "numberbatch1706", "en");
        assertTrue(getWord2VecProcessor().getWord2VecModel().getModels().containsKey("numberbatch1706"));
        assertNotNull(getWord2VecProcessor().getWord2Vec("astronaut", "numberbatch1706"));
    }

    @Test
    public void testModelsWithNonDefaultLanguageCanBeAdded() {
        String w2vSourcePath = getClass().getClassLoader().getResource("").getPath() + "import/swedishSource";
        String w2vDestinPath = System.getProperty("java.io.tmpdir") + File.separator + "swedishIndex_" + System.currentTimeMillis();
        getWord2VecProcessor().getWord2VecModel().createModelFromPaths(w2vSourcePath, w2vDestinPath, "swedish-model", "sv");
        assertTrue(getWord2VecProcessor().getWord2VecModel().getModels().containsKey("swedish-model"));
        assertNotNull(getWord2VecProcessor().getWord2Vec("öring", "swedish-model"));
    }

    @Test
    public void testFastTextModelsCanBaAdded() {
        String w2vSourcePath = getClass().getClassLoader().getResource("").getPath() + "import/fasttextSource";
        String w2vDestinPath = System.getProperty("java.io.tmpdir") + File.separator + "fasttextIndex_" + System.currentTimeMillis();
        getWord2VecProcessor().getWord2VecModel().createModelFromPaths(w2vSourcePath, w2vDestinPath, "fasttext", "en");
        assertTrue(getWord2VecProcessor().getWord2VecModel().getModels().containsKey("fasttext"));
        assertNotNull(getWord2VecProcessor().getWord2Vec("highest", "fasttext"));
    }

    @Test
    public void testCanComputeNearestNeighbors() {
        String w2vSourcePath = getClass().getClassLoader().getResource("").getPath() + "import/fasttextSource";
        String w2vDestinPath = System.getProperty("java.io.tmpdir") + File.separator + "fasttextIndex_" + System.currentTimeMillis();
        getWord2VecProcessor().getWord2VecModel().createModelFromPaths(w2vSourcePath, w2vDestinPath, "fasttext", "en");
        assertTrue(getWord2VecProcessor().getWord2VecModel().getModels().containsKey("fasttext"));
        assertNotNull(getWord2VecProcessor().getWord2Vec("highest", "fasttext"));
//        getWord2VecProcessor().getWord2VecModel().getModels().get("fasttext").loadNN(30);
        long now = System.currentTimeMillis();
        List<Pair> nn = getWord2VecProcessor().getWord2VecModel().getModels().get("fasttext").getNearestNeighbors("asparagus", 10);
        System.out.println(System.currentTimeMillis() - now);
        assertEquals(10, nn.size());
        nn.forEach(pair -> {
            System.out.println(pair.first().toString() + " : " + pair.second().toString());
        });
    }
}
