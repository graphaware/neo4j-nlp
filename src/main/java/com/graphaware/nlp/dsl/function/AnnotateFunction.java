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
package com.graphaware.nlp.dsl.function;

import org.codehaus.jackson.map.ObjectMapper;
import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.dsl.AbstractDSL;
import com.graphaware.nlp.dsl.request.PipelineSpecification;
import com.graphaware.nlp.processor.TextProcessor;
import org.codehaus.jackson.map.SerializationConfig;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

import java.util.Map;

public class AnnotateFunction extends AbstractDSL {

    @UserFunction("ga.nlp.processor.annotate")
    @Description("Perform the annotation on the given text, returns the produced annotation domain")
    public Map<String, Object> getAnnotation(@Name("text") String text, @Name("pipelineSpecification") Map<String, Object> specificationInput) {
        PipelineSpecification pipelineSpecification = PipelineSpecification.fromMap(specificationInput);
        TextProcessor processor = getNLPManager().getTextProcessorsManager().retrieveTextProcessor(pipelineSpecification.getTextProcessor(), pipelineSpecification.getName());
        AnnotatedText annotatedText = processor.annotateText(text, pipelineSpecification.getName(), "en", null);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);

        return mapper.convertValue(annotatedText, Map.class);
    }


}
