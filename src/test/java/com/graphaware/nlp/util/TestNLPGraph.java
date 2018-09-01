package com.graphaware.nlp.util;

import com.graphaware.nlp.NLPManager;
import org.neo4j.graphdb.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class TestNLPGraph {

    private final GraphDatabaseService database;

    public TestNLPGraph(GraphDatabaseService database) {
        this.database = database;
    }

    public void assertNodesCount(String label, long count) {
        executeInTransaction("MATCH (n:`"+label+"`) RETURN count(n) AS c", (result -> {
            assertEquals(count, result.next().get("c"));
        }));
    }

    public void assertSentenceWithIdHasPhraseWithText(String id, String text) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", id);
        parameters.put("text", text);
        executeInTransaction("MATCH (n:Sentence) WHERE n.id = {id} " +
                "MATCH (n)-[:HAS_PHRASE]->(p) WHERE p.value = {text} RETURN n, p", parameters, (result -> {
                    assertTrue(result.hasNext());
        }));
    }

    public void assertSentenceWithIdHasPhraseOccurrenceCount(String id, long count) {
        executeInTransaction("MATCH (n:Sentence) WHERE n.id = {id} " +
                "RETURN size((n)-[:SENTENCE_PHRASE_OCCURRENCE]->()) AS c", Collections.singletonMap("id", id), (result -> {
                    assertTrue(result.hasNext());
                    assertEquals(count, result.next().get("c"));
        }));
    }

    public void assertSentenceWithIdHasSentimentLabel(String id, String label) {
        executeInTransaction("MATCH (n:Sentence) WHERE n.id = {id} RETURN n", Collections.singletonMap("id", id), (result -> {
            assertTrue(result.hasNext());
            Map<String, Object> record = result.next();
            Node sentence = (Node) record.get("n");
            assertTrue(sentence.hasLabel(Label.label(label)));
        }));
    }

    public void assertPhraseOccurrenceForTextHasStartAndEndPosition(String text, int start, int end) {
        Map<String, Object> params = new HashMap<>();
        params.put("text", text);
        params.put("start", start);
        params.put("end", end);
        executeInTransaction("MATCH (n:Phrase) WHERE n.value = {text} " +
                "MATCH (n)<-[:PHRASE_OCCURRENCE_PHRASE]-(occ) " +
                "WHERE occ.startPosition = {start} AND occ.endPosition = {end} RETURN n, occ", params, (result -> {
            assertTrue(result.hasNext());
        }));
    }

    public void assertPhraseWithTextExist(String text) {
        executeInTransaction("MATCH (n:Phrase) WHERE n.value = {text} RETURN n", Collections.singletonMap ("text", text), (result -> {
            assertTrue(result.hasNext());
        }));
    }

    public void assertTagWithValueHasNERLabel(String value, String label) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("value", value);
        parameters.put("label", label);
        executeInTransaction("MATCH (n:Tag) WHERE n.value = {value} AND {label} IN labels(n) RETURN n", parameters, (result -> {
            assertTrue(result.hasNext());
        }));
    }

    public void assertTagWithValueHasNE(String value, String ne) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("value", value);
        parameters.put("ne", ne);
        executeInTransaction("MATCH (n:Tag) WHERE n.value = {value} AND {ne} IN n.ne RETURN n", parameters, (result -> {
            assertTrue(result.hasNext());
        }));
    }

    public void assertTagWithValueHasPos(String value, String pos) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("value", value);
        parameters.put("pos", pos);
        executeInTransaction("MATCH (n:Tag) WHERE n.value = {value} AND {pos} IN n.pos RETURN n", parameters, (result -> {
            assertTrue(result.hasNext());
        }));
    }

    public void assertTagHasRelatedTag(String original, String target) {
        Map<String, Object> params = new HashMap<>();
        params.put("original", original);
        params.put("target", target);
        executeInTransaction("MATCH (n:Tag {value: {original} })-[:IS_RELATED_TO]-(t:Tag {value: {target} }) RETURN n, t", params,
                (result -> {
                    assertTrue(result.hasNext());
                }));
    }

    public void assertTagWithValueExist(String value) {
        executeInTransaction("MATCH (n:Tag) WHERE n.value = {value} RETURN n",
                Collections.singletonMap("value", value),
                (result -> {
                    assertTrue(result.hasNext());

        }));
    }

    public void assertTagWithValueDoesNotExist(String value) {
        executeInTransaction("MATCH (n:Tag) WHERE n.value = {value} RETURN n",
                Collections.singletonMap("value", value),
                (result) -> {
            assertFalse(result.hasNext());
                });
    }

    public void assertTagWithIdExist(String id) {
        executeInTransaction("MATCH (n:Tag) WHERE n.id = {id} RETURN n", Collections.singletonMap("id", id), (result -> {
            assertTrue(result.hasNext());
        }));
    }

    public void assertTagOccurrenceWithValueExist(String value) {
        executeInTransaction("MATCH (n:TagOccurrence) WHERE n.value = {value} RETURN n", Collections.singletonMap("value", value), (result -> {
            assertTrue(result.hasNext());
        }));
    }

    public void assertTagOccurrenceWithValueDoesNotExist(String value) {
        executeInTransaction("MATCH (n:TagOccurrence) WHERE n.value = {value} RETURN n", Collections.singletonMap("value", value), (result -> {
            assertFalse(result.hasNext());
        }));
    }

    public void assertTagOccurrenceWithValueAndNeExist(String value, String ne) {
        Map<String, Object> map = new HashMap<>();
        map.put("value", value);
        map.put("ne", ne);
        executeInTransaction("MATCH (n:TagOccurrence) WHERE n.value = $value AND $ne IN labels(n) RETURN n", map, (result) -> {
            assertTrue(result.hasNext());
        });
    }

    public void assertPhraseOccurrenceNodesCount(long count) {
        assertNodesCount("PhraseOccurrence", count);
    }

    public void assertPhraseNodesCount(long count) {
        assertNodesCount("Phrase", count);
    }

    public void assertTagNodesCount(long count) {
        assertNodesCount("Tag", count);
    }

    public void assertTagOccurrenceNodesCount(long count) {
        assertNodesCount("TagOccurrence", count);
    }

    public void assertSentenceNodesCount(long count) {
        assertNodesCount("Sentence", count);
    }

    public void assertAnnotatedTextNodesCount(long count) {
        assertNodesCount("AnnotatedText", count);
    }

    public void debugAnnotatedTextsCount() {
        executeInTransaction("MATCH (n:AnnotatedText) RETURN count(n) AS c", (result -> {
            System.out.println(result.next().get("c").toString());
        }));
    }

    public void debugKeywords() {
        executeInTransaction("MATCH (n:Keyword)-[r:DESCRIBES]->() RETURN n.value AS v, r.relevance AS r", (result -> {
            while (result.hasNext()) {
                Map<String, Object> record = result.next();
                System.out.println(String.format("%s : %s", record.get("v"), record.get("r").toString()));
            }
        }));
    }

    public void executeInTransaction(String query, Consumer<Result> resultConsumer) {
        executeInTransaction(query, Collections.emptyMap(), resultConsumer);
    }

    public void executeInTransaction(String query, Map<String, Object> parameters, Consumer<Result> resultConsumer) {
        try (Transaction tx = database.beginTx()) {
            Map<String, Object> p = (parameters == null) ? Collections.emptyMap() : parameters;
            resultConsumer.accept(database.execute(query, p));
            tx.success();
        }
    }

}
