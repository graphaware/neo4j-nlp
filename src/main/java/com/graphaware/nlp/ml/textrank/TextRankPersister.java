/*
 * Copyright (c) 2013-2018 GraphAware
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
package com.graphaware.nlp.ml.textrank;

import com.graphaware.common.log.LoggerFactory;
import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.domain.Keyword;
import com.graphaware.nlp.persistence.persisters.KeywordPersister;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.graphaware.nlp.persistence.constants.Relationships.DESCRIBES;

public class TextRankPersister {

    private static final Log LOG = LoggerFactory.getLogger(TextRankPersister.class);

    private final Label keywordLabel;

    public TextRankPersister(Label keywordLabel) {
        this.keywordLabel = keywordLabel;
    }

    public void peristKeywords(Map<String, Keyword> results, Node annotatedText) {
        List<String> printKeywords = new ArrayList<>();
        KeywordPersister persister = NLPManager.getInstance().getPersister(Keyword.class);
        persister.setLabel(keywordLabel);
        results.entrySet().stream()
                .forEach(en -> {
                    // check keyword consistency
                    if (en.getKey().split("_").length > 2) {
                        LOG.warn("Tag " + en.getKey() + " has more than 1 underscore symbols, newly created " + keywordLabel.name() + " node might be wrong.");
                    }
                    Node newNode = persister.persist(en.getValue(), en.getKey(), String.valueOf(System.currentTimeMillis()));
                    if (newNode != null) {
                        //LOG.info("New node has labels: " + iterableToList(newNode.getLabels()).stream().map(l -> l.name()).collect(Collectors.joining(", ")));
                        Relationship rel = mergeRelationship(annotatedText, newNode);
                        rel.setProperty("count_exactMatch", en.getValue().getExactMatchCount());
                        rel.setProperty("count", en.getValue().getTotalCount());
                        rel.setProperty("relevance", en.getValue().getRelevance());
                    }
                    printKeywords.add(en.getKey().split("_")[0]);
                });
        LOG.debug("--- TextRank results: \n  " + printKeywords.stream().collect(Collectors.joining("\n  ")));
    }

    private Relationship mergeRelationship(Node annotatedText, Node newNode) {
        Relationship rel = null;
        Iterable<Relationship> itr = newNode.getRelationships(Direction.OUTGOING, DESCRIBES);
        for (Relationship r : itr) {
            if (r.getEndNode().equals(annotatedText)) {
                rel = r;
                break;
            }
        }
        if (rel == null) {
            rel = newNode.createRelationshipTo(annotatedText, DESCRIBES);
        }
        return rel;
    }
}
