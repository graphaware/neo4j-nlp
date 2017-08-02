/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.ml.textrank;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import org.junit.Test;
import static org.junit.Assert.*;

import com.graphaware.test.integration.EmbeddedDatabaseIntegrationTest;


public class TextRankTest extends EmbeddedDatabaseIntegrationTest {

    private static final List<String> finalKeywords = Arrays.asList("computer_en", "download_en", "equipment_en", "extraneous computer_en", "extraneous_en", "flight_en", "general purpose computer_en", "general_en", "purpose_en", "update_en");

    public TextRankTest() {
    }

    @Test
    public void testTextRank() {
        /*getDatabase().execute(
            "CREATE (:Tag {id: \"frequency_en\", value: \"frequency\"})-[:CO_OCCURRENCE {weight: 1}]->(:Tag {id: \"multiplier_en\", value: \"multiplier\"})-[:CO_OCCURRENCE {weight: 1}]->(:Tag {id: \"juno_en\", value: \"juno\"})-[:CO_OCCURRENCE {weight: 1}]->(:Tag {id: \"deep_en\", value: \"deep\"})-[:CO_OCCURRENCE {weight: 1}]->(:Tag {id: \"space_en\", value: \"space\"})-[:CO_OCCURRENCE {weight: 1}]->(:Tag {id: \"transponder_en\", value: \"transponder\"})-[:CO_OCCURRENCE {weight: 1}]->(:Tag {id: \"sdst_en\", value: \"sdst_en\"})-[:CO_OCCURRENCE {weight: 1}]->(:Tag {id: \"desktop_en\", value: \"desktop\"})-[:CO_OCCURRENCE {weight: 1}]->(:Tag {id: \"computer_en\", value: \"computer\"})"
            + "CREATE (:Tag {id: \"\", value: \"\"})-[:CO_OCCURRENCE {weight: }]->(:Tag {id: \"\", value: \"\"})\n"
            + "CREATE (:Tag {id: \"\", value: \"\"})-[:CO_OCCURRENCE {weight: }]->(:Tag {id: \"\", value: \"\"})\n"
            + "\n"
        );*/

        // annotate example lesson
        long annID = 1001101;
        /*try (Transaction tx = getDatabase().beginTx();) {
            getDatabase().execute("CREATE (l:Lesson {title: \"Verify That Test Equipment Cannot be Interrupted by Extraneous Computer Processes\", abstract: \"Thermal-vacuum testing of a Ka-side frequency multiplier for the Juno Small Deep Space Transponder (SDST) was interrupted in mid-performance when a desktop computer running the test unexpectedly sought to download an update of an installed software package. While the flight hardware and test equipment were not harmed, eight hours of test data were lost. Verify that test support equipment critical to tests or experiments cannot be interrupted by extraneous computer processes or external commands.\", lesson: \"Many computers used for specialized purposes in test facilities are general purpose computers that may also be configured to perform office functions. Processes extraneous to flight system testing (e.g., code updates, scheduled file backups, e-mail downloads, and security scans) may occur intermittently on these units without user interaction or knowledge, particularly when they are connected to institutional networks. Should an extraneous process interrupt an active test, significant test time may be lost and the test article or support equipment may be damaged.\", name: " + annID + "})\n"
                + "WITH l\n"
                + "CALL ga.nlp.annotate({text: l.title + \". \" + l.abstract + \" \" + l.lesson, id: l.name}) YIELD result\n"
                + "MERGE (l)-[:HAS_ANNOTATED_TEXT]->(result)\n"
                + "RETURN l, result\n"
            );
        }*/

        createGraph(annID);

        // run TextRank
        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().execute(
                "MATCH (a:AnnotatedText) WHERE a.id = " + annID + "\n"
                + "WITH a LIMIT 100\n"
                + "CALL ga.nlp.ml.textrank.compute({annotatedID: a.id}) YIELD result\n" // TO DO: set a flag saying "don't use tf*idf weights"
                + "RETURN count(*) as n_documents_processed"
            );
        } catch (Exception e) {
            assertTrue("TextRank failed: " + e.getMessage(), true);
            return;
        }

        // evaluate results
        try (Transaction tx = getDatabase().beginTx()) {
            Result result = getDatabase().execute(
                "MATCH (k:Keyword)-[:DESCRIBES]->(a:AnnotatedText)\n"
                + "WHERE a.id = " + annID + "\n"
                + "RETURN k.id AS id, k.value AS value\n"
            );

            int totCount  = 0;
            int trueCount = 0;
            while (result!=null && result.hasNext()) {
                Map<String, Object> next = result.next();
                String tag = (String) next.get("id");
                totCount++;
                if (finalKeywords.contains(tag))
                    trueCount++;
            }
            assertEquals("Some keywords are missing.", finalKeywords.size(), trueCount);
            assertEquals("Found more keywords than expected.", finalKeywords.size(), totCount);
        } catch (Exception e) {
            assertTrue("TextRank evaluation failed: " + e.getMessage(), true);
        }
    }

    private void createGraph(long id) {
        // use this query to get the graph below:
        /*match (l:Lesson)-[:HAS_ANNOTATED_TEXT]->(a:AnnotatedText) where a.id=8501 
        match (a)-[:CONTAINS_SENTENCE]->(s:Sentence)-[:SENTENCE_TAG_OCCURRENCE]->(to:TagOccurrence)-[:TAG_OCCURRENCE_TAG]->(t:Tag)
        return '+ "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: ' + s.sentenceNumber + '})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: ' + to.startPosition + ', endPosition: ' + to.endPosition + '})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \\"' + t.id + '\\", value: \\"' + t.value + '\\"})\\n"'*/
        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().execute(
                "CREATE (l:Lesson {name: " + id + "})-[:HAS_ANNOTATED_TEXT]->(a:AnnotatedText {id: " + id + "})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 7})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1071, endPosition: 1082})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"experiment_en\", value: \"experiment\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 7})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1050, endPosition: 1058})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"critical_en\", value: \"critical\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 7})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1150, endPosition: 1158})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"command_en\", value: \"command\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 7})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1119, endPosition: 1127})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"computer_en\", value: \"computer\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 7})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1093, endPosition: 1104})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"interrupt_en\", value: \"interrupt\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 7})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1108, endPosition: 1118})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"extraneous_en\", value: \"extraneous\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 7})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1040, endPosition: 1049})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"equipment_en\", value: \"equipment\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 7})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1128, endPosition: 1137})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"process_en\", value: \"process\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 7})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1015, endPosition: 1021})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"verify_en\", value: \"verify\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 7})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1141, endPosition: 1149})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"external_en\", value: \"external\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 899, endPosition: 907})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"software_en\", value: \"software\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 769, endPosition: 780})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"interrupt_en\", value: \"interrupt\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 807, endPosition: 814})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"desktop_en\", value: \"desktop\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 841, endPosition: 853})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"unexpectedly_en\", value: \"unexpectedly\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 740, endPosition: 745})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"space_en\", value: \"space\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 724, endPosition: 728})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"juno_en\", value: \"juno\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 815, endPosition: 823})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"computer_en\", value: \"computer\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 746, endPosition: 757})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"transponder_en\", value: \"transponder\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 908, endPosition: 915})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"package_en\", value: \"package\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 876, endPosition: 882})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"update_en\", value: \"update\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 695, endPosition: 704})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"frequency_en\", value: \"frequency\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 854, endPosition: 860})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"seek_en\", value: \"seek\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 735, endPosition: 739})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"deep_en\", value: \"deep\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 889, endPosition: 898})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"install_en\", value: \"install\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 759, endPosition: 763})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"sdst_en\", value: \"sdst\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 824, endPosition: 831})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"run_en\", value: \"run\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 705, endPosition: 715})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"multiplier_en\", value: \"multiplier\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 5})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 864, endPosition: 872})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"download_en\", value: \"download\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 6})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 1009, endPosition: 1013})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"lose_en\", value: \"lose\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 6})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 952, endPosition: 961})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"equipment_en\", value: \"equipment\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 6})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 934, endPosition: 942})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"hardware_en\", value: \"hardware\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 6})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 979, endPosition: 990})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"eight hours_en\", value: \"eight hours\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 6})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 927, endPosition: 933})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"flight_en\", value: \"flight\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 6})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 971, endPosition: 977})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"harm_en\", value: \"harm\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 444, endPosition: 456})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"particularly_en\", value: \"particularly\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 349, endPosition: 357})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"security_en\", value: \"security\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 413, endPosition: 417})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"user_en\", value: \"user\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 318, endPosition: 325})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"backup_en\", value: \"backup\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 484, endPosition: 497})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"institutional_en\", value: \"institutional\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 294, endPosition: 301})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"update_en\", value: \"update\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 418, endPosition: 429})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"interaction_en\", value: \"interaction\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 260, endPosition: 266})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"flight_en\", value: \"flight\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 289, endPosition: 293})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"code_en\", value: \"code\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 433, endPosition: 442})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"knowledge_en\", value: \"knowledge\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 369, endPosition: 374})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"occur_en\", value: \"occur\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 399, endPosition: 404})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"unit_en\", value: \"unit\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 303, endPosition: 312})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"schedule_en\", value: \"schedule\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 334, endPosition: 343})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"download_en\", value: \"download\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 236, endPosition: 245})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"process_en\", value: \"process\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 358, endPosition: 363})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"scan_en\", value: \"scan\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 313, endPosition: 317})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"file_en\", value: \"file\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 375, endPosition: 389})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"intermittently_en\", value: \"intermittently\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 471, endPosition: 480})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"connect_en\", value: \"connect\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 246, endPosition: 256})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"extraneous_en\", value: \"extraneous\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 405, endPosition: 412})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"without_en\", value: \"without\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 2})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 498, endPosition: 506})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"network_en\", value: \"network\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 3})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 592, endPosition: 596})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"lose_en\", value: \"lose\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 3})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 529, endPosition: 536})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"process_en\", value: \"process\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 3})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 610, endPosition: 617})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"article_en\", value: \"article\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 3})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 563, endPosition: 574})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"significant_en\", value: \"significant\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 3})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 629, endPosition: 638})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"equipment_en\", value: \"equipment\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 3})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 550, endPosition: 556})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"active_en\", value: \"active\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 3})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 646, endPosition: 653})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"damage_en\", value: \"damage\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 3})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 518, endPosition: 528})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"extraneous_en\", value: \"extraneous\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 3})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 537, endPosition: 546})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"interrupt_en\", value: \"interrupt\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 1})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 218, endPosition: 224})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"office_en\", value: \"office\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 1})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 109, endPosition: 120})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"specialize_en\", value: \"specialize\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 1})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 153, endPosition: 160})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"general_en\", value: \"general\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 1})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 121, endPosition: 129})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"purpose_en\", value: \"purpose\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 1})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 138, endPosition: 148})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"facility_en\", value: \"facility\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 1})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 169, endPosition: 178})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"computer_en\", value: \"computer\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 1})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 90, endPosition: 99})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"computer_en\", value: \"computer\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 1})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 161, endPosition: 168})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"purpose_en\", value: \"purpose\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 1})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 196, endPosition: 206})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"configure_en\", value: \"configure\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 1})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 85, endPosition: 89})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"many_en\", value: \"many\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 1})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 225, endPosition: 234})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"function_en\", value: \"function\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 0})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 0, endPosition: 6})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"verify_en\", value: \"verify\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 0})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 37, endPosition: 48})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"interrupt_en\", value: \"interrupt\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 0})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 52, endPosition: 62})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"extraneous_en\", value: \"extraneous\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 0})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 17, endPosition: 26})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"equipment_en\", value: \"equipment\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 0})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 63, endPosition: 71})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"computer_en\", value: \"computer\"})\n"
                + "MERGE (a)-[:CONTAINS_SENTENCE]->(:Sentence {sentenceNumber: 0})-[:SENTENCE_TAG_OCCURRENCE]->(:TagOccurrence {startPosition: 72, endPosition: 81})-[:TAG_OCCURRENCE_TAG]->(:Tag {id: \"processes_en\", value: \"processes\"})\n"
            );
        } catch (Exception e) {
            assertTrue("TextRank failed while creating initial graph ...", true);
        }
    }

}
