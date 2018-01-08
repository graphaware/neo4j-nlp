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
package com.graphaware.nlp.ml.word2vec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public void createModelFromPaths(String sourcePath, String destPath, String modelName) {
        List<String> modelsName = Word2VecIndexCreator.inspectDirectoryAndLoad(sourcePath, destPath);
        if (modelsName != null) {
            modelsName.forEach(model -> {
                Word2VecIndexLookup index = new Word2VecIndexLookup(destPath + model);
                models.put(modelName, index);
                if (defaultModel == null) {
                    defaultModel = modelName;
                }
            });
        }
    }

    public Map<String, Word2VecIndexLookup> getModels() {
        return models;
    }

    public double[] getWordToVec(String lemma, String modelName) {
        if (models.isEmpty()) {
            return null;
        }
        if (modelName == null || modelName.equals("")) {
            return models.get(defaultModel).searchIndex(lemma);
        } else if (models.containsKey(modelName)) {
            return models.get(modelName).searchIndex(lemma);
        }
        return null;
    }

}
