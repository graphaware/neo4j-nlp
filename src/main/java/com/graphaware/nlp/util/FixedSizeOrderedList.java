/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.util;

import java.util.ArrayList;
import java.util.Iterator;

public class FixedSizeOrderedList<T extends Comparable> extends ArrayList<T> {
    
    private final int maxSize;

    public FixedSizeOrderedList(int maxSize) {
        super();
        this.maxSize = maxSize;
    }
    
    @Override
    public boolean add(T value) {
        int index = 0;
        Iterator<T> iter = this.iterator();
        while (iter.hasNext() 
                && iter.next().compareTo(value) > 0) {
            index++;
            if (index > maxSize) {
                return false;
            }
        }
        super.add(index, value);
        if (this.size() > maxSize) {
            this.remove(maxSize);
        }
        return true;
    }
}
