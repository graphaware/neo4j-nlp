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

import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.annotation.NLPModuleExtension;
import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.dsl.request.Word2VecRequest;
import com.graphaware.nlp.extension.AbstractExtension;
import com.graphaware.nlp.extension.NLPExtension;
import com.graphaware.nlp.persistence.constants.Labels;
import com.graphaware.nlp.processor.TextProcessor;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@NLPModuleExtension(name = "Word2VecProcessor")
public class Word2VecProcessor extends AbstractExtension implements NLPExtension {

    private static final Logger LOG = LoggerFactory.getLogger(Word2VecProcessor.class);
    private static final String RELATIONSHIP_IS_RELATED_TO_SUB_TAG = "subTag";

    private Word2VecModel word2VecModel;

    @Override
    public void postLoaded() {
        word2VecModel = new Word2VecModel();
        word2VecModel.init();
    }

    public int attach(Word2VecRequest request) {
        try {
            Iterator<Node> tagsIterator;
            if (request.getAnnotatedNode() != null) {
                tagsIterator = getAnnotatedTextTags(request.getAnnotatedNode());
            } else if (request.getTagNode() != null) {
                List<Node> proc = new ArrayList<>();
                proc.add(request.getTagNode());
                tagsIterator = proc.iterator();
            } else if (request.getQuery() != null) {
                tagsIterator = getByQuery(request.getQuery());
            } else {
                throw new RuntimeException("You need to specify or an annotated text "
                        + "or a tag "
                        + "or a query");
            }
            TextProcessor processor = getProcessor(request.getProcessor());
            List<Tag> tags = new ArrayList<>();
            while (tagsIterator.hasNext()) {
                Tag tag = (Tag) getPersister(Tag.class).fromNode(tagsIterator.next());
                if (request.getSplitTags()) {
                    List<Tag> annotateTags = processor.annotateTags(tag.getLemma(), request.getLang());
                    if (annotateTags.size() == 1 && annotateTags.get(0).getLemma().equalsIgnoreCase(tag.getLemma())) {
                        tags.add(tag);
                    } else {
                        annotateTags.forEach((newTag) -> {
                            tags.add(newTag);
                            tag.addParent(RELATIONSHIP_IS_RELATED_TO_SUB_TAG, newTag, 0.0f);
                        });
                    }
                } else {
                    tags.add(tag);
                }
            }
            List<Tag> extendedTags = new ArrayList<>();
            tags.stream().forEach((tag) -> {
                LOG.info("Searching for: " + tag.getLemma().toLowerCase());
                double[] vector = word2VecModel.getWordToVec(tag.getLemma().toLowerCase(), request.getModelName());
                if (vector != null) {
                    tag.addProperties(request.getPropertyName(), vector);
                    extendedTags.add(tag);
                }
            });
            AtomicInteger affectedTag = new AtomicInteger(0);
            extendedTags.stream().forEach((newTag) -> {
                if (newTag != null) {
                    getPersister(Tag.class).getOrCreate(newTag, newTag.getId(), String.valueOf(System.currentTimeMillis()));

                    affectedTag.incrementAndGet();
                }
            });
            return affectedTag.get();
        } catch (Exception ex) {
            LOG.error("Error!!!! ", ex);
            throw new RuntimeException("Error", ex);
        }
    }

    public double[] getWord2Vec(String value, String modelName) {
        return word2VecModel.getWordToVec(value, modelName);
    }

    public Word2VecModel getWord2VecModel() {
        return word2VecModel;
    }

    private TextProcessor getProcessor(String processor) throws RuntimeException {
        if (processor.length() > 0) {
            TextProcessor textProcessorInstance
                    = NLPManager.getInstance().getTextProcessorsManager().getTextProcessor(processor);
            if (textProcessorInstance == null) {
                throw new RuntimeException("Text processor " + processor + " doesn't exist");
            }
            return textProcessorInstance;
        }
        return NLPManager.getInstance().getTextProcessorsManager().getDefaultProcessor();
    }

    private ResourceIterator<Node> getAnnotatedTextTags(Node annotatedNode) throws QueryExecutionException {
        Map<String, Object> params = new HashMap<>();
        params.put("id", annotatedNode.getId());
        Result queryRes = getDatabase().execute("MATCH (n:" 
                + getConfiguration().getLabelFor(Labels.AnnotatedText) 
                + ")-[*..2]->(t:"
                + getConfiguration().getLabelFor(Labels.Tag) 
                + ") where id(n) = {id} return distinct t", 
                params);
        ResourceIterator<Node> tags = queryRes.columnAs("t");
        return tags;
    }

    private ResourceIterator<Node> getByQuery(String query) throws QueryExecutionException {
        Map<String, Object> params = new HashMap<>();
        Result queryRes = getDatabase().execute(query, params);
        ResourceIterator<Node> tags = queryRes.columnAs("t");
        return tags;
    }
}
