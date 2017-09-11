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
package com.graphaware.nlp.ml.textrank;

/**
 *
 * @author vla
 */
public class KeywordItem {
    private final String keyword;
    private final String keyword_noLangInfo;
    private final String[] keywords_arr;
    private final int n_words;
    private int count_exactMatch;
    private int count_total;

    public KeywordItem(String word) {
        this.keyword = word;
        this.keyword_noLangInfo = word.split("_")[0];
        this.keywords_arr = keyword_noLangInfo.split(" ");
        this.n_words = keywords_arr.length;
        this.count_exactMatch = 1;
        this.count_total = 1;
    }
    
    public String getKeyword() {
        return this.keyword;
    }

    public String getRawKeyword() {
        return this.keyword_noLangInfo;
    }

    public String[] getListOfWords() {
        return this.keywords_arr;
    }

    public int getNWords() {
        return this.n_words;
    }

    public int getExactMatchCount() {
        return this.count_exactMatch;
    }

    public void incExactMatchCount() {
        this.count_exactMatch++;
    }

    public void incExactMatchCountBy(int val) {
        this.count_exactMatch += val;
    }

    public int getTotalCount() {
        return this.count_total;
    }

    public void incTotalCount() {
        this.count_total++;
    }

    public void incTotalCountBy(int val) {
        this.count_total += val;
    }

    public void incCounts() {
        this.count_exactMatch++;
        this.count_total++;
    }

    public void incCountsBy(int val) {
        this.count_exactMatch += val;
        this.count_total += val;
    }

    public boolean contains(KeywordItem ki) {
        if (ki == null)
            return false;
        return keyword_noLangInfo.contains(ki.getRawKeyword());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!KeywordItem.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        final KeywordItem other = (KeywordItem) obj;
        if ( !this.keyword.equals(other.getKeyword()) )
            return false;
        return true;
    }
}
