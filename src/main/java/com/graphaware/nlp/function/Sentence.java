package com.graphaware.nlp.function;

import com.graphaware.nlp.domain.Properties;
import com.graphaware.nlp.persistence.Relationships;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sentence {

    @UserFunction("ga.nlp.sentence.nextTags")
    @Description("Returns a list of Tag nodes that appear just after the given Tag in a sentence along with the frequency")
    public List<Map<String, Object>> nextTags(@Name("from") Node from) {
        Map<Long, Integer> freqMap = new HashMap<>();
        Map<Long, Node> references = new HashMap<>();

        for (Relationship rel : from.getRelationships(Relationships.TAG_OCCURRENCE_TAG, Direction.INCOMING)) {
            Node tagOccurence = rel.getStartNode();
            int minPosition = (int) tagOccurence.getProperty(Properties.END_POSITION);

            Node sentence = tagOccurence.getSingleRelationship(Relationships.SENTENCE_TAG_OCCURRENCE, Direction.INCOMING).getStartNode();
            for (Relationship rel2 : sentence.getRelationships(Relationships.SENTENCE_TAG_OCCURRENCE, Direction.OUTGOING)) {
                Node occurence = rel2.getEndNode();
                int occPosition = (int) occurence.getProperty(Properties.START_POSITION);
                if (occPosition == (minPosition + 1) ) {
                    Node tag = occurence.getSingleRelationship(Relationships.TAG_OCCURRENCE_TAG, Direction.OUTGOING).getEndNode();
                    Integer freq = freqMap.containsKey(tag.getId()) ? freqMap.get(tag.getId()) + 1 : 1;
                    freqMap.put(tag.getId(), freq);
                }
            }
        }

        List<Map<String, Object>> response = new ArrayList<>();
        for (Long l : references.keySet()) {
            Map<String, Object> m = new HashMap<>();
            m.put("node", references.get(l));
            m.put("frequency", freqMap.get(l));
            response.add(m);
        }

        return response;
    }

}
