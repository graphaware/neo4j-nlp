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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Word2VecIndexLookup {

    private static final Log LOG = LoggerFactory.getLogger(Word2VecIndexLookup.class);

    private final String storePath;
    private int vectorDimension;

    private final Map<String, float[]> inMemoryNN = new ConcurrentHashMap<>();
    private final Map<String, List<Pair>> nnCache = new HashMap<>();

    public Word2VecIndexLookup(String storePath) {
        this.storePath = storePath;
        try {
            StoredField binaryVector = (StoredField) getIndexSearcher().doc(0).getField(Word2VecIndexCreator.VECTOR_FIELD);
            this.vectorDimension = TypeConverter.toFloatArray(binaryVector.binaryValue().bytes).length;
        } catch (IOException e) {
            LOG.error("Couldn't retrieve vector dimension. ", e);
            this.vectorDimension = -1;
        }
    }

    public long countIndex() throws IOException {
        return getIndexSearcher().collectionStatistics(Word2VecIndexCreator.WORD_FIELD).docCount();
    }

    public String getStorePath() {
        return storePath;
    }

    public float[] searchIndex(String searchString) {
        try {
            Analyzer analyzer = new KeywordAnalyzer();
            QueryParser queryParser = new QueryParser(Word2VecIndexCreator.WORD_FIELD, analyzer);
            Query query = queryParser.parse(searchString.replace(" ", "_")
                    .replace("*", "\\*").replace("?", "\\?"));
            TopDocs searchResult = getIndexSearcher().search(query, 1);
            LOG.debug("Searching for '" + searchString + "'. Number of hits: " + searchResult.totalHits);
            if (searchResult.totalHits != 1) {
                LOG.debug("Found too many hits for search string " + searchString + ".");
                return null;
            }
            ScoreDoc hit = searchResult.scoreDocs[0];
            Document hitDoc = getIndexSearcher().doc(hit.doc);
            StoredField binaryVector = (StoredField) hitDoc.getField(Word2VecIndexCreator.VECTOR_FIELD);
            return TypeConverter.toFloatArray(binaryVector.binaryValue().bytes);
        } catch (ParseException | IOException ex) {
            LOG.error("Error while getting word2vec for " + searchString, ex.getMessage());
        }
        return null;
    }

    public void loadNN() {
        try {
            IndexSearcher indexSearcher = getIndexSearcher();
            IndexReader indexReader = indexSearcher.getIndexReader();
            for (int i = 0; i < indexReader.maxDoc(); ++i) {
                Document hitDoc = indexReader.document(i);
                String word = hitDoc.getField(Word2VecIndexCreator.WORD_FIELD).stringValue();
                StoredField binaryVector = (StoredField) hitDoc.getField(Word2VecIndexCreator.VECTOR_FIELD);
                inMemoryNN.put(word, TypeConverter.toFloatArray(binaryVector.binaryValue().bytes));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Pair> getNearestNeighbors(String searchString, Integer limit) {
        IndexSearcher indexSearcher = getIndexSearcher();
        LOG.info("Searching nearest neighbors for : '" + searchString + "'");
        String key = searchString + "__" + limit.toString();
        if (inMemoryNN.containsKey(searchString)) {

            if (nnCache.containsKey(key)) {
                return nnCache.get(key);
            }

            return cacheIfNeeded(searchString, getTopNeighbors(inMemoryNN.get(searchString), limit, inMemoryNN), limit);
        }
        try {
            Analyzer analyzer = new KeywordAnalyzer();
            QueryParser queryParser = new QueryParser(Word2VecIndexCreator.WORD_FIELD, analyzer);
            Query query = queryParser.parse(searchString.replace(" ", "_"));
            TopDocs searchResult = indexSearcher.search(query, 1);
            LOG.info("Searching for '" + searchString + "'. Number of hits: " + searchResult.totalHits);
            if (searchResult.totalHits != 1) {
                return new ArrayList<>();
            }
            ScoreDoc hit = searchResult.scoreDocs[0];
            Document hitDoc = indexSearcher.doc(hit.doc);

            return cacheIfNeeded(searchString, getTopXNeighbors(getVector(hitDoc), limit), limit);
        } catch (ParseException | IOException ex) {
            LOG.error("Error while getting word2vec for " + searchString, ex);
        }

        return new ArrayList<>();
    }

    public void cleanCache() {
        nnCache.clear();
    }

    private List<Pair> getTopXNeighbors(float[] originalVector, Integer limit) {
        IndexSearcher indexSearcher = getIndexSearcher();
        IndexReader indexReader = indexSearcher.getIndexReader();
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

        LOG.debug("Computed nearest neighbors in " + (System.currentTimeMillis() - now));
        return coll;
    }

    private List<Pair> getTopNeighbors(float[] originalVector, Integer limit, Map<String, float[]> vectors) {
        FixedSizeOrderedList coll = new FixedSizeOrderedList(limit);
        CosineSimilarity cosineSimilarity = new CosineSimilarity();
        List<Pair> top = vectors.keySet().parallelStream().map(k -> {
            return new ComparablePair(k, cosineSimilarity.cosineSimilarity(originalVector, vectors.get(k)));
        }).collect(Collectors.toList());
        coll.addAll(top);

        return coll;
    }

    private float[] getVector(Document doc) {
        StoredField storedField = (StoredField) doc.getField(Word2VecIndexCreator.VECTOR_FIELD);

        return TypeConverter.toFloatArray(storedField.binaryValue().bytes);
    }

    private IndexSearcher getIndexSearcher() {
        IndexSearcher indexSearcher;
        try {
            IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(storePath)));
            indexSearcher = new IndexSearcher(indexReader);
        } catch (IOException ex) {
            throw new RuntimeException("Error while creating index", ex);
        }

        return indexSearcher;
    }

    private List<Pair> cacheIfNeeded(String word, List<Pair> nn, Integer limit) {
        String key = word + "__" + limit.toString();
        if (!nnCache.containsKey(key)) {
            nnCache.put(key, nn);
        }

        return nn;
    }

    public int getVectorDimension() { return this.vectorDimension; }
}
