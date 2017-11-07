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
package com.graphaware.nlp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AnnotatedText {

    private static final Logger LOG = LoggerFactory.getLogger(AnnotatedText.class);
    private static final long serialVersionUID = -1L;

    private String text;

    private int numTerms;

    private List<Sentence> sentences = new ArrayList<>();

    public List<Sentence> getSentences() {
        return sentences;
    }

    public void addSentence(Sentence sentence) {
        sentences.add(sentence);
    }

    public List<String> getTokens() {
        List<String> result = new ArrayList<>();
        sentences.forEach((sentence) -> {
            sentence.getTags().forEach((tag) -> {
                result.add(tag.getLemma());
            });
        });
        return result;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getNumTerms() {
        return numTerms;
    }

    public void setNumTerms(int numTerms) {
        this.numTerms = numTerms;
    }

    public List<Tag> getTags() {
        List<Tag> result = new ArrayList<>();
        sentences.forEach((sentence) -> {
            sentence.getTags().forEach((tag) -> {
                result.add(tag);
            });
        });
        return result;
    }

    public boolean filter(String filterQuery) {
        Map<String, FilterQueryTerm> filterQueryTerms = getFilterQueryTerms(filterQuery);
        List<Tag> tags = getTags();
        for (Tag tag : tags) {
            FilterQueryTerm query = filterQueryTerms.get(tag.getLemma().toLowerCase());
            if (query != null && query.evaluate(tag)) {
                return true;
            }
        }

        return false;
    }

    //Query example "Nice/Location, attack"
    private Map<String, FilterQueryTerm> getFilterQueryTerms(String query) {
        Map<String, FilterQueryTerm> result = new HashMap<>();
        if (query != null) {
            String[] terms = query.split(",");
            for (String term : terms) {
                FilterQueryTerm filterQueryTerm = new FilterQueryTerm(term);
                result.put(filterQueryTerm.getValue().toLowerCase(), filterQueryTerm);
            }
        }

        return result;
    }

    public List<Sentence> getSentencesSorted() {
        sentences.sort((Sentence o1, Sentence o2) -> o1.compareTo(o2));

        return sentences;
    }

    private class FilterQueryTerm {

        private final String value;
        private final String NE;

        FilterQueryTerm(String query) {
            String[] parts = query.split("/");
            this.value = parts[0];
            this.NE = parts.length > 1 ? parts[1] : null;

        }

        public String getValue() {
            return value;
        }

        private boolean evaluate(Tag tag) {
            if (NE != null) {
                return tag.getNeAsList().contains(NE) && tag.getLemma().equalsIgnoreCase(value);
            } else {
                return tag.getLemma().equalsIgnoreCase(value);
            }
        }

    }
}
