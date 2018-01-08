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
import com.graphaware.nlp.dsl.result.Word2VecModelResult;
import com.graphaware.nlp.ml.word2vec.Word2VecIndexLookup;
import com.graphaware.nlp.ml.word2vec.Word2VecProcessor;
import org.apache.commons.lang.ArrayUtils;
import org.neo4j.graphdb.Node;
import org.neo4j.procedure.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    @Procedure(name = "ga.nlp.ml.word2vec.listModels", mode = Mode.WRITE)
    public Stream<Word2VecModelResult> listModels() {
        Word2VecProcessor word2VecProcessor = (Word2VecProcessor) getNLPManager().getExtension(Word2VecProcessor.class);
        Map<String, Word2VecIndexLookup> models = word2VecProcessor.getWord2VecModel().getModels();
        List<Word2VecModelResult> results = new ArrayList<>();
        models.keySet().forEach(s -> {
            try {
                results.add(new Word2VecModelResult(s, models.get(s).getStorePath(), models.get(s).countIndex()));
            } catch (IOException e) {
                //
            }
        });

        return results.stream();
    }

    @Procedure(name = "ga.nlp.ml.word2vec.addModel", mode = Mode.WRITE)
    public Stream<SingleResult> addModel(@Name("sourePath") String sourcePath, @Name("destinationPath") String destinationPath, @Name("modelName") String modelName) {
        Word2VecProcessor word2VecProcessor = (Word2VecProcessor) getNLPManager().getExtension(Word2VecProcessor.class);
        word2VecProcessor.getWord2VecModel().createModelFromPaths(sourcePath, destinationPath, modelName);

        return Stream.of(SingleResult.success());
    }

    @UserFunction(name = "ga.nlp.ml.word2vec.vector")
    @Description("Retrieve the embedding vector for the given Tag node")
    public List<Double> retrieveVector(@Name("tag") Node tag, @Name(value = "modelName", defaultValue = "") String modelName) {
        Word2VecProcessor word2VecProcessor = (Word2VecProcessor) getNLPManager().getExtension(Word2VecProcessor.class);
        Double[] doubleArray = ArrayUtils.toObject(word2VecProcessor.getWord2Vec(tag.getProperty("value").toString(), modelName));
        return Arrays.asList(doubleArray);
    }

    @UserFunction(name = "ga.nlp.ml.word2vec.wordVector")
    @Description("Retrieve the embedding vector for the given word ")
    public List<Double> retrieveVectorForWord(@Name("word") String word, @Name(value = "modelName", defaultValue = "") String modelName) {
        Word2VecProcessor word2VecProcessor = (Word2VecProcessor) getNLPManager().getExtension(Word2VecProcessor.class);
        double[] vector = word2VecProcessor.getWord2Vec(word, modelName);
        if (vector == null) {
            return null;
        }
        
        Double[] doubleArray = ArrayUtils.toObject(vector);
        return Arrays.asList(doubleArray);
    }
}
