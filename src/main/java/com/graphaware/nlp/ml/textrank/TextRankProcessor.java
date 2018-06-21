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
package com.graphaware.nlp.ml.textrank;

import com.graphaware.nlp.annotation.NLPModuleExtension;
import com.graphaware.nlp.dsl.request.TextRankPostprocessRequest;
import com.graphaware.nlp.dsl.request.TextRankRequest;
import com.graphaware.nlp.dsl.result.SingleResult;
import com.graphaware.nlp.extension.AbstractExtension;
import com.graphaware.nlp.extension.NLPExtension;
import org.neo4j.logging.Log;
import com.graphaware.common.log.LoggerFactory;

@NLPModuleExtension(name = "TextRankProcessor")
public class TextRankProcessor extends AbstractExtension implements NLPExtension {

    private static final Log LOG = LoggerFactory.getLogger(TextRankProcessor.class);

    public SingleResult process(TextRankRequest request) {
        TextRank.Builder textrankBuilder = new TextRank.Builder(getDatabase(), getNLPManager().getConfiguration());
        if (request.getStopWords() != null 
                && !request.getStopWords().isEmpty()) {
            textrankBuilder.setStopwords(request.getStopWords());
        }
        textrankBuilder.removeStopWords(request.isDoStopwords())
                .respectDirections(request.isRespectDirections())
                .respectSentences(request.isRespectSentences())
                .useDependencies(request.isUseDependencies())
                .useDependenciesForCooccurrences(request.isUseDependenciesForCooccurrences())
                //.setCooccurrenceWindow(request.getCooccurrenceWindow())
                .setTopXTags(request.getTopXTags())
                .setCleanKeywords(request.isCleanKeywords())
                .setKeywordLabel(request.getKeywordLabel());
        
        TextRank textRank = textrankBuilder.build();
        boolean res = textRank.evaluate(request.getNode(), 
                request.getIterations(), 
                request.getDamp(), 
                request.getThreshold());
        LOG.info("AnnotatedText with ID " + request.getNode().getId() + " processed. Result: " + res);
        return res ? SingleResult.success() : SingleResult.fail();
    }

    public SingleResult postprocess(TextRankPostprocessRequest request) {
        LOG.info("Starting TextRank post-processing ...");
        TextRank.Builder textrankBuilder = new TextRank.Builder(getDatabase(), getNLPManager().getConfiguration());
        textrankBuilder.setKeywordLabel(request.getKeywordLabel());
        if (!textrankBuilder.build().postprocess(request.getMethod(), request.getAnnotatedText()))
            return SingleResult.fail();
        LOG.info("TextRank post-processing completed.");
        return SingleResult.success();
    }

    public SingleResult summarize(TextRankRequest request) {
        TextRankSummarizer.Builder summarizerBuilder = new TextRankSummarizer.Builder(getDatabase(), getNLPManager().getConfiguration());
        //summarizerBuilder.setKeywordLabel(request.getKeywordLabel());

        TextRankSummarizer trSummarizer = summarizerBuilder.build();
        boolean res = trSummarizer.evaluate(request.getNode(),
                request.getIterations(),
                request.getDamp(),
                request.getThreshold());
        LOG.info("AnnotatedText with ID " + request.getNode().getId() + " processed. Result: " + res);

        return res ? SingleResult.success() : SingleResult.fail();
    }
}
