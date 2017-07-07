/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.ml.word2vec;

import com.graphaware.nlp.util.TypeConverter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
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

/**
 *
 * @author ale
 */
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
                    doc.add(new StringField(WORD_FIELD, split[0], Field.Store.YES));
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
        
        for (File file : listOfFiles) {
            if (!file.isFile()) {
                continue;
            }
            String fileName = file.getName();
            String[] sp = fileName.split("-");
            String modelName = sp[0];
            LOG.info("Custom models: Found file " + fileName + ". Assigned name: " + modelName);
            if (loadFromFile((path + fileName), (destPath +  modelName), false)) {
                modelNames.add(modelName);
            }
        }
        return modelNames;
    }
}
