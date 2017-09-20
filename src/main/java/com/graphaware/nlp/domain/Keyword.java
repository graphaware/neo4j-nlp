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

public class Keyword {
    private final String keyword;
    private final String keywordNoLangInfo;
    private final String[] keywordsArray;
    private final int wordsCount;
    private int exactMatch;
    private int total;
    private double relevance;

    public Keyword(String word) {
        this.keyword = word;
        this.keywordNoLangInfo = word.split("_")[0];
        this.keywordsArray = keywordNoLangInfo.split(" ");
        this.wordsCount = keywordsArray.length;
        this.exactMatch = 1;
        this.total = 1;
    }
    
    public String getKeyword() {
        return this.keyword;
    }

    public String getRawKeyword() {
        return this.keywordNoLangInfo;
    }

    public String[] getListOfWords() {
        return this.keywordsArray;
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
