package com.graphaware.nlp.dsl.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.dsl.AbstractDSL;
import com.graphaware.nlp.processor.PipelineSpecification;
import com.graphaware.nlp.processor.TextProcessor;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

import java.util.HashMap;
import java.util.Map;

public class AnnotateFunction extends AbstractDSL {

    @UserFunction("ga.nlp.processor.annotate")
    @Description("Perform the annotation on the given text, returns the produced annotation domain")
    public Map<String, Object> getAnnotation(@Name("text") String text, @Name("pipelineSpecification") Map<String, Object> specificationInput) {
        PipelineSpecification pipelineSpecification = PipelineSpecification.fromMap(specificationInput);
        TextProcessor processor = getNLPManager().getTextProcessorsManager().getTextProcessor(pipelineSpecification.getTextProcessor());
        AnnotatedText annotatedText = processor.annotateText(text, pipelineSpecification.getName(), "en", null);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        return mapper.convertValue(annotatedText, Map.class);
    }


}
