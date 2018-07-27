package com.graphaware.nlp.parser.procedure;

import com.graphaware.nlp.NLPIntegrationTest;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ParserProcedureTest extends NLPIntegrationTest {

    @Test
    public void testParsingPdfWithProcedure() throws Exception {
        String f = getClass().getClassLoader().getResource("import/blogpost.pdf").getPath();
        System.out.println("Loading file from " + f);
        executeInTransaction("CALL ga.nlp.parser.pdf({file})", Collections.singletonMap("file", f), (result -> {
            assertTrue(result.hasNext());
        }));
    }

    @Test
    public void testParsingPdfAndStore() throws Exception {
        clearDb();
        String f = getClass().getClassLoader().getResource("import/blogpost.pdf").getPath();
        System.out.println("Loading file from " + f);
        executeInTransaction("CALL ga.nlp.parser.pdf({file})\n" +
                "YIELD number, paragraphs\n" +
                "UNWIND paragraphs AS paragraph\n" +
                "WITH number, paragraph WHERE trim(paragraph) <> \"\"\n" +
                "CREATE (d:Document) SET d.text = paragraph, d.pageNumber = number", Collections.singletonMap("file", f), (result -> {

        }));
        executeInTransaction("MATCH (d:Document) RETURN count(d) AS c", (result -> {
            assertTrue(result.hasNext());
            assertTrue((Long) result.next().get("c") > 0);
        }));
    }

    @Test
    public void testParsingRetrieveTitlesFromDocument() {
        clearDb();
        String f = getClass().getClassLoader().getResource("import/outfit-reco.pdf").getPath();
        System.out.println("Loading file from " + f);
        executeInTransaction("CALL ga.nlp.parser.pdf({file})\n" +
                "YIELD number, paragraphs\n" +
                "UNWIND range(0, size(paragraphs)-1) AS i\n" +
                "WITH number, i, paragraphs[i] AS paragraph WHERE trim(paragraph) <> \"\"\n" +
                "CREATE (d:Document) SET d.text = paragraph, d.paraNum = i, d.pageNumber = number", Collections.singletonMap("file", f), (result -> {

        }));
        executeInTransaction("MATCH (d:Document) RETURN count(d) AS c", (result -> {
            assertTrue(result.hasNext());
            assertTrue((Long) result.next().get("c") > 0);
        }));

        executeInTransaction("MATCH (d:Document {pageNumber: 1}) RETURN d.text AS  text", (result -> {
            String txt = result.next().get("text").toString();
            assertTrue(txt.startsWith("Recommending Outfits from Personal Closet"));
        }));
    }

    @Test
    public void testParsingAndStoreFromUrl() {
        clearDb();
        String f = "http://www.pdf995.com/samples/pdf.pdf";
        executeInTransaction("CALL ga.nlp.parser.pdf({file})\n" +
                "YIELD number, paragraphs\n" +
                "UNWIND paragraphs AS paragraph\n" +
                "WITH number, paragraph WHERE trim(paragraph) <> \"\"\n" +
                "CREATE (d:Document) SET d.text = paragraph, d.pageNumber = number", Collections.singletonMap("file", f), (result -> {

        }));
        executeInTransaction("MATCH (d:Document) RETURN count(d) AS c", (result -> {
            assertTrue(result.hasNext());
            assertTrue((Long) result.next().get("c") > 0);
        }));
    }

    @Test
    public void testParsingPowerpoint() {
        clearDb();
        String f = getClass().getClassLoader().getResource("test.pptx").getPath();
        System.out.println("Loading file from " + f);
        executeInTransaction("CALL ga.nlp.parser.powerpoint({file})\n" +
                "YIELD number, paragraphs\n" +
                "UNWIND paragraphs AS paragraph\n" +
                "WITH number, paragraph WHERE trim(paragraph) <> \"\"\n" +
                "CREATE (d:Document) SET d.text = paragraph, d.pageNumber = number", Collections.singletonMap("file", f), (result -> {

        }));
        executeInTransaction("MATCH (d:Document) RETURN count(d) AS c", (result -> {
            assertTrue(result.hasNext());
            assertTrue((Long) result.next().get("c") > 0);
        }));
    }

    @Test
    public void testParsingWordDoc() {
        clearDb();
        String f = getClass().getClassLoader().getResource("vui.docx").getPath();
        System.out.println("Loading file from " + f);
        executeInTransaction("CALL ga.nlp.parser.word({file})\n" +
                "YIELD number, paragraphs\n" +
                "UNWIND paragraphs AS paragraph\n" +
                "WITH number, paragraph WHERE trim(paragraph) <> \"\"\n" +
                "CREATE (d:Document) SET d.text = paragraph, d.pageNumber = number", Collections.singletonMap("file", f), (result -> {

        }));
        executeInTransaction("MATCH (d:Document) RETURN count(d) AS c", (result -> {
            assertTrue(result.hasNext());
            assertTrue((Long) result.next().get("c") > 0);
        }));
    }

    @Test
    public void testParsingVTT() {
        clearDb();
        String f = getClass().getClassLoader().getResource("transcript.vtt").getPath();
        executeInTransaction("CALL ga.nlp.parser.webvtt({p0})", buildSeqParameters(f), (result -> {
            assertTrue(result.hasNext());
            while (result.hasNext()) {
                Map<String, Object> record = result.next();
                assertTrue(record.get("startTime").toString().contains(":"));
            }
        }));
    }
}
