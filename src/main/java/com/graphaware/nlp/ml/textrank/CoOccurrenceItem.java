/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.ml.textrank;

/**
 *
 * @author ale
 */
public class CoOccurrenceItem {
    private final long source;
    private final long destination;
    private int count;

    public CoOccurrenceItem(long source, long destination) {
        this.source = source;
        this.destination = destination;
        this.count = 1;
    }
    
    public long getSource() {
        return source;
    }

    public long getDestination() {
        return destination;
    }

    public int getCount() {
        return count;
    }
    
    public void incCount() {
        this.count++;
    }
}
