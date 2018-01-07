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

import com.graphaware.nlp.util.TypeConverter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Word2VecIndexCreator {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(Word2VecIndexLookup.class);

    public static final String VECTOR_FIELD = "values";
    public static final String WORD_FIELD = "word";

    public static boolean loadFromFile(String sourceFile, String indexPath, boolean create) {
        try {
            if (!create) {
                File f = new File(indexPath);
                if (f.exists()) {
                    return true;
                }
            }
            Directory dir = FSDirectory.open(Paths.get(indexPath));
            Analyzer analyzer = new KeywordAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            if (create) {
                iwc.setOpenMode(OpenMode.CREATE);
            } else {
                iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
            }
            iwc.setRAMBufferSizeMB(256.0);
            try (IndexWriter writer = new IndexWriter(dir, iwc)) {
                indexWord2Vec(writer, sourceFile);
                writer.forceMerge(1);
            }
            
        }   catch (IOException ex) {
            LOG.error("Error loading Word2Vec: " + sourceFile, ex);
            return false;
        }
        return true;
    }

    private static void indexWord2Vec(IndexWriter writer, String sourceFile) throws IOException {

        LineIterator it = FileUtils.lineIterator(new File(sourceFile), "UTF-8");
        try {
            while (it.hasNext()) {
                String line = it.nextLine();
                String[] split = line.split(" ");
                if (split != null && split.length > 2) {
                    Document doc = new Document();
                    String word = split[0];
                    String wordToUse = split[0];
                    if (word.startsWith("/c/") && !word.startsWith("/c/en/")) {
                        continue;
                    }
                    if (word.startsWith("/c/en")) {
                        wordToUse = wordToUse.replace("/c/en/", "").trim();
                    }
                    doc.add(new StringField(WORD_FIELD, wordToUse, Field.Store.YES));
                    double[] vector = new double[split.length - 1];
                    for (int i = 0; i < split.length - 1; i++) {
                        vector[i] = Double.parseDouble(split[i + 1]);
                    }
                    doc.add(new StoredField(VECTOR_FIELD, TypeConverter.toByteArray(vector)));
                    writer.addDocument(doc);
                }
            }
        } finally {
            it.close();
        }
    }
    
    public static List<String> inspectDirectoryAndLoad(String path, String destPath) {
        List<String> modelNames = new ArrayList<>();
        if (path == null || path.length() == 0) {
            LOG.error("Scanning for word2Vec files: wrong path specified.");
            return modelNames;
        }
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null) {
            LOG.warn("No files in " + path + " for loading word2Vec");
            return modelNames;
        }
        LOG.info("path = " + path);
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        
        for (File file : listOfFiles) {
            if (!file.isFile()) {
                continue;
            }
            String fileName = file.getName();
            if (isIgnorableFile(fileName)) {
                continue;
            }
            String[] sp = fileName.split("-");
            String modelName = sp[0];
            LOG.info("Custom models: Found file " + fileName + ". Assigned name: " + modelName);
            if (loadFromFile((path + fileName), (destPath +  modelName), false)) {
                modelNames.add(modelName);
            }
        }

        return modelNames;
    }

    private static boolean isIgnorableFile(String filename) {
        List<String> ignores = Arrays.asList(".DS_Store");

        return ignores.contains(filename);
    }
}
