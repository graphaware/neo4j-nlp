/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.ml.pagerank;

import com.graphaware.nlp.dsl.PageRankRequest;
import com.graphaware.nlp.dsl.result.SingleResult;
import com.graphaware.nlp.ml.textrank.CoOccurrenceItem;
import com.graphaware.nlp.ml.textrank.PageRank;
import com.graphaware.nlp.processor.TextProcessorsManager;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ale
 */
public class PageRankProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TextProcessorsManager.class);

    private final GraphDatabaseService database;

    public PageRankProcessor(GraphDatabaseService database) {
        this.database = database;
    }

    public SingleResult process(PageRankRequest request) {
        String nodeType = request.getNodeType();
        String relType = request.getRelationshipType();
        String relWeight = request.getRelationshipWeight();
        int iter = request.getIteration().intValue();
        double damp = request.getDamp();
        double threshold = request.getThreshold();

        PageRank pagerank = new PageRank(database);
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
