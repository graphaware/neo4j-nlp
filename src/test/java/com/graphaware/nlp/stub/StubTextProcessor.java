package com.graphaware.nlp.stub;

import com.graphaware.nlp.annotation.NLPTextProcessor;
import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.domain.Phrase;
import com.graphaware.nlp.processor.AbstractTextProcessor;
import com.graphaware.nlp.processor.PipelineInfo;
import com.graphaware.nlp.domain.Sentence;
import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.dsl.request.PipelineSpecification;
import com.graphaware.nlp.processor.TextProcessor;

import java.util.*;

@NLPTextProcessor(name = "StubTextProcessor")
public class StubTextProcessor implements TextProcessor {

    private String lastPipelineUsed = "";

    @Override
    public String getAlias() {
        return "stub";
    }

    @Override
    public String override() {
        return null;
    }

    @Override
    public void init() {
        this.pipelineInfos.put("tokenizer", new PipelineInfo(
                "tokenizer",
                StubTextProcessor.class.getName(),
                Collections.emptyMap(),
                Collections.singletonMap("tokenize", true),
                4,
                Arrays.asList("start", "starter")
        ));
    }

    private final Map<String, PipelineInfo> pipelineInfos = new HashMap<>();

    @Override
    public List<String> getPipelines() {
        return new ArrayList<>(pipelineInfos.keySet());
    }

    @Override
    public List<PipelineInfo> getPipelineInfos() {
        List<PipelineInfo> list = new ArrayList<>();
        pipelineInfos.values().forEach(pipelineInfo -> {
            list.add(pipelineInfo);
        });

        return list;
    }

    @Override
    public void createPipeline(PipelineSpecification pipelineSpecification) {
        String name = pipelineSpecification.getName();
        pipelineInfos.put(name, new PipelineInfo(name, this.getClass().getName(), Collections.emptyMap(), Collections.emptyMap(), 4, Collections.emptyList()));
    }

    @Override
    public boolean checkPipeline(String name) {
        return pipelineInfos.containsKey(name);
    }



    @Override
    public AnnotatedText annotateText(String text, String pipelineName, String lang, Map<String, String> extraParams) {
        this.lastPipelineUsed = pipelineName;
        AnnotatedText annotatedText = new AnnotatedText();
        String[] sentencesSplit = text.split("\\.");
        int sentenceNumber = 0;
        for (String stext : sentencesSplit) {
            String[] parts = stext.split(" ");
            int pos = 0;
            final Sentence sentence = new Sentence(stext, sentenceNumber);
            for (String token : parts) {
                Tag tag = new Tag(token, lang);
                tag.setNe(Collections.singletonList("test"));
                tag.setPos(Collections.singletonList("TESTVB"));
                int begin = pos;
                pos += token.length() + 1;
                sentence.addTagOccurrence(begin, pos, token, sentence.addTag(tag));
            }
            Phrase phrase = new Phrase(stext);
            sentence.addPhraseOccurrence(0, stext.length(), phrase);
            annotatedText.addSentence(sentence);
            sentenceNumber++;
        }

        return annotatedText;
    }

    @Override
    public AnnotatedText annotateText(String text, String lang, PipelineSpecification pipelineSpecification) {
        this.lastPipelineUsed = "CORE";
        AnnotatedText annotatedText = new AnnotatedText();
        String[] sentencesSplit = text.split("\\.");
        int sentenceNumber = 0;
        for (String stext : sentencesSplit) {
            String[] parts = stext.split(" ");
            int pos = 0;
            final Sentence sentence = new Sentence(stext, sentenceNumber);
            for (String token : parts) {
                Tag tag = new Tag(token, lang);
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
    public Tag annotateSentence(String text, String lang) {
        return null;
    }

    @Override
    public Tag annotateTag(String text, String lang) {
        return null;
    }

    @Override
    public List<Tag> annotateTags(String text, String lang) {
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
        pipelineInfos.remove(pipeline);
    }

    @Override
    public String train(String alg, String modelId, String file, String lang, Map<String, Object> params) {
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
