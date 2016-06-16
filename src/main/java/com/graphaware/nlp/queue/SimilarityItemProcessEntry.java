package com.graphaware.nlp.queue;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.util.ArrayList;

/**
 *
 * @author alessandro@graphaware.com
 */
public class SimilarityItemProcessEntry {
    private final ArrayList<SimilarityItem> kNN;
    private final long node;

    public SimilarityItemProcessEntry(long node, ArrayList<SimilarityItem> kNN) {
        this.kNN = kNN;
        this.node = node;
    }

    public ArrayList<SimilarityItem> getkNN() {
        return kNN;
    }

    public long getNodeId() {
        return node;
    }
}
