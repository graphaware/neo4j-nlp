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
package com.graphaware.nlp.dsl.procedure;

import com.graphaware.nlp.dsl.AbstractDSL;
import com.graphaware.nlp.dsl.request.PageRankRequest;
import com.graphaware.nlp.dsl.result.SingleResult;
import com.graphaware.nlp.ml.pagerank.PageRankProcessor;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

public class PageRankProcedure extends AbstractDSL {

    @Procedure(name = "ga.nlp.ml.pageRank", mode = Mode.WRITE)
    @Description("PageRank procedure")
    public Stream<SingleResult> computePageRank(@Name("pageRankRequest") Map<String, Object> pageRankRequest) {
        PageRankRequest request = mapper.convertValue(pageRankRequest, PageRankRequest.class);
        PageRankProcessor processor = (PageRankProcessor) getNLPManager().getExtension(PageRankProcessor.class);
        return Stream.of(processor.process(request));
    }
}
