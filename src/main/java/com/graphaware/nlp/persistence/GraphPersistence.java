/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.persistence;

import com.graphaware.nlp.domain.AnnotatedText;

/**
 *
 * @author ale
 */
public interface GraphPersistence {
    public void persistOnGraph(AnnotatedText text);
}
