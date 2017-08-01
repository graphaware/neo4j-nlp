/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.ml.word2vec;

import java.io.File;
import java.net.URL;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ale
 */
public class Word2VecIndexTest {
    
    public Word2VecIndexTest() {
    }

    @Test
    public void textIndexing() throws Exception {
        URL resource = this.getClass().getResource("test_numberbatch-en-17.02.txt");
        System.out.println("Loading file: " + resource.getFile());
        String sourceFile = resource.getFile();
        String indexPath = System.getProperty("java.io.tmpdir") 
                + "word2VecIndex_" 
                + System.currentTimeMillis();
        System.out.println("Storing in file: " + indexPath);
        Word2VecIndexCreator.loadFromFile(sourceFile, indexPath, true);
        
        Word2VecIndexLookup lookup = new Word2VecIndexLookup(indexPath);
        double[] searchIndex = lookup.searchIndex("agriculturist");
        assertTrue(-0.0129d == searchIndex[2]);
    }
    
}
