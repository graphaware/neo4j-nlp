package com.graphaware.nlp.stub;

import com.graphaware.nlp.annotation.NLPTextProcessor;
import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.domain.Phrase;
import com.graphaware.nlp.domain.Sentence;
import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.dsl.request.PipelineSpecification;
import com.graphaware.nlp.processor.AbstractTextProcessor;
import com.graphaware.nlp.processor.TextProcessor;

import java.util.*;

@NLPTextProcessor(name = "StubTextProcessor")
public class StubTextProcessor extends AbstractTextProcessor {

    private String lastPipelineUsed = "";

    private final Map<String, Object> pipelines = new HashMap<>();

    @Override
    public void init() {

    }

    @Override
    public String getAlias() {
        return "stub";
    }

    @Override
    public String override() {
        return null;
    }

    @Override
    public List<String> getPipelines() {
        return new ArrayList<>(pipelines.keySet());
    }

    @Override
    public void createPipeline(PipelineSpecification pipelineSpecification) {
        String name = pipelineSpecification.getName();
        pipelines.put(name, null);
    }

    @Override
    public boolean checkPipeline(String name) {
        return pipelines.containsKey(name);
    }

    @Override
    public AnnotatedText annotateText(String text, PipelineSpecification pipelineSpecification) {
        this.lastPipelineUsed = pipelineSpecification.getName();
        AnnotatedText annotatedText = new AnnotatedText();
        String[] sentencesSplit = text.split("\\.");
        int sentenceNumber = 0;
        for (String stext : sentencesSplit) {
            String[] parts = stext.split(" ");
            int pos = 0;
            final Sentence sentence = new Sentence(stext, sentenceNumber);
            for (String token : parts) {
                Tag tag = new Tag(token, pipelineSpecification.getLanguage());
                if (!pipelineSpecification.getExcludedNER().contains("test")) {
                    tag.setNe(Collections.singletonList("test"));
                }
                tag.setPos(Collections.singletonList("TESTVB"));
                int begin = pos;
                pos += token.length() + 1;
                sentence.addTagOccurrence(begin, pos, token, sentence.addTag(tag));
            }
            if (pipelineSpecification.hasProcessingStep("phrase")) {
                Phrase phrase = new Phrase(stext);
                sentence.addPhraseOccurrence(0, stext.length(), phrase);
            }
            annotatedText.addSentence(sentence);
            sentenceNumber++;
        }

        return annotatedText;
    }

    @Override
    public Tag annotateSentence(String text, PipelineSpecification pipelineSpecification) {
        return null;
    }

    @Override
    public Tag annotateTag(String text, PipelineSpecification pipelineSpecification) {
        return null;
    }

    @Override
    public List<Tag> annotateTags(String text, String lang) {
        return null;
    }

    @Override
    public List<Tag> annotateTags(String text, PipelineSpecification pipelineSpecification) {
        return null;
    }

    @Override
    public boolean checkLemmaIsValid(String value) {
        return true;
    }

    @Override
    public AnnotatedText sentiment(AnnotatedText annotatedText) {
        annotatedText.getSentences().forEach(sentence -> {
            sentence.setSentiment(4);
        });

        return annotatedText;
    }

    @Override
    public void removePipeline(String pipeline) {
        pipelines.remove(pipeline);
    }

    @Override
    public String train(String alg, String modelId, String file, String lang, Map<String, Object> params) {
        String modelFile = getModelsWorkdir() + "/" + file + ".gz";
        storeModelLocation(modelId, modelFile);

        return null;
    }

    @Override
    public String test(String alg, String modelId, String file, String lang) {
        return null;
    }

    public String getLastPipelineUsed() {
        return lastPipelineUsed;
    }
}
