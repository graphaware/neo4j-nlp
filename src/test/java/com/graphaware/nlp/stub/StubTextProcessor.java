package com.graphaware.nlp.stub;

import com.graphaware.nlp.annotation.NLPTextProcessor;
import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.domain.Phrase;
import com.graphaware.nlp.processor.PipelineInfo;
import com.graphaware.nlp.domain.Sentence;
import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.processor.PipelineSpecification;
import com.graphaware.nlp.processor.TextProcessor;

import java.util.*;

@NLPTextProcessor(name = "StubTextProcessor")
public class StubTextProcessor implements TextProcessor {

    private final Map<String, PipelineInfo> pipelineInfos = new HashMap<>();

    @Override
    public List<String> getPipelines() {
        return new ArrayList<>();
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
        return false;
    }



    @Override
    public AnnotatedText annotateText(String text, String pipelineName, String lang, Map<String, String> extraParams) {
        AnnotatedText annotatedText = new AnnotatedText();
        String[] sentencesSplit = text.split("\\.");
        int sentenceNumber = 0;
        for (String stext : sentencesSplit) {
            String[] parts = stext.split(" ");
            int pos = 0;
            final Sentence sentence = new Sentence(stext, sentenceNumber);
            for (String token : parts) {
                Tag tag = new Tag(token, "en");
                tag.setNe(Collections.singletonList("test"));
                tag.setPos(Collections.singletonList("TESTVB"));
                int begin = pos;
                pos += token.length() + 1;
                sentence.addTagOccurrence(begin, pos, sentence.addTag(tag));
            }
            Phrase phrase = new Phrase(stext);
            sentence.addPhraseOccurrence(0, stext.length(), phrase);
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
    public AnnotatedText sentiment(AnnotatedText annotated, Map<String, String> otherParams) {
        return null;
    }

    @Override
    public void removePipeline(String pipeline) {
        pipelineInfos.remove(pipeline);
    }

    @Override
    public String train(String project, String alg, String model, String file, String lang, Map<String, String> params) {
        return null;
    }

    @Override
    public String test(String project, String alg, String model, String file, String lang) {
        return null;
    }
}
