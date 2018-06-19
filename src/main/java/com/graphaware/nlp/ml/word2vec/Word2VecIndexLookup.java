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
import org.apache.commons.lang3.StringUtils;
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
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Word2VecIndexLookup {

    private static final Log LOG = LoggerFactory.getLogger(Word2VecIndexLookup.class);

    private final String storePath;

    private final Map<String, List<Pair>> inMemoryNN = new ConcurrentHashMap<>();

    public Word2VecIndexLookup(String storePath) {
        this.storePath = storePath;
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
            Query query = queryParser.parse(searchString.replace(" ", "_"));
            TopDocs searchResult = getIndexSearcher().search(query, 1);
            LOG.info("Searching for '" + searchString + "'. Number of hits: " + searchResult.totalHits);
            if (searchResult.totalHits != 1) {
                return null;
            }
            ScoreDoc hit = searchResult.scoreDocs[0];
            Document hitDoc = getIndexSearcher().doc(hit.doc);
            StoredField binaryVector = (StoredField) hitDoc.getField(Word2VecIndexCreator.VECTOR_FIELD);
            return TypeConverter.toFloatArray(binaryVector.binaryValue().bytes);
        } catch (ParseException | IOException ex) {
            LOG.error("Error while getting word2vec for " + searchString, ex);
        }
        return null;
    }

    public void loadNN(Integer maxNeighbors, IndexWriter indexWriter) throws IOException {
        final AtomicInteger batch = new AtomicInteger(0);
        try {
            IndexSearcher indexSearcher = getIndexSearcher();
            IndexReader indexReader = indexSearcher.getIndexReader();
            IntStream str = IntStream.range(0, indexReader.maxDoc() + 1);
            str.parallel().forEach(i -> {
                try {
                    Document hitDoc = indexReader.document(i);
                    String word = hitDoc.getField(Word2VecIndexCreator.WORD_FIELD).stringValue();
                    List<Pair> nn = getNearestNeighbors(word, maxNeighbors);
                    hitDoc.add(new StoredField(Word2VecIndexCreator.NEAREST_NEIGHBORS_FIELD, asStorableField(nn)));
                    indexWriter.updateDocument(new Term(Word2VecIndexCreator.WORD_FIELD, word), hitDoc);
                    batch.incrementAndGet();
                    if (batch.get() > 1000) {
                        indexWriter.commit();
                        LOG.info("committed 1000 nearest neighbor operations");
                        batch.set(0);
                    }
                } catch (Exception e) {
                    //
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            indexWriter.commit();
            indexWriter.close();

        }

    }

    public List<Pair> getNNFromDisk(String w, Integer maxNeighbors) {
        IndexSearcher indexSearcher = getIndexSearcher();
        List<Pair> result = new ArrayList<>();
        try {
            Analyzer analyzer = new KeywordAnalyzer();
            QueryParser queryParser = new QueryParser(Word2VecIndexCreator.WORD_FIELD, analyzer);
            Query query = queryParser.parse(w.replace(" ", "_"));
            TopDocs searchResult = indexSearcher.search(query, 1);
            for (ScoreDoc scoreDoc : searchResult.scoreDocs) {
                Document hitDoc = indexSearcher.doc(scoreDoc.doc);
                StoredField binaryVector = (StoredField) hitDoc.getField(Word2VecIndexCreator.NEAREST_NEIGHBORS_FIELD);
                return asPairList(binaryVector.binaryValue().bytes).stream().limit(maxNeighbors).collect(Collectors.toList());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    public List<Pair> getNearestNeighbors(String searchString, Integer limit) {
        IndexSearcher indexSearcher = getIndexSearcher();
        LOG.info("Searching nearest neighbors for : '" + searchString + "'");
        if (inMemoryNN.containsKey(searchString)) {
            return inMemoryNN.get(searchString).stream().limit(limit).collect(Collectors.toList());
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

    private float[] getVector(Document doc) {
        StoredField storedField = (StoredField) doc.getField(Word2VecIndexCreator.VECTOR_FIELD);

        return TypeConverter.toFloatArray(storedField.binaryValue().bytes);
    }

    private byte[] asStorableField(List<Pair> nns) {
        return compress(nns).getBytes(Charset.forName("UTF-8"));
    }

    private String compress(List<Pair> nns) {
        List<String> parts = new ArrayList<>();
        nns.forEach(nn -> {
            parts.add(nn.first().toString() + "__" + nn.second().toString());
        });

        return StringUtils.join(parts, ">>>>");
    }

    private List<Pair> asPairList(byte[] bytes) throws Exception {
        return uncompress(new String(bytes, "UTF-8"));
    }

    private List<Pair> uncompress(String s) {
        List<Pair> coll = new ArrayList<>();
        String[] elts = s.split(">>>>");
        for (String el : elts) {
            List<String> parts = Arrays.asList(el.split("__"));
            coll.add(new ComparablePair(parts.get(0), parts.get(1)));
        }

        return coll;
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
}
