/*
 * Copyright (c) 2013-2016 GraphAware
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
package com.graphaware.nlp.processor;

import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.domain.Tag;
import java.util.List;
import java.util.Map;

public interface TextProcessor {
    
    public List<String> getPipelines();
    
    public void createPipeline(Map<String, Object> pipelineSpec);
    
    public boolean checkPipeline(String name);

    @Deprecated
    public AnnotatedText annotateText(String text, Object id, int level, String lang, boolean store);

    public AnnotatedText annotateText(String text, Object id, String name, String lang, boolean store, Map<String, String> otherParams);

    public Tag annotateSentence(String text, String lang);

    public Tag annotateTag(String text, String lang);
    
    public List<Tag> annotateTags(String text, String lang);

    public boolean checkPuntuation(String value);

    public AnnotatedText sentiment(AnnotatedText annotated, Map<String, String> otherParams);

    public void removePipeline(String pipeline);

    public String train(String project, String alg, String model, String file, String lang, Map<String, String> params);

}
