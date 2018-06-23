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
package com.graphaware.nlp.dsl.request;

import org.neo4j.graphdb.Node;
import org.neo4j.logging.Log;
import com.graphaware.common.log.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TextRankPostprocessRequest {

    private static final Log LOG = LoggerFactory.getLogger(TextRankPostprocessRequest.class);

    private final static String PARAMETER_KEYWORD_LABEL = "keywordLabel";
    private final static String PARAMETER_METHOD = "method";
    private final static String PARAMETER_ANNOTATED_TEXT = "annotatedText";

    private String keywordLabel;
    private String method;
    private Node annotatedText;

    private static final String DEFAULT_KEYWORD_LABEL = "Keyword";

    public static TextRankPostprocessRequest fromMap(Map<String, Object> textRankRequest) {
        TextRankPostprocessRequest result = new TextRankPostprocessRequest();
        result.setKeywordLabel((String) textRankRequest.getOrDefault(PARAMETER_KEYWORD_LABEL, DEFAULT_KEYWORD_LABEL));
        if (textRankRequest.containsKey(PARAMETER_METHOD))
            result.setMethod((String) textRankRequest.get(PARAMETER_METHOD));
        else {
            throw new RuntimeException("Missing parameter '" + PARAMETER_METHOD + "', aborting.");
        }

        if (textRankRequest.containsKey(PARAMETER_ANNOTATED_TEXT)) {
            result.setAnnotatedText((Node) textRankRequest.get(PARAMETER_ANNOTATED_TEXT));
        }

        return result;
    }


    public String getKeywordLabel() {
        return keywordLabel;
    }

    public void setKeywordLabel(String label) {
        this.keywordLabel = label;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Node getAnnotatedText() {
        return annotatedText;
    }

    public void setAnnotatedText(Node annotatedText) {
        this.annotatedText = annotatedText;
    }
}
