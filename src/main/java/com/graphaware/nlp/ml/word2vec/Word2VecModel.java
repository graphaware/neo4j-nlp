/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.ml.word2vec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ale
 */
public class Word2VecModel {
    private static final Logger LOG = LoggerFactory.getLogger(Word2VecModel.class);

    protected static final String IMPORT_DIRECTORY = "import/";
    protected static final String WORD2VEC_SOURCE_DIRECTORY = IMPORT_DIRECTORY + "word2vecSource/";
    protected static final String WORD2VEC_DEST_DIRECTORY = IMPORT_DIRECTORY + "word2vecIndex/";

    private final Map<String, Word2VecIndexLookup> models;
    private String defaultModel;

    public Word2VecModel() {
        models = new HashMap<>();
    }

    protected final void init() {
        List<String> modelsName = Word2VecIndexCreator.inspectDirectoryAndLoad(WORD2VEC_SOURCE_DIRECTORY, WORD2VEC_DEST_DIRECTORY);
        if (modelsName != null && modelsName.size() > 0) {
            modelsName.forEach((modelName) -> {
                LOG.info("Adding model: " + modelName);
                Word2VecIndexLookup index = new Word2VecIndexLookup(WORD2VEC_DEST_DIRECTORY + modelName);
                models.put(modelName, index);
                if (defaultModel == null) {
                    LOG.info("Setting default model to: " + modelName);
                    defaultModel = modelName;
                }
            });
        }
    }

    public double[] getWordToVec(String lemma, String modelName) {
        if (models.isEmpty()) {
            return null;
        }
        if (modelName == null) {
            return models.get(defaultModel).searchIndex(lemma);
        } else if (models.containsKey(modelName)) {
            return models.get(modelName).searchIndex(lemma);
        }
        return null;
    }

}
