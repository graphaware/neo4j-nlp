/*
 * Copyright (c) 2013-2016 GraphAware
 *
 * This file is part of the GraphAware Framework.
 *
 * GraphAware Framework is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package com.graphaware.nlp.conceptnet5;

import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.util.ServiceLoader;
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
        TextProcessor textProcessor = ServiceLoader.loadTextProcessor("com.graphaware.nlp.processor.stanford.StanfordTextProcessor");
        ConceptNet5Importer instance = new ConceptNet5Importer.Builder("http://conceptnet5.media.mit.edu/data/5.4", textProcessor).build();
        Tag source = textProcessor.annotateTag("bad");
        List<Tag> result = instance.importHierarchy(source, "en");
        List<Tag> expResult = null;
        //assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }
    
}
