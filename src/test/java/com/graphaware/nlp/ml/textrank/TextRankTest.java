/*
 * Copyright (c) 2013-2016 GraphAware
 *
 * This file is part of the GraphAware Framework.
 *
 * GraphAware Framework is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package com.graphaware.nlp.procedure.textrank;

import com.graphaware.nlp.NLPIntegrationTest;
//import com.graphaware.nlp.extension.AbstractExtension;
import com.graphaware.nlp.ml.textrank.TextRank;
import com.graphaware.nlp.ml.textrank.CoOccurrenceItem;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
//import org.neo4j.kernel.internal.GraphDatabaseAPI;
//import org.neo4j.kernel.impl.proc.Procedures;

public class TextRankTest extends NLPIntegrationTest {

    private static final List<String> expectedKeywords = Arrays.asList("flight failure", "speed brake", "space shuttle", "ground operation", "actuator", "installation", "flight", "gear", "shuttle", "brake", "speed", "failure", "unusual", "design");
    private static final String annIdStr = "\"test118\"";

    /**
     * Test of TextRank procedure of class TextRank.
     */
    @Test
    public void testTextRank() {
        createGraph();

        // run TextRank
        try (Transaction tx = getDatabase().beginTx()) {
            /*Result result = getDatabase().execute("MATCH (a:AnnotatedText) where a.id = " + annIdStr + "\n"
                    + "CALL ga.nlp.ml.textRank({annotatedText:a}) YIELD result\n"
                    + "return result;");
            ResourceIterator<Object> rowIterator = result.columnAs("result");
            assertTrue(rowIterator.hasNext());
            assertTrue("TextRank didn't return SUCCESS. Return value = " + (String)result.next().get("result"), ((String)result.next().get("result")).toLowerCase().equals("success"));*/
            Result result = getDatabase().execute("match (a:AnnotatedText {id: " + annIdStr + "}) return a");
            assertTrue("TextRank: didn't find AnnotatedText (error in graph initialization).", result.hasNext());
            if (!result.hasNext())
                return;
            Node annText = (Node) result.next().get("a");
            TextRank textrank = new TextRank(getDatabase(), getNLPManager().getConfiguration());
            Map<Long, Map<Long, CoOccurrenceItem>> coOccurrence = textrank.createCooccurrences(annText);
            boolean res = textrank.evaluate(annText, coOccurrence, 30, 0.85, 0.0001);
            assertTrue("TextRank failed, returned false.", res);
            tx.success();
        } catch (Exception e) {
            assertTrue("TextRank failed: " + e.getMessage(), false);
            return;
        }

        // evaluate results
        try (Transaction tx = getDatabase().beginTx()) {
            Result result = getDatabase().execute(
                "MATCH (k:Keyword)-[:DESCRIBES]->(a:AnnotatedText)\n"
                + "WHERE a.id = " + annIdStr + "\n"
                + "RETURN k.id AS id, k.value AS value\n");
            int totCount  = 0;
            int totCount_phrases = 0;
            int trueCount = 0;
            while (result!=null && result.hasNext()) {
                Map<String, Object> next = result.next();
                String tag = (String) next.get("value");
                totCount++;
                if (tag.split(" ").length > 1)
                    totCount_phrases++;
                if (expectedKeywords.contains(tag))
                    trueCount++;
                //assertTrue("Found unexpected keyword: " + tag, expectedKeywords.contains(tag));
            }
            //assertEquals("Some keywords are missing.", expectedKeywords.size(), trueCount);
            assertTrue("TextRank evaluation: didn't find any keywords!", totCount > 0);
            assertTrue("TextRank evaluation: didn't find any keyphrases!", totCount_phrases > 0);
            assertTrue("TextRank evaluation: didn't find any expected keyword!", trueCount > 0);
            tx.success();
        } catch (Exception e) {
            assertTrue("Evaluation of TextRank results failed: " + e.getMessage(), false);
            return;
        }

        // clean after ourselves
        getDatabase().execute("MATCH (n)-[r]-() DETACH DELETE n, r");

    }

    private void createGraph() {
        // use this query to create a string for creating graph below
        /*match (l:Lesson)-[:HAS_ANNOTATED_TEXT]->(a:AnnotatedText) where l.name=1760 
        match (a)-[:CONTAINS_SENTENCE]->(s:Sentence)-[:SENTENCE_TAG_OCCURRENCE]->(to:TagOccurrence)-[:TAG_OCCURRENCE_TAG]->(t:Tag)
        return '+ "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: ' + s.sentenceNumber + '})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: ' + to.startPosition + ', endPosition: ' + to.endPosition + '})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \\"' + t.id + '\\", value: \\"' + t.value + '\\"})\\n"'  */
        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().execute("MATCH (n)-[r]-() DELETE n, r");
            getDatabase().execute(
                "create (at:AnnotatedText {id: " + annIdStr + "})-[:TestRel]->(d:Test {value: \"D\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 10})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1183, endPosition: 1210})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"Process Control Focus Group_en\", value: \"Process Control Focus Group\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 10})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1102, endPosition: 1115})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"This 4-minute_en\", value: \"This 4-minute\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 10})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1167, endPosition: 1174})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"program_en\", value: \"program\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 10})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1127, endPosition: 1132})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"video_en\", value: \"video\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 10})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1159, endPosition: 1166})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"shuttle_en\", value: \"shuttle\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 10})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1117, endPosition: 1126})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"33-second_en\", value: \"33-second\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 10})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1153, endPosition: 1158})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"space_en\", value: \"space\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 10})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1138, endPosition: 1145})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"product_en\", value: \"product\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 12})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1220, endPosition: 1230})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"mechanical_en\", value: \"mechanical\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 12})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1280, endPosition: 1288})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"assembly_en\", value: \"assembly\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 12})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1349, endPosition: 1359})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"inspection_en\", value: \"inspection\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 12})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1381, endPosition: 1387})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"flight_en\", value: \"flight\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 12})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1293, endPosition: 1301})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"assembly_en\", value: \"assembly\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 12})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1320, endPosition: 1327})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"clearly_en\", value: \"clearly\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 12})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1388, endPosition: 1395})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"failure_en\", value: \"failure\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 12})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1262, endPosition: 1269})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"prevent_en\", value: \"prevent\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 12})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1328, endPosition: 1335})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"evident_en\", value: \"evident\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 12})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1231, endPosition: 1238})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"design_en\", value: \"design\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 12})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1270, endPosition: 1279})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"incorrect_en\", value: \"incorrect\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 12})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1364, endPosition: 1369})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"cause_en\", value: \"cause\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 12})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1213, endPosition: 1219})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"unless_en\", value: \"unless\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 12})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1372, endPosition: 1380})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"critical_en\", value: \"critical\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 12})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1302, endPosition: 1307})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"error_en\", value: \"error\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 12})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1243, endPosition: 1258})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"mistake-proofed_en\", value: \"mistake-proofed\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 12})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1341, endPosition: 1348})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"initial_en\", value: \"initial\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 9})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1021, endPosition: 1036})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"mistake-proof_en\", value: \"mistake-proof\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 9})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1066, endPosition: 1075})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"assemble_en\", value: \"assemble\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 9})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1003, endPosition: 1010})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"design_en\", value: \"design\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 9})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1045, endPosition: 1055})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"component_en\", value: \"component\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 9})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1076, endPosition: 1087})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"incorrectly_en\", value: \"incorrectly\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 7})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 859, endPosition: 865})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"ground_en\", value: \"ground\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 7})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 794, endPosition: 801})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"process_en\", value: \"process\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 7})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 831, endPosition: 843})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"accommodate_en\", value: \"accommodate\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 7})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 814, endPosition: 825})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"implement_en\", value: \"implement\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 7})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 766, endPosition: 771})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"gear_en\", value: \"gear\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 7})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 866, endPosition: 876})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"operation_en\", value: \"operation\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 7})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 848, endPosition: 855})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"demand_en\", value: \"demand\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 7})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 802, endPosition: 809})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"control_en\", value: \"control\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 7})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 762, endPosition: 765})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"new_en\", value: \"new\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 7})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 777, endPosition: 786})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"install_en\", value: \"install\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 8})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 967, endPosition: 979})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"installation_en\", value: \"installation\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 8})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 936, endPosition: 944})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"physical_en\", value: \"physical\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 8})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 922, endPosition: 930})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"actuator_en\", value: \"actuator\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 8})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 890, endPosition: 897})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"fixture_en\", value: \"fixture\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 8})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 987, endPosition: 996})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"incorrect_en\", value: \"incorrect\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 8})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 959, endPosition: 966})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"prevent_en\", value: \"prevent\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 8})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 880, endPosition: 889})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"dedicated_en\", value: \"dedicated\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 8})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 997, endPosition: 1001})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"gear_en\", value: \"gear\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 8})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 945, endPosition: 953})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"feature_en\", value: \"feature\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 8})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 902, endPosition: 912})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"fabricate_en\", value: \"fabricate\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 6})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 681, endPosition: 690})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"certify_en\", value: \"certify\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 6})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 647, endPosition: 653})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"sooner_en\", value: \"sooner\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 6})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 727, endPosition: 734})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"shuttle_en\", value: \"shuttle\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 6})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 748, endPosition: 751})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"100_en\", value: \"100\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 6})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 715, endPosition: 719})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"life_en\", value: \"life\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 6})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 699, endPosition: 707})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"original_en\", value: \"original\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 6})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 666, endPosition: 675})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"actuator_en\", value: \"actuator\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 6})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 752, endPosition: 760})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"mission_en\", value: \"mission\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 6})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 736, endPosition: 744})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"10 years_en\", value: \"10 years\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 6})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 708, endPosition: 714})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"flight_en\", value: \"flight\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 6})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 641, endPosition: 646})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"find_en\", value: \"find\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 6})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 625, endPosition: 632})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"mistake_en\", value: \"mistake\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 555, endPosition: 562})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"operate_en\", value: \"operate\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 525, endPosition: 531})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"robust_en\", value: \"robust\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 604, endPosition: 611})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"without_en\", value: \"without\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 591, endPosition: 593})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"30_en\", value: \"30\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 547, endPosition: 551})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"able_en\", value: \"able\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 594, endPosition: 602})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"mission_en\", value: \"mission\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 532, endPosition: 541})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"actuator_en\", value: \"actuator\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 612, endPosition: 619})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"failure_en\", value: \"failure\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 567, endPosition: 575})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"20 years_en\", value: \"20 years\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 577, endPosition: 590})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"approximately_en\", value: \"approximately\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 4})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 413, endPosition: 418})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"error_en\", value: \"error\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 4})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 507, endPosition: 519})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"topography_en\", value: \"topography\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 4})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 475, endPosition: 480})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"gear_en\", value: \"gear\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 4})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 490, endPosition: 499})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"different_en\", value: \"different\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 4})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 441, endPosition: 450})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"identical_en\", value: \"identical\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 4})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 500, endPosition: 506})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"design_en\", value: \"design\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 4})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 465, endPosition: 474})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"component_en\", value: \"component\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 4})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 419, endPosition: 426})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"stem_en\", value: \"stem\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 4})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 451, endPosition: 461})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"appearance_en\", value: \"appearance\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 4})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 436, endPosition: 440})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"near_en\", value: \"near\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 4})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 400, endPosition: 412})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"installation_en\", value: \"installation\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 1})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 117, endPosition: 123})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"panel_en\", value: \"panel\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 1})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 179, endPosition: 186})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"landing_en\", value: \"landing\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 1})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 146, endPosition: 151})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"space_en\", value: \"space\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 1})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 82, endPosition: 86})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"tail_en\", value: \"tail\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 1})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 111, endPosition: 116})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"brake_en\", value: \"brake\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 1})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 137, endPosition: 141})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"slow_en\", value: \"slow\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 1})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 105, endPosition: 110})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"speed_en\", value: \"speed\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 1})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 167, endPosition: 174})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"descent_en\", value: \"descent\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 1})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 127, endPosition: 132})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"steer_en\", value: \"steer\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 1})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 61, endPosition: 70})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"actuator_en\", value: \"actuator\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 1})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 152, endPosition: 159})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"shuttle_en\", value: \"shuttle\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 1})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 87, endPosition: 93})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"enable_en\", value: \"enable\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 1})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 98, endPosition: 104})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"rudder_en\", value: \"rudder\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 1})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 71, endPosition: 77})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"inside_en\", value: \"inside\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 0})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 8, endPosition: 12})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"clip_en\", value: \"clip\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 0})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 44, endPosition: 51})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"process_en\", value: \"process\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 0})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 13, endPosition: 19})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"lesson_en\", value: \"lesson\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 0})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 20, endPosition: 27})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"learn_en\", value: \"learn\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 0})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 35, endPosition: 40})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"topic_en\", value: \"topic\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 0})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 52, endPosition: 59})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"control_en\", value: \"control\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 0})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 2, endPosition: 7})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"video_en\", value: \"video\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 3})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 375, endPosition: 381})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"flight_en\", value: \"flight\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 3})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 330, endPosition: 335})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"gear_en\", value: \"gear\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 3})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 284, endPosition: 293})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"discovery_en\", value: \"discovery\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 3})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 366, endPosition: 374})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"critical_en\", value: \"critical\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 3})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 382, endPosition: 389})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"failure_en\", value: \"failure\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 3})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 351, endPosition: 362})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"incorrectly_en\", value: \"incorrectly\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 3})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 320, endPosition: 325})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"build_en\", value: \"build\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 3})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 308, endPosition: 315})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"Shuttle_en\", value: \"Shuttle\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 3})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 275, endPosition: 278})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"lead_en\", value: \"lead\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 3})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 390, endPosition: 394})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"mode_en\", value: \"mode\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 3})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 341, endPosition: 350})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"install_en\", value: \"install\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 205, endPosition: 210})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"find_en\", value: \"find\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 238, endPosition: 247})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"actuator_en\", value: \"actuator\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 218, endPosition: 224})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"planet_en\", value: \"planet\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 188, endPosition: 195})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"unusual_en\", value: \"unusual\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 258, endPosition: 268})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"inspection_en\", value: \"inspection\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 196, endPosition: 200})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"wear_en\", value: \"wear\"})\n"
    + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 225, endPosition: 230})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"gear_en\", value: \"gear\"})\n"
            );
            tx.success();
        } catch (Exception e) {
            assertTrue("TextRankTest: error while initialising graph", true);
        }
    }

}
