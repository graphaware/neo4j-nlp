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
package com.graphaware.nlp.dsl.procedure;

import com.graphaware.common.log.LoggerFactory;
import com.graphaware.nlp.dsl.AbstractDSL;
import com.graphaware.nlp.dsl.request.SummaryRequest;
import com.graphaware.nlp.dsl.request.TextRankPostprocessRequest;
import com.graphaware.nlp.dsl.request.TextRankRequest;
import com.graphaware.nlp.dsl.result.SingleResult;
import com.graphaware.nlp.ml.textrank.TextRankProcessor;
import com.graphaware.nlp.ml.textrank.TextRankResult;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

public class SummarizationProcedure extends AbstractDSL {

    private static final Log LOG = LoggerFactory.getLogger(SummarizationProcedure.class);

    @Procedure(name = "ga.nlp.ml.textRank.summarize", mode = Mode.WRITE)
    @Description("Summarizartion procedure")
    public Stream<SingleResult> summarizeText(@Name("textRankRequest") Map<String, Object> textRankRequest) {
        try {
            SummaryRequest request = SummaryRequest.fromMap(textRankRequest);
            boolean summarized = getNLPManager().summarize(request);
            return Stream.of(summarized ? SingleResult.success() : SingleResult.fail());
        } catch (Exception e) {
            LOG.error("ERROR in TextRankSummarizer", e);
            throw new RuntimeException(e);
        }
    }

    public class KeywordResult {
        public String value;

        public double relevance;

        public KeywordResult(String value, double relevance) {
            this.value = value;
            this.relevance = relevance;
        }
    }
}
