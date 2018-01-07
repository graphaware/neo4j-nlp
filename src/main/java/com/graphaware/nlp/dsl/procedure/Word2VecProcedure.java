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
import com.graphaware.nlp.dsl.request.Word2VecRequest;
import com.graphaware.nlp.dsl.result.SingleResult;
import com.graphaware.nlp.ml.word2vec.Word2VecProcessor;
import org.neo4j.graphdb.Node;
import org.neo4j.procedure.*;

import java.util.Map;
import java.util.stream.Stream;

public class Word2VecProcedure extends AbstractDSL {

    @Procedure(name = "ga.nlp.ml.word2vec.attach", mode = Mode.WRITE)
    @Description("For each tag attach the related word2vec value")
    public Stream<SingleResult> attachConcepts(@Name("input") Map<String, Object> word2VecRequest) {
        Word2VecRequest request = Word2VecRequest.fromMap(word2VecRequest);
        Word2VecProcessor word2VecProcessor = (Word2VecProcessor) getNLPManager().getExtension(Word2VecProcessor.class);
        int processed = word2VecProcessor.attach(request);
        return Stream.of(new SingleResult(processed));
    }
//
//    @UserFunction(name = "ga.nlp.ml.word2vec.getVector")
//    @Description("Retrieve the embedding vector for the given Tag node")
//    public SingleResult retrieveVector(@Name("tag") Node tag) {
//        Word2VecProcessor word2VecProcessor = (Word2VecProcessor) getNLPManager().getExtension(Word2VecProcessor.class);
//        return new SingleResult(word2VecProcessor.getWord2Vec(tag.getProperty("value").toString(), null));
//    }
}
