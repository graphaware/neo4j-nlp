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
package com.graphaware.nlp.dsl.request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TextRankPostprocessRequest {

    private static final Logger LOG = LoggerFactory.getLogger(TextRankPostprocessRequest.class);

    private final static String PARAMETER_KEYWORD_LABEL = "keywordLabel";

    private String keywordLabel;

    private static final String DEFAULT_KEYWORD_LABEL = "Keyword";

    public static TextRankPostprocessRequest fromMap(Map<String, Object> textRankRequest) {
        TextRankPostprocessRequest result = new TextRankPostprocessRequest();
        result.setKeywordLabel((String) textRankRequest.getOrDefault(PARAMETER_KEYWORD_LABEL, DEFAULT_KEYWORD_LABEL));

        return result;
    }


    public String getKeywordLabel() {
        return keywordLabel;
    }

    public void setKeywordLabel(String label) {
        this.keywordLabel = label;
    }
}
