/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.enrich.conceptnet5;

import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

/**
 *
 * @author ale
 */
public class ConceptNet5ImporterTest {

    public ConceptNet5ImporterTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSomeMethod() {
        String test = "this_is_a_test_en";
        ConceptNet5Importer importer = new ConceptNet5Importer("http://api.conceptnet.io", 0, ConceptNet5Importer.DEFAULT_ADMITTED_RELATIONSHIP);
        String removedParenthesis = importer.removeParenthesis(test + "(en)");
        assertEquals(test, removedParenthesis);

        removedParenthesis = importer.removeParenthesis("(en)" + test);
        assertEquals("(en)" + test, removedParenthesis);

        removedParenthesis = importer.removeParenthesis(test);
        assertEquals(test, removedParenthesis);
    }

}
