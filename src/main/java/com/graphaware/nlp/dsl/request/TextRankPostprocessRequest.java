/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.dsl.request;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ale
 */
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
