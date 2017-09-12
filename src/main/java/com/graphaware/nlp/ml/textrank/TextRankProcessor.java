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

import com.graphaware.nlp.annotation.NLPModuleExtension;
import com.graphaware.nlp.dsl.TextRankRequest;
import com.graphaware.nlp.dsl.TextRankPostprocessRequest;
import com.graphaware.nlp.dsl.result.SingleResult;

import com.graphaware.nlp.extension.AbstractExtension;
import com.graphaware.nlp.extension.NLPExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NLPModuleExtension(name = "TextRankProcessor")
public class TextRankProcessor extends AbstractExtension implements NLPExtension {

    private static final Logger LOG = LoggerFactory.getLogger(TextRankProcessor.class);

    public SingleResult process(TextRankRequest request) {

        TextRank.Builder textrankBuilder = new TextRank.Builder(getDatabase(), getNLPManager().getConfiguration());
        if (request.getStopWords() != null 
                && !request.getStopWords().isEmpty()) {
            textrankBuilder.setStopwords(request.getStopWords());
        }
        textrankBuilder.removeStopWords(request.isDoStopwords())
                .respectDirections(request.isRespectDirections())
                .respectSentences(request.isRespectSentences())
                .useTfIdfWeights(request.isUseTfIdfWeights())
                .useDependencies(request.isUseDependencies())
                .setCooccurrenceWindow(request.getCooccurrenceWindow())
                .setMaxSingleKeywords(request.getMaxSingleKeywords())
                .setTopXWordsForPhrases(request.getTopXWordsForPhrases())
                .setTopXSinglewordKeywords(request.getTopXSinglewordKeywords())
                .setKeywordLabel(request.getKeywordLabel());
        
        TextRank textRank = textrankBuilder.build();
        boolean res = textRank.evaluate(request.getNode(), 
                request.getIterations(), 
                request.getDamp(), 
                request.getThreshold());
        LOG.info("AnnotatedText with ID " + request.getNode().getId() + " processed. Res: " + res);
        return res ? SingleResult.success() : SingleResult.fail();
    }

    public SingleResult postprocess(TextRankPostprocessRequest request) {
        LOG.info("Starting TextRank post-processing ...");
        TextRank.Builder textrankBuilder = new TextRank.Builder(getDatabase(), getNLPManager().getConfiguration());
        textrankBuilder.setKeywordLabel(request.getKeywordLabel());
        if (!textrankBuilder.build().postprocess())
            return SingleResult.fail();
        LOG.info("TextRank post-processing completed.");
        return SingleResult.success();
    }
}
