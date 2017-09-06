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
import com.graphaware.nlp.dsl.result.SingleResult;
import java.util.Map;

import com.graphaware.nlp.extension.AbstractExtension;
import com.graphaware.nlp.extension.NLPExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NLPModuleExtension(name = "TextRankProcessor")
public class TextRankProcessor extends AbstractExtension implements NLPExtension {

    private static final Logger LOG = LoggerFactory.getLogger(TextRankProcessor.class);

    public SingleResult process(TextRankRequest request) {

        TextRank textrank = new TextRank(getDatabase(), getNLPManager().getConfiguration());

        if (request.getStopWords() != null 
                && !request.getStopWords().isEmpty()) {
            textrank.setStopwords(request.getStopWords());
        }
        textrank.removeStopWords(request.isDoStopwords());
        textrank.respectDirections(request.isRespectDirections());
        textrank.respectSentences(request.isRespectSentences());
        textrank.useTfIdfWeights(request.isUseTfIdfWeights());
        textrank.useDependencies(request.isUseDependencies());
        textrank.setCooccurrenceWindow(request.getCooccurrenceWindow());
        textrank.setMaxSingleKeywords(request.getMaxSingleKeywords());
        textrank.setKeywordLabel(request.getKeywordLabel());

        Map<Long, Map<Long, CoOccurrenceItem>> coOccurrence = textrank.createCooccurrences(request.getNode());
        boolean res = textrank.evaluate(request.getNode(), 
                coOccurrence, 
                request.getIterations(), 
                request.getDamp(), 
                request.getThreshold());

        if (!res) {
            return SingleResult.fail();
        }

        LOG.info("AnnotatedText with ID " + request.getNode().getId() + " processed.");

        return SingleResult.success();
    }

    public SingleResult postprocess(TextRankRequest request) {
        LOG.info("Starting TextRank post-processing ...");
        TextRank textrank = new TextRank(getDatabase(), getNLPManager().getConfiguration());
        textrank.setKeywordLabel(request.getKeywordLabel());
        if (!textrank.postprocess())
            return SingleResult.fail();
        LOG.info("TextRank post-processing completed.");
        return SingleResult.success();
    }
}
