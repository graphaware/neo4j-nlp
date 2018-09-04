/*
 * Copyright (c) 2013-2018 GraphAware
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

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Keyword {
    private String keyword;
    private String keywordNoLangInfo;
    private String[] listOfWords;
    private int wordsCount;
    private String keywordOriginal;
    private int exactMatch;
    private int total;
    private double relevance;
    private int nTopRated;
    private TfIdfObject tfIdf;

    public Keyword() {
    }

    public Keyword(String word) {
        this.keyword = word;
        this.keywordNoLangInfo = word.split("_")[0];
        this.keywordOriginal = word;
        this.listOfWords = keywordNoLangInfo.split(" ");
        this.wordsCount = listOfWords.length;
        this.exactMatch = 1;
        this.total = 1;
        this.nTopRated = 0;
        this.tfIdf = new TfIdfObject(0., 0.);
    }

    public Keyword(String word, int occurrences) {
        this.keyword = word;
        this.keywordNoLangInfo = word.split("_")[0];
        this.keywordOriginal = word;
        this.listOfWords = keywordNoLangInfo.split(" ");
        this.wordsCount = listOfWords.length;
        this.exactMatch = occurrences;
        this.total = occurrences;
        this.nTopRated = 0;
        this.tfIdf = new TfIdfObject(0., 0.);
    }
    
    public String getKeyword() {
        return this.keyword;
    }

    public String getRawKeyword() {
        return this.keywordNoLangInfo;
    }

    public String getKeywordNoLangInfo() {
        return keywordNoLangInfo;
    }

    public String getOriginalRawKeyword() {
        return this.keywordOriginal.split("_")[0];
    }

    public String getOriginalTagId() {
        return this.keywordOriginal;
    }

    public void setOriginalTagId(String val) {
        this.keywordOriginal = val;
    }

    public String[] getListOfWords() {
        return this.listOfWords;
    }

    public int getWordsCount() {
        return this.wordsCount;
    }

    public int getExactMatchCount() {
        return this.exactMatch;
    }

    public void incExactMatchCount() {
        this.exactMatch++;
    }

    public void incExactMatchCountBy(int val) {
        this.exactMatch += val;
    }

    public int getTotalCount() {
        return this.total;
    }

    public void incTotalCount() {
        this.total++;
    }

    public void incTotalCountBy(int val) {
        this.total += val;
    }

    public void incCounts() {
        this.exactMatch++;
        this.total++;
    }

    public void incCountsBy(int val) {
        this.exactMatch += val;
        this.total += val;
    }

    public boolean contains(Keyword ki) {
        if (ki == null)
            return false;
        return keywordNoLangInfo.contains(ki.getRawKeyword());
    }

    public double getRelevance() {
        return relevance;
    }

    public void setRelevance(double relevance) {
        this.relevance = relevance;
    }

    @JsonIgnore
    public double getMeanRelevance() {
        return this.relevance/this.wordsCount;
    }

    public TfIdfObject getTfIdf() {
        return tfIdf;
    }

    public void setTfIdf(TfIdfObject tfIdf) {
        this.tfIdf = tfIdf;
    }

    public double getTf() {
        return this.tfIdf.getTf();
    }

    public void setTf(double val) {
        this.tfIdf.setTf(val);
    }

    @JsonIgnore
    public double getIdf() {
        return this.tfIdf.getIdf();
    }

    public void setIdf(double val) {
        this.tfIdf.setIdf(val);
    }

    public int getNTopRated() {
        return this.nTopRated;
    }

    public void setNTopRated(int val) {
        this.nTopRated = val;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!Keyword.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        final Keyword other = (Keyword) obj;
        if ( !this.keyword.equals(other.getKeyword()) )
            return false;
        return true;
    }
}
