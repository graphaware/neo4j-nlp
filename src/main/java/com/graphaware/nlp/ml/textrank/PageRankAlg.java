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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import static com.graphaware.nlp.domain.Constants.KNN_SIZE;
import com.graphaware.nlp.domain.Labels;
import com.graphaware.nlp.domain.Relationships;
import com.graphaware.nlp.ml.queue.SimilarityItemProcessEntry;
import com.graphaware.nlp.ml.queue.SimilarityItem;
import com.graphaware.nlp.ml.queue.SimilarityQueueProcessor;
import com.graphaware.nlp.util.FixedSizeOrderedList;
import com.graphaware.nlp.ml.similarity.CosineSimilarity;
import com.graphaware.nlp.ml.similarity.Similarity;
import com.graphaware.nlp.domain.Tag;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PageRankAlg {

    private static final Logger LOG = LoggerFactory.getLogger(PageRankAlg.class);

    protected final GraphDatabaseService database;
    private Map<Node, Double> nodeWeights;
    private Map<Integer, List<Double>> adjMatrix;
    private List<Double> nodes_rels_sumW;
    private List<Double> pagerank;

    private Map<Node, Integer> nodes_nodeToIdx;
    private Map<Integer, Node> nodes_idxToNode;

    public PageRankAlg(GraphDatabaseService database) {
        this.database = database;
    }

    public Set<Object[]> run(Long id, /*String nodeType,*/ String relType, String relWeight, int iter, double damp) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        //params.put("nodeType", nodeType);
        params.put("relType", relType);
        params.put("relWeight", relWeight);

        // get node weights (tf-idf)
        initializeNodeWeights(params);

        // get adjacancy matrix
        initializeAdjacencyMatrix(params);

        // run it
        final int nNodes = nodeWeights.size();
        initializePRValues(nNodes, damp);
        double max_node_w = nodeWeights.entrySet().stream().max(Map.Entry.comparingByValue(Double::compareTo)).get().getValue();
        double sum_node_w = nodeWeights.entrySet().stream().mapToDouble(e -> e.getValue()).sum();
        double[] prTemp = new double[nNodes];
        for (int it=0; it<iter; it++) {
            for (int ii=0; ii<nNodes; ii++)
                prTemp[ii] = 0.;

            // calculate main part of the PR calculation, include weights of nodes and relationships
            for (int i=0; i<nNodes; i++) {
                double sum_locnodes_w = 0.;
                double max_locnodes_w = 0.;
                int n_locNodes = 0;
                for (int j=0; j<nNodes; j++) {
                    if (nodes_rels_sumW.get(j)!=0) {
                        //prTemp[i] += pagerank.get(j) * adjMatrix.get(i).get(j) / nodes_rels_sumW.get(j);
                        prTemp[i] += nodeWeights.get(nodes_idxToNode.get(j)) * pagerank.get(j) * adjMatrix.get(i).get(j) / nodes_rels_sumW.get(j);
                        if (adjMatrix.get(i).get(j)!=0) {
                            sum_locnodes_w += nodeWeights.get(nodes_idxToNode.get(j));
                            n_locNodes += 1;
                        }
                        if (adjMatrix.get(i).get(j)!=0 && max_locnodes_w < nodeWeights.get(nodes_idxToNode.get(j)))
                            max_locnodes_w = nodeWeights.get(nodes_idxToNode.get(j));
                    }
                }
                prTemp[i] /= sum_locnodes_w!=0 ? sum_locnodes_w/n_locNodes : 1;
                //prTemp[i] /= max_locnodes_w!=0 ? max_locnodes_w : 1;
                //prTemp[i] /= sum_node_w/nNodes;
                ////prTemp[i] /= max_node_w;
            }

            // finish page rank computation and store it to the final list
            for (int i=0; i<nNodes; i++) {
                prTemp[i] = (1-damp)/nNodes + damp * prTemp[i];
                //prTemp[i] = (1-damp) + damp * prTemp[i];
                //prTemp[i] = (1-damp) * nodeWeights.get(nodes_idxToNode.get(i))/sum_node_w + damp * prTemp[i];
                pagerank.set(i, prTemp[i]);
            }

        } // iterations

        LOG.debug("PageRank results:");
        Set<Object[]> result = new HashSet<>();
        for (int i=0; i<nNodes; i++) {
            LOG.debug(nodes_idxToNode.get(i).getProperty("id") + ": " + pagerank.get(i));
            result.add(new Object[]{nodes_idxToNode.get(i), pagerank.get(i), nodeWeights.get(nodes_idxToNode.get(i))});
        }
        LOG.debug("Max node-weight (tf-idf) value: " + max_node_w);
        return result;
    }

    private void initializeNodeWeights(Map params) {
        String query = "MATCH (doc:AnnotatedText)\n"
            + "WITH count(doc) as documentsCount\n"
            + "MATCH (a:AnnotatedText)-[:CONTAINS_SENTENCE]->(:Sentence)-[ht:HAS_TAG]->(t:Tag)\n"
            + "WHERE a.id = {id} and EXISTS((t)-[:" + params.get("relType") + "]-(:Tag))\n" // we're interested in this subset of tags only
            + "WITH t, sum(ht.tf) as tf, documentsCount\n"
            + "MATCH (a:AnnotatedText)-[:CONTAINS_SENTENCE]->(:Sentence)-[:HAS_TAG]->(t)\n"
            + "RETURN t as tag, tf, count(distinct a) as docCountForTag, documentsCount\n";

        nodeWeights = new HashMap<Node, Double>();
        Result res;
        try (Transaction tx = database.beginTx();) {
            res = database.execute(query, params);
            tx.success();
        } catch (Exception e) {
            LOG.error("Error while initializing node weights: ", e);
            return;
        }
        while (res != null && res.hasNext()) {
            Map<String, Object> next = res.next();
            Node tag = (Node) next.get("tag");
            long tf = (Long) next.get("tf"); //getDoubleValue(next.get("tf"));

            long docCount = (long) next.get("documentsCount");
            long docCountTag = (long) next.get("docCountForTag");
            double idf = Double.valueOf(Math.log10(Double.valueOf(1.0f*docCount/docCountTag).doubleValue())).floatValue();

            nodeWeights.put(tag, tf*idf);
            LOG.debug("Tag = " + tag.getProperty("value") + ": tf = " + tf + ", idf = " + idf + " (docCountTag = " + docCountTag + "), tf*idf = " + tf*idf);
        }
    }

    private void initializeAdjacencyMatrix(Map params) {
        String query = "MATCH (a:AnnotatedText)-[:CONTAINS_SENTENCE]->(s:Sentence)-[:HAS_TAG]->(t:Tag)-[c:" + params.get("relType") + "]->(t2:Tag)\n"
            + "WHERE a.id = {id} and EXISTS((t2)<-[:HAS_TAG]-(:Sentence)<-[:CONTAINS_SENTENCE]-(a))\n"
            + "WITH distinct c as c\n"
            + "MATCH (t:Tag)-[c]->(t2:Tag)\n"
            + "RETURN t as Tag1, t2 as Tag2, (case c." + params.get("relWeight") + " when null then 1.0f else c." + params.get("relWeight") + " end) as wr\n";
            //+ "ORDER BY Tag1\n";

        final int nNodes = nodeWeights.size();
        indexNodes(nodeWeights);

        // initialize adjacancy matrix
        adjMatrix = new HashMap<Integer, List<Double>>();
        for (int i=0; i<nNodes; i++) {
            List pom = new LinkedList<Double>();
            for (int j=0; j<nNodes; j++)
                pom.add(j, 0.);
            adjMatrix.put(i, pom);
        }

        // run query and process results
        Result res;
        try (Transaction tx = database.beginTx();) {
            res = database.execute(query, params);
            tx.success();
        } catch (Exception e) {
            LOG.error("Error while initializing adjacency matrix: ", e);
            return;
        }
        while (res != null && res.hasNext()) {
            Map<String, Object> next = res.next();
            Node tag1 = (Node) next.get("Tag1");
            Node tag2 = (Node) next.get("Tag2");
            double wr = Double.valueOf((Long) next.get("wr")); //getDoubleValue(next.get("wr"));

            // when relationship direction is not important:
            if (!nodes_nodeToIdx.containsKey(tag1)) {
                LOG.error("Tag node " + tag1.getProperty("id") + " was not indexed (nNodes_indexed = " + nNodes + "). Check node-weights query.");
                continue;
            }
            if (!nodes_nodeToIdx.containsKey(tag2)) {
                LOG.error("Tag node " + tag2.getProperty("id") + " was not indexed (nNodes_indexed = " + nNodes + "). Check node-weights query.");
                continue;
            }
            adjMatrix.get(nodes_nodeToIdx.get(tag1)).set(nodes_nodeToIdx.get(tag2), wr);
            adjMatrix.get(nodes_nodeToIdx.get(tag2)).set(nodes_nodeToIdx.get(tag1), wr);
        }

        // calculate sum of relationship weights for each node
        nodes_rels_sumW = new LinkedList<Double>();
        for (int i=0; i<nNodes; i++)
            nodes_rels_sumW.add(i, adjMatrix.get(i).stream().mapToDouble(Double::doubleValue).sum());
    }

    private void indexNodes(Map<Node, Double> map) {
        nodes_nodeToIdx = new HashMap<Node, Integer>();
        nodes_idxToNode = new HashMap<Integer, Node>();
        int i = 0;
        for (Node key : map.keySet()) {
            nodes_nodeToIdx.put(key, i);
            nodes_idxToNode.put(i, key);
            i++;
        }
    }

    private void initializePRValues(int n, double damp) {
        pagerank = new LinkedList<Double>();
        for (int i=0; i<n; i++)
            pagerank.add((1.-damp)/n);
    }

}
