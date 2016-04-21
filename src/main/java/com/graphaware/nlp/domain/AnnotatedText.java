/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.domain;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ale
 */
public class AnnotatedText {
    private final List<Sentence> sentences;

    public AnnotatedText() {
        sentences = new ArrayList<>();
    }

    public List<Sentence> getSentences() {
        return sentences;
    }
    
    public void addSentence(Sentence sentence) {
        sentences.add(sentence);
    }
}
