/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.domain;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author ale
 */
public class Sentence {
    private final Map<String, Tag> tags;

    public Sentence() {
        tags = new HashMap<>();
    }

    public Collection<Tag> getTags() {
        return tags.values();
    }
    
    public void addTag(Tag tag) {
        if (tags.containsKey(tag.getLemma()))
            tags.get(tag.getLemma()).incMultiplicity();
        else 
            tags.put(tag.getLemma(), tag);
    }
}
