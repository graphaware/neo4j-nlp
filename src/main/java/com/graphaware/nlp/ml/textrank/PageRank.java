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
package com.graphaware.nlp.ml.textrank;

import com.google.common.util.concurrent.AtomicDouble;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageRank {

    private static final Logger LOG = LoggerFactory.getLogger(PageRank.class);

    protected final GraphDatabaseService database;

    public PageRank(GraphDatabaseService database) {
        this.database = database;
    }

    public Map<Long, Double> run(Map<Long, Map<Long, CoOccurrenceItem>> coOccurrences, int iter, double dampFactor, double threshold) {
        Map<Long, Double> nodeWeights = initializeNodeWeights(coOccurrences);
        Map<Long, Double> pagerank = getInitializedPageRank(nodeWeights, dampFactor);
        boolean thresholdHit = false;
        for (int iteration = 0; iteration < iter && !thresholdHit; iteration++) {
            Map<Long, Double> prTemp = new HashMap<>();
            // calculate main part of the PR calculation, include weights of nodes and relationships
            nodeWeights.keySet().stream().forEach((nodeIdExt) -> {
                AtomicDouble internalSum = new AtomicDouble(0.0);
                nodeWeights.keySet().stream()
                        .filter((nodeIdInt) -> coOccurrences.containsKey(nodeIdInt)
                        && coOccurrences.get(nodeIdInt).containsKey(nodeIdExt))
                        .forEach((nodeIdInt) -> {
                            Map<Long, CoOccurrenceItem> coOccurrentTags = coOccurrences.get(nodeIdInt);
                            //Can be optimized
                            double totalWeightSum = coOccurrentTags.values().stream().map(item -> item.getCount()).mapToDouble(Integer::intValue).sum();
                            internalSum.addAndGet(((1.0d * coOccurrentTags.get(nodeIdExt).getCount()) / totalWeightSum) * pagerank.get(nodeIdInt));
                        });
                double newPrValue = (1 - dampFactor) + dampFactor * internalSum.get();
                prTemp.put(nodeIdExt, newPrValue);
            });
            thresholdHit = checkThreshold(pagerank, prTemp, threshold);
            if (thresholdHit) {
                LOG.warn("Threshold hit after " + iter + " iterations");
            }
            // finish page rank computation and store it to the final list
            nodeWeights.keySet().stream().forEach((nodeIdExt) -> {
                pagerank.put(nodeIdExt, prTemp.get(nodeIdExt));
            });
        } // iterations
        return pagerank;
    }

    private Map<Long, Double> initializeNodeWeights(Map<Long, Map<Long, CoOccurrenceItem>> coOccurrences) {
        
        Map<Long, Double> nodeInitialWeights = new HashMap<>();
        coOccurrences.entrySet().stream().forEach((coOccurrence) -> {
            coOccurrence.getValue().entrySet().stream().forEach((entry) -> {
                nodeInitialWeights.put(entry.getValue().getSource(), 1.0d);
                nodeInitialWeights.put(entry.getValue().getDestination(), 1.0d);
            });
        });
        return nodeInitialWeights;
    }

    private Map<Long, Double> getInitializedPageRank(Map<Long, Double> nodeWeights, double damp) {
        Map<Long, Double> pageRank = new HashMap<>();
        int n = nodeWeights.size();
        nodeWeights.entrySet().stream().forEach((item) -> {
            pageRank.put(item.getKey(), (1. - damp) / n);
        });

        return pageRank;
    }

    private boolean checkThreshold(Map<Long, Double> pagerank, Map<Long, Double> prTemp, double threshold) {
        Iterator<Long> iterator = pagerank.keySet().iterator();
        while (iterator.hasNext()) {
            long nodeIdExt = iterator.next();
            double diff = prTemp.get(nodeIdExt) - pagerank.get(nodeIdExt);
            if (diff > threshold) {
                return false;
            }
        }
        return true;
    }

}
