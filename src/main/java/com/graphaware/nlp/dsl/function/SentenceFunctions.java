/*
 * Copyright (c) 2013-2017 GraphAware
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
package com.graphaware.nlp.dsl.function;

import com.graphaware.nlp.persistence.constants.Properties;
import com.graphaware.nlp.persistence.constants.Relationships;
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

public class SentenceFunctions {

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
