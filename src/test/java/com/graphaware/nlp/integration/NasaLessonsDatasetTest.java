package com.graphaware.nlp.integration;

import com.graphaware.nlp.NLPIntegrationTest;
import com.graphaware.nlp.stub.StubTextProcessor;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class NasaLessonsDatasetTest extends NLPIntegrationTest {

    @Test
    public void testAnnotation() {
        clearDb();
        importDataset();
        String query = "MATCH (n:Lesson) CALL ga.nlp.annotate({text: n.text, id: id(n), textProcessor: {textProcessor}, checkLanguage:false}) YIELD result MERGE (n)-[:HAS_ANNOTATED_TEXT]->(result)";
        executeInTransaction(query, Collections.singletonMap("textProcessor", StubTextProcessor.class), emptyConsumer());

        String similQ = "MATCH (a:AnnotatedText) with collect(a) as list\n" +
                "CALL ga.nlp.ml.similarity.cosine({input:list})\n" +
                "YIELD result RETURN result";
        executeInTransaction(similQ, emptyConsumer());

        executeInTransaction("MATCH (n)-[r:SIMILARITY_COSINE]->() RETURN count(r) AS c", (result -> {
            assertTrue(result.hasNext());
            assertTrue((long) result.next().get("c") > 0);
        }));

    }

    private void importDataset() {
        String query =
                "LOAD CSV WITH HEADERS FROM \"https://raw.githubusercontent.com/davidmeza1/doctopics/master/data/llis.csv\" AS line\n" +
                "WITH line, SPLIT(line.LessonDate, '-') AS date LIMIT 2\n" +
                "CREATE (lesson:Lesson { name: toInteger(line.`LessonId`) } )\n" +
                "SET lesson.year = toInteger(date[0]),\n" +
                "    lesson.month = toInteger(date[1]),\n" +
                "    lesson.day = toInteger(date[2]),\n" +
                "    lesson.title = (line.Title),\n" +
                "    lesson.abstract = (line.Abstract),\n" +
                "    lesson.lesson = (line.Lesson),\n" +
                "    lesson.org = (line.MissionDirectorate),\n" +
                "    lesson.safety = (line.SafetyIssue),\n" +
                "    lesson.url = (line.url)";

        executeInTransaction(query, emptyConsumer());
        executeInTransaction("MATCH (n:Lesson) SET n.text = n.title + '. ' + n.abstract + '. ' + n.lesson", emptyConsumer());
    }
}
