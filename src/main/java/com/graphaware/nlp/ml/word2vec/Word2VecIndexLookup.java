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
package com.graphaware.nlp.ml.word2vec;

import com.graphaware.common.util.Pair;
import com.graphaware.nlp.ml.similarity.CosineSimilarity;
import com.graphaware.nlp.util.ComparablePair;
import com.graphaware.nlp.util.FixedSizeOrderedList;
import com.graphaware.nlp.util.TypeConverter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import com.graphaware.common.log.LoggerFactory;
import org.neo4j.logging.Log;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Word2VecIndexLookup {

    private static final Log LOG = LoggerFactory.getLogger(Word2VecIndexLookup.class);

    private final String storePath;
    private final IndexReader indexReader;
    private final IndexSearcher indexSearcher;
    private final Set<String> fieldsToLoad;

    private final Map<String, List<Pair>> inMemoryNN = new HashMap<>();

    public Word2VecIndexLookup(String storePath) {
        this.storePath = storePath;
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

    public long countIndex() throws IOException {
        return indexSearcher.collectionStatistics(Word2VecIndexCreator.WORD_FIELD).docCount();
    }

    public String getStorePath() {
        return storePath;
    }

    public float[] searchIndex(String searchString) {
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
            return TypeConverter.toFloatArray(binaryVector.binaryValue().bytes);
        } catch (ParseException | IOException ex) {
            LOG.error("Error while getting word2vec for " + searchString, ex);
        }
        return null;
    }

    public void loadNN(Integer maxNeighbors) {
        inMemoryNN.clear();
        try {
            Query query = new MatchAllDocsQuery();
            TopDocs searchResult = indexSearcher.search(query, indexReader.numDocs());
            for (ScoreDoc scoreDoc : searchResult.scoreDocs) {
                Document hitDoc = indexSearcher.doc(scoreDoc.doc);
                String word = hitDoc.getField(Word2VecIndexCreator.WORD_FIELD).stringValue();
                inMemoryNN.put(word, getTopXNeighbors(getVector(hitDoc), maxNeighbors));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Pair> getNearestNeighbors(String searchString, Integer limit) {
        LOG.info("Searching nearest neighbors for : '" + searchString + "'");
        if (inMemoryNN.containsKey(searchString)) {
            return inMemoryNN.get(searchString);
        }
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

            return getTopXNeighbors(getVector(hitDoc), limit);
        } catch (ParseException | IOException ex) {
            LOG.error("Error while getting word2vec for " + searchString, ex);
        }

        return new ArrayList<>();
    }

    private List<Pair> getTopXNeighbors(float[] originalVector, Integer limit) {
        long now = System.currentTimeMillis();
        FixedSizeOrderedList coll = new FixedSizeOrderedList(limit);
        CosineSimilarity cosineSimilarity = new CosineSimilarity();
        try {
            Query query = new MatchAllDocsQuery();
            TopDocs searchResult = indexSearcher.search(query, indexReader.numDocs());
            for (ScoreDoc scoreDoc : searchResult.scoreDocs) {
                Document hitDoc = indexSearcher.doc(scoreDoc.doc);
                String word = hitDoc.getField(Word2VecIndexCreator.WORD_FIELD).stringValue();
                coll.add(new ComparablePair(word, cosineSimilarity.cosineSimilarity(originalVector, getVector(hitDoc))));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        LOG.info("Computed nearest neighbors in " + (System.currentTimeMillis() - now));
        return coll;
    }

    private float[] getVector(Document doc) {
        StoredField storedField = (StoredField) doc.getField(Word2VecIndexCreator.VECTOR_FIELD);

        return TypeConverter.toFloatArray(storedField.binaryValue().bytes);
    }
}
