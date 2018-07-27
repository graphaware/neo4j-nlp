package com.graphaware.nlp.dsl;

import com.graphaware.nlp.NLPIntegrationTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class UtilsProcedureTest extends NLPIntegrationTest {

    @Test
    public void testListFilesProcedure() {
        String p = getClass().getClassLoader().getResource("dummy-dir-do-not-add-or-remove-files/").getPath();
        executeInTransaction("CALL ga.nlp.utils.listFiles({p0}) YIELD filePath RETURN count(*) AS c", buildSeqParameters(p), (result -> {
            assertTrue(result.hasNext());
            assertEquals(4L, result.next().get("c"));
        }));
    }

    @Test
    public void testListFilesProcedureWithFilter() {
        String p = getClass().getClassLoader().getResource("dummy-dir-do-not-add-or-remove-files/").getPath();
        executeInTransaction("CALL ga.nlp.utils.listFiles({p0}, '.vtt') YIELD filePath RETURN count(*) AS c", buildSeqParameters(p), (result -> {
            assertTrue(result.hasNext());
            assertEquals(1L, result.next().get("c"));
        }));
    }

    @Test
    public void testWalkdirProcedure() {
        String p = getClass().getClassLoader().getResource("dummy-dir-do-not-add-or-remove-files/").getPath();
        executeInTransaction("CALL ga.nlp.utils.walkdir({p0}) YIELD filePath RETURN count(*) AS c", buildSeqParameters(p), (result -> {
            assertTrue(result.hasNext());
            assertEquals(5L, result.next().get("c"));
        }));
    }

}
