package com.graphaware.nlp.parser.procedure;

import com.graphaware.nlp.NLPIntegrationTest;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
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
    public void testParsingProtectedPdf() {
        String userAgent = "Mozilla/5.0 (Windows; U; Win98; en-US; rv:1.7.2) Gecko/20040803";
        String url = "https://www.ncbi.nlm.nih.gov/pmc/articles/PMC5854482/pdf/nihms949230.pdf";
        clearDb();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("url", url);
        parameters.put("ua", userAgent);
        executeInTransaction("CALL ga.nlp.parser.pdf($url, [], {UserAgent: $ua}) YIELD number, paragraphs RETURN number, paragraphs", parameters, (result -> {
            assertTrue(result.hasNext());
        }));
    }

    @Test
    public void testCustomUASettingIsUsedForProtectedPdf() throws Exception {
        String userAgent = "Mozilla/5.0 (Windows; U; Win98; en-US; rv:1.7.2) Gecko/20040803";
        executeInTransaction("CALL ga.nlp.config.set('SETTING_DEFAULT_UA', $ua)", Collections.singletonMap("ua", userAgent), emptyConsumer());
        String url = "https://www.ncbi.nlm.nih.gov/pmc/articles/PMC5854482/pdf/nihms949230.pdf";
        clearDb();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("url", url);
        executeInTransaction("CALL ga.nlp.parser.pdf($url) YIELD number, paragraphs RETURN number, paragraphs", parameters, (result -> {
            assertTrue(result.hasNext());
        }));
    }
}
