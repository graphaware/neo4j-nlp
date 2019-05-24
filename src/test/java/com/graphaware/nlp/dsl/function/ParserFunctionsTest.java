package com.graphaware.nlp.dsl.function;

import com.graphaware.nlp.NLPIntegrationTest;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ParserFunctionsTest extends NLPIntegrationTest {

    @Test
    public void testFileParsingRaw() throws Exception {
        String fp = getClass().getClassLoader().getResource("rawfile.txt").getPath();

        String content = new String(Files.readAllBytes(Paths.get(fp)));

        executeInTransaction("RETURN ga.nlp.parse.raw($p0) AS text", buildSeqParameters(fp), (result -> {
            assertTrue(result.hasNext());
            String text = result.next().get("text").toString();
            assertEquals(content, text);
        }));
    }

}
