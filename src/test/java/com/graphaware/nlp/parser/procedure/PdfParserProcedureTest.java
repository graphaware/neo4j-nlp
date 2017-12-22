package com.graphaware.nlp.parser.procedure;

import com.graphaware.nlp.NLPIntegrationTest;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class PdfParserProcedureTest extends NLPIntegrationTest {

    @Test
    public void testParsingPdfWithProcedure() {
        String file = "blogpost.pdf";
        executeInTransaction("CALL ga.nlp.parser.pdf({file})", Collections.singletonMap("file", file), (result -> {
            assertTrue(result.hasNext());
            while (result.hasNext()) {
                Map<String, Object> record = result.next();
                System.out.println(record.keySet());
                List<String> paras = (List<String>) record.get("paragraphs");
                if (paras.size() > 0) {
                    System.out.println(paras.get(0));
                }
            }
        }));
    }

}
