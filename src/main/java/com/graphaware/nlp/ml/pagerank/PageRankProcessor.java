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
package com.graphaware.nlp.ml.pagerank;

import com.graphaware.nlp.annotation.NLPModuleExtension;
import com.graphaware.nlp.dsl.request.PageRankRequest;
import com.graphaware.nlp.dsl.result.SingleResult;
import com.graphaware.nlp.extension.AbstractExtension;
import com.graphaware.nlp.extension.NLPExtension;
import com.graphaware.nlp.ml.textrank.CoOccurrenceItem;
import com.graphaware.nlp.ml.textrank.PageRank;
import com.graphaware.nlp.processor.TextProcessorsManager;
import org.neo4j.logging.Log;
import com.graphaware.common.log.LoggerFactory;

import java.util.Map;

@NLPModuleExtension(name = "PageRankProcessor")
public class PageRankProcessor extends AbstractExtension implements NLPExtension {

    private static final Log LOG = LoggerFactory.getLogger(TextProcessorsManager.class);

    public SingleResult process(PageRankRequest request) {
        String nodeType = request.getNodeType();
        String relType = request.getRelationshipType();
        String relWeight = request.getRelationshipWeight();
        int iter = request.getIteration().intValue();
        double damp = request.getDamp();
        double threshold = request.getThreshold();
        boolean respectDirections = request.getRespectDirections();

        PageRank pagerank = new PageRank(getDatabase());
        pagerank.respectDirections(respectDirections);
        Map<Long, Map<Long, CoOccurrenceItem>> coOccurrences = pagerank.processGraph(nodeType, relType, relWeight);
        if (coOccurrences.isEmpty()) {
            return SingleResult.fail();
        }
        Map<Long, Double> pageranks = pagerank.run(coOccurrences, iter, damp, threshold);
        if (pageranks.isEmpty()) {
            return SingleResult.fail();
        }
        pageranks.entrySet().stream().forEach(en -> LOG.info("PR(" + en.getKey() + ") = " + en.getValue()));
        LOG.info("Sum of PageRanks: " + pageranks.values().stream().mapToDouble(Number::doubleValue).sum());
        pagerank.storeOnGraph(pageranks, nodeType);

        return SingleResult.success();
    }
}
