package com.graphaware.nlp.ml.word2vec;

import java.io.File;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class Word2VecIndexTest {

    private String indexPath;
    
    @Before
    public void setUp() {
        URL resource = this.getClass().getResource("test_numberbatch-en-17.02.txt");
        System.out.println("Loading file: " + resource.getFile());
        String sourceFile = resource.getFile();
        indexPath = System.getProperty("java.io.tmpdir") + File.separator
                + "word2VecIndex_"
                + System.currentTimeMillis();
        System.out.println("Storing in file: " + indexPath);
        Word2VecIndexCreator.loadFromFile(sourceFile, indexPath, true);
    }

    @Test
    public void textIndexing() throws Exception {
        Word2VecIndexLookup lookup = new Word2VecIndexLookup(indexPath);
        double[] searchIndex = lookup.searchIndex("agriculturist");
        assertTrue(-0.0129d == searchIndex[2]);
    }

    @Test
    public void testLookUp() {
        Word2VecIndexLookup lookup = new Word2VecIndexLookup(indexPath);
        double[] results = lookup.searchIndex("agriculturist");
        for(double r : results) {
            System.out.println(r);
        }
    }

    @Test
    public void testLookupWithProcessor() {
        Word2VecProcessor processor = new Word2VecProcessor();
        System.out.println(processor.getWord2Vec("agriculturist", null));
    }
    
}
