package com.graphaware.nlp.parser.procedure;

import com.graphaware.nlp.NLPIntegrationTest;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class PdfParserProcedureTest extends NLPIntegrationTest {

    @Test
    public void testParsingPdfWithProcedure() throws Exception {
        String f = getClass().getClassLoader().getResource("blogpost.pdf").toURI().toString();
        executeInTransaction("CALL ga.nlp.parser.pdf({file})", Collections.singletonMap("file", f), (result -> {
            assertTrue(result.hasNext());
        }));
    }

    @Test
    public void testParsingPdfAndStore() throws Exception {
        clearDb();
        String f = getClass().getClassLoader().getResource("blogpost.pdf").toURI().toString();
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

}
