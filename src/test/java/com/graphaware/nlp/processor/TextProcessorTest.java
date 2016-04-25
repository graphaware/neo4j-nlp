/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.processor;

import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.persistence.GraphPersistence;
import com.graphaware.nlp.persistence.LocalGraphDatabase;
import com.graphaware.test.integration.DatabaseIntegrationTest;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

/**
 *
 * @author ale
 */
public class TextProcessorTest extends DatabaseIntegrationTest {

    @Test
    public void testAnnotatedText() {
        TextProcessor textProcessor = new TextProcessor();
        AnnotatedText annotateText = textProcessor.annotateText("On 8 May 2013, "
                + "one week before the Pakistani election, the third author, "
                + "in his keynote address at the Sentiment Analysis Symposium, "
                + "forecast the winner of the Pakistani election. The chart "
                + "in Figure 1 shows varying sentiment on the candidates for "
                + "prime minister of Pakistan in that election. The next day, "
                + "the BBC’s Owen Bennett Jones, reporting from Islamabad, wrote "
                + "an article titled “Pakistan Elections: Five Reasons Why the "
                + "Vote is Unpredictable,”1 in which he claimed that the election "
                + "was too close to call. It was not, and despite his being in Pakistan, "
                + "the outcome of the election was exactly as we predicted.");

        assertEquals(4, annotateText.getSentences().size());
        assertEquals(15, annotateText.getSentences().get(0).getTags().size());
        assertEquals(11, annotateText.getSentences().get(1).getTags().size());
        assertEquals(24, annotateText.getSentences().get(2).getTags().size());
        assertEquals(9, annotateText.getSentences().get(3).getTags().size());

        GraphPersistence peristence = new LocalGraphDatabase(getDatabase());
        peristence.persistOnGraph(annotateText);
        checkLocation("Pakistan");
        checkVerb("show");

    }

    private void checkLocation(String location) throws QueryExecutionException {
        try (Transaction tx = getDatabase().beginTx()) {
            ResourceIterator<Object> rowIterator = getTagsIterator(location);
            Node pakistanNode = (Node) rowIterator.next();
            assertFalse(rowIterator.hasNext());
            assertEquals(pakistanNode.getProperty("ne"), "LOCATION");
            tx.success();
        }
    }

    private void checkVerb(String verb) throws QueryExecutionException {
        try (Transaction tx = getDatabase().beginTx()) {
            ResourceIterator<Object> rowIterator = getTagsIterator(verb);
            Node pakistanNode = (Node) rowIterator.next();
            assertFalse(rowIterator.hasNext());
            assertEquals(pakistanNode.getProperty("pos"), "VBZ");
            tx.success();
        }
    }

    private ResourceIterator<Object> getTagsIterator(String value) throws QueryExecutionException {
        Map<String, Object> params = new HashMap<>();
        params.put("value", value);
        Result pakistan = getDatabase().execute("MATCH (n:Tag {value: {value}}) return n", params);
        ResourceIterator<Object> rowIterator = pakistan.columnAs("n");
        return rowIterator;
    }

    @Test
    public void testAnnotatedTag() {
        TextProcessor textProcessor = new TextProcessor();
        Tag annotateTag = textProcessor.annotateTag("winners");
        assertEquals(annotateTag.getLemma(), "winner");
    }
}
