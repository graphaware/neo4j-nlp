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
package com.graphaware.nlp.ml.textrank;

import com.google.common.util.concurrent.AtomicDouble;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PageRank {

    private static final Logger LOG = LoggerFactory.getLogger(PageRank.class);

    protected final GraphDatabaseService database;
    private Map<Long, Double> nodeWeights;
    private boolean directionsMatter;

    public PageRank(GraphDatabaseService database) {
        this.database = database;
        this.directionsMatter = true;
    }

    public void setNodeWeights(Map<Long, Double> w) {
        this.nodeWeights = w;
    }

    public void respectDirections(boolean respectDirections) {
        this.directionsMatter = respectDirections;
    }

    public Map<Long, Double> run(Map<Long, Map<Long, CoOccurrenceItem>> coOccurrences, int iter, double dampFactor, double threshold) {
        nodeWeights = initializeNodeWeights(coOccurrences);
        Map<Long, Double> pagerank = getInitializedPageRank(nodeWeights, dampFactor);
        int nNodes = pagerank.size();
        boolean thresholdHit = false;
        Map<Long, Double> prTemp = new HashMap<>();
        for (int iteration = 0; iteration < iter && !thresholdHit; iteration++) {
            //Map<Long, Double> prTemp = new HashMap<>();
            // calculate main part of the PR calculation, include weights of nodes and relationships
            nodeWeights.entrySet().stream().forEach(enExt -> {
                Long nodeIdExt = enExt.getKey();
                Double nodeWExt = enExt.getValue();
                AtomicDouble internalSum = new AtomicDouble(0.0);
                //AtomicDouble internalNodeWSum = new AtomicDouble(0.0);
                nodeWeights.entrySet().stream()
                        .filter(enInt -> coOccurrences.containsKey(enInt.getKey()) && coOccurrences.get(enInt.getKey()).containsKey(nodeIdExt))
                        .forEach(enInt -> {
                            Long nodeIdInt = enInt.getKey();
                            Double nodeWInt = enInt.getValue();
                            //internalNodeWSum.addAndGet(nodeWInt);
                            Map<Long, CoOccurrenceItem> coOccurrentTags = coOccurrences.get(nodeIdInt);
                            //Can be optimized
                            double totalWeightSum = coOccurrentTags.values().stream().map(item -> item.getCount()).mapToDouble(Number::doubleValue).sum();
                            //internalSum.addAndGet(1.0d/coOccurrentTags.size() * pagerank.get(nodeIdInt)); // no relationship weights
                            internalSum.addAndGet(((1.0d * coOccurrentTags.get(nodeIdExt).getCount()) / totalWeightSum) * pagerank.get(nodeIdInt)); // with relationship weights
                            //internalSum.addAndGet(((1.0d * coOccurrentTags.get(nodeIdExt).getCount()) / totalWeightSum) * pagerank.get(nodeIdInt) * nodeWInt); // with relationship & node weights
                        });
                double newPrValue = (1 - dampFactor) / nNodes + dampFactor * internalSum.get(); // PR is a probability (PR values add up to 1)

                // PageRank with node weights
                //long nInt = nodeWeights.entrySet().stream()
                //        .filter(enInt -> coOccurrences.containsKey(enInt.getKey()) && coOccurrences.get(enInt.getKey()).containsKey(nodeIdExt))
                //        .count();
                //double newPrValue = (1 - dampFactor) / nNodes + dampFactor * internalSum.get() * (nInt / internalNodeWSum.get()); // PR is a probability (PR values add up to 1); WITH node weights
                prTemp.put(nodeIdExt, newPrValue);
            });
            thresholdHit = checkThreshold(pagerank, prTemp, threshold);
            if (thresholdHit) {
                LOG.warn("Threshold hit after " + (iteration + 1) + " iterations");
            }
            // finish page rank computation and store it to the final list
            nodeWeights.keySet().stream().forEach((nodeIdExt) -> {
                pagerank.put(nodeIdExt, prTemp.get(nodeIdExt));
            });

        } // iterations
        return pagerank;
    }

    public Map<Long, Map<Long, CoOccurrenceItem>> processGraph(String nodeType, String relType, String weightProperties) {
        String query = "MATCH (t1:" + nodeType + ")-[r:" + relType + "]->(t2:" + nodeType + ")\n"
                + "RETURN id(t1) as node1, id(t2) as node2, r as rel, count(*)\n";
        LOG.info("Running query: " + query);
        Map<Long, Map<Long, CoOccurrenceItem>> results = new HashMap<>();
        try (Transaction tx = database.beginTx();) {
            Result res = database.execute(query);
            while (res != null && res.hasNext()) {
                Map<String, Object> next = res.next();
                Long tag1 = (Long) next.get("node1");
                Long tag2 = (Long) next.get("node2");
                double w = 1.; //not used
                Relationship rel = (Relationship) next.get("rel");
                if (weightProperties != null
                        && rel.hasProperty(weightProperties)) {
                    w = (int) rel.getProperty(weightProperties);
                }
                addTagToCoOccurrence(results, tag1, tag2, w);
                if (!directionsMatter)
                    addTagToCoOccurrence(results, tag2, tag1, w);
            }
            tx.success();
        } catch (Exception e) {
            LOG.error("processGraph() failed: " + e.getMessage());
        }
        return results;
    }

    public void storeOnGraph(Map<Long, Double> pageranks, String nodeType) {
        for (Long tag : pageranks.keySet()) {
            try (Transaction tx = database.beginTx();) {
                database.execute("MATCH (t:" + nodeType + ") WHERE id(t)=" + tag + "\n SET t.pagerank = " + pageranks.get(tag));
                tx.success();
            } catch (Exception e) {
                LOG.error("storeOnGraph() failed: " + e.getMessage());
            }
        }
    }

    private void addTagToCoOccurrence(Map<Long, Map<Long, CoOccurrenceItem>> results, Long tag1, Long tag2, double w) {
        Map<Long, CoOccurrenceItem> mapTag1;
        if (!results.containsKey(tag1)) {
            mapTag1 = new HashMap<>();
            results.put(tag1, mapTag1);
        } else {
            mapTag1 = results.get(tag1);
        }
        if (mapTag1.containsKey(tag2)) {
            mapTag1.get(tag2).incCountBy(w);
        } else {
            mapTag1.put(tag2, new CoOccurrenceItem(tag1, tag2));
            mapTag1.get(tag2).setCount(w);
        }
    }

    private Map<Long, Double> initializeNodeWeights(Map<Long, Map<Long, CoOccurrenceItem>> coOccurrences) {
        if (nodeWeights != null && nodeWeights.size() > 0) {
            return nodeWeights;
        }
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
            double diff = Math.abs(prTemp.get(nodeIdExt) - pagerank.get(nodeIdExt));
            if (diff > threshold) {
                return false;
            }
        }
        return true;
    }

}
