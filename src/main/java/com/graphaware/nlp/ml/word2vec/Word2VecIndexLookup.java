/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.ml.word2vec;

import com.graphaware.nlp.util.TypeConverter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ale
 */
public class Word2VecIndexLookup {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(Word2VecIndexLookup.class);

    private final IndexReader indexReader;
    private final IndexSearcher indexSearcher;
    
    private final Set<String> fieldsToLoad;

    public Word2VecIndexLookup(String storePath) {
        try {
            indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(storePath)));;
            indexSearcher = new IndexSearcher(indexReader);
            this.fieldsToLoad = new HashSet<>();
            fieldsToLoad.add(Word2VecIndexCreator.WORD_FIELD);
            fieldsToLoad.add(Word2VecIndexCreator.VECTOR_FIELD);
        } catch (IOException ex) {
            throw new RuntimeException("Error while creating index", ex);
        }
    }

    public double[] searchIndex(String searchString) {
        try {
            Analyzer analyzer = new KeywordAnalyzer();
            QueryParser queryParser = new QueryParser(Word2VecIndexCreator.WORD_FIELD, analyzer);
            Query query = queryParser.parse(searchString.replace(" ", "_"));
            TopDocs searchResult = indexSearcher.search(query, 1);
            LOG.info("Searching for '" + searchString + "'. Number of hits: " + searchResult.totalHits);
            if (searchResult.totalHits != 1) {
                return null;
            }
            ScoreDoc hit = searchResult.scoreDocs[0];
            Document hitDoc = indexSearcher.doc(hit.doc);
            StoredField binaryVector = (StoredField) hitDoc.getField(Word2VecIndexCreator.VECTOR_FIELD);
            return TypeConverter.toDoubleArray(binaryVector.binaryValue().bytes);
        } catch (ParseException | IOException ex) {
            LOG.error("Error while getting word2vec for " + searchString, ex);
        }
        return null;
    }

}
