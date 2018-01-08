/*
 * Copyright (c) 2013-2017 GraphAware
 *
 * This file is part of the GraphAware Framework.
 *
 * GraphAware Framework is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General License for more details. You should have received a copy of
 * the GNU General License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package com.graphaware.nlp.processor;

import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.dsl.request.PipelineSpecification;

import java.util.List;
import java.util.Map;

public interface TextProcessor {

    String DEFAULT_PIPELINE = "tokenizer";

    void init();

    String getAlias();

    String override();

    List<String> getPipelines();

    List<PipelineInfo> getPipelineInfos();

    void createPipeline(PipelineSpecification pipelineSpecification);

    boolean checkPipeline(String name);

    AnnotatedText annotateText(String text, String pipelineName, String lang, Map<String, String> extraParameters);

    AnnotatedText annotateText(String text, String lang, PipelineSpecification pipelineSpecification);

    Tag annotateSentence(String text, String lang);

    Tag annotateTag(String text, String lang);

    List<Tag> annotateTags(String text, String lang);

    boolean checkLemmaIsValid(String value);

    AnnotatedText sentiment(AnnotatedText annotatedText);

    void removePipeline(String pipeline);

    String train(String alg, String modelId, String file, String lang, Map<String, Object> params);

    String test(String alg, String modelId, String file, String lang);

}
