/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.conceptnet5;

import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.processor.TextProcessor;
import java.util.List;
import org.junit.Test;

public class ConceptNet5ImporterTest {
    
    public ConceptNet5ImporterTest() {
    }

    /**
     * Test of importHierarchy method, of class ConceptNet5Importer.
     */
    @Test
    public void testImportHierarchy() {
        TextProcessor textProcessor = new TextProcessor();
        ConceptNet5Importer instance = new ConceptNet5Importer.Builder("http://conceptnet5.media.mit.edu/data/5.4", textProcessor).build();
        Tag source = textProcessor.annotateTag("bad");
        List<Tag> result = instance.importHierarchy(source, "en");
        List<Tag> expResult = null;
        //assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }
    
}
