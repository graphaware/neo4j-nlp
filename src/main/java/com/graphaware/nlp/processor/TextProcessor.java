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
import com.graphaware.nlp.domain.Sentence;
import com.graphaware.nlp.domain.Tag;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import static edu.stanford.nlp.sequences.SeqClassifierFlags.DEFAULT_BACKGROUND_SYMBOL;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.StringUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TextProcessor.class);

    public enum PIPELINE {
        BASIC,
        SENTIMENT,
        COMPLETE
    }

    public String backgroundSymbol = DEFAULT_BACKGROUND_SYMBOL;
    private static final String CUSTOM_STOP_WORD_LIST = "start,starts,period,periods,a,an,and,are,as,at,be,but,by,for,if,in,into,is,it,no,not,of,o,on,or,such,that,the,their,then,there,these,they,this,to,was,will,with";

    private final Map<PIPELINE, StanfordCoreNLP> pipelines = new HashMap<>();
    private final Pattern patternCheck;

    public TextProcessor() {
        createBasicPipeline();
        createSentimentPipeline();
        createCompletePipeline();

        String pattern = "\\p{Punct}";
        patternCheck = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    }

    private void createBasicPipeline() {
        StanfordCoreNLP pipeline = new PipelineBuilder()
                .tokenize()
                .defaultStopWordAnnotator()
                .build();
        pipelines.put(PIPELINE.BASIC, pipeline);
    }

    private void createSentimentPipeline() {
        StanfordCoreNLP pipeline = new PipelineBuilder()
                .tokenize()
                .extractSentiment()
                .build();
        pipelines.put(PIPELINE.SENTIMENT, pipeline);
    }

    private void createCompletePipeline() {
        StanfordCoreNLP pipeline = new PipelineBuilder()
                .tokenize()
                .defaultStopWordAnnotator()
                .extractSentiment()
                .threadNumber(6)
                .build();
        pipelines.put(PIPELINE.COMPLETE, pipeline);
    }

    public AnnotatedText annotateText(String text, Object id, boolean sentiment, boolean store) {
        AnnotatedText result = new AnnotatedText(id);
        Annotation document = new Annotation(text);
        if (sentiment) {
            pipelines.get(PIPELINE.COMPLETE).annotate(document);
        } else {
            pipelines.get(PIPELINE.BASIC).annotate(document);
        }
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        final AtomicInteger sentenceSequence = new AtomicInteger(0);
        sentences.stream().map((sentence) -> {
            return sentence;
        }).forEach((sentence) -> {
            String sentenceId = id + "_" + sentenceSequence.getAndIncrement();
            final Sentence newSentence = new Sentence(sentence.toString(), store, sentenceId);
            extractTokens(sentence, newSentence);
            if (sentiment) {
                extractSentiment(sentence, newSentence);
            }
            result.addSentence(newSentence);

        });
        return result;
    }

    protected void extractSentiment(CoreMap sentence, final Sentence newSentence) {
        int score = extractSentiment(sentence);
        newSentence.setSentiment(score);
    }

    protected void extractTokens(CoreMap sentence, final Sentence newSentence) {
        List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
        final String background = backgroundSymbol;
//      final AtomicReference<String> prevNe = new AtomicReference<>();
//      prevNe.set(background);
//      final AtomicReference<StringBuilder> sb = new AtomicReference<>();
//      sb.set(new StringBuilder());
        TokenHolder currToken = new TokenHolder();
        currToken.setNe(background);
        tokens.stream()
                .filter((token) -> (token != null) && checkPuntuation(token.get(CoreAnnotations.LemmaAnnotation.class)))
                .map((token) -> {
                    //
                    String currentNe = StringUtils.getNotNullString(token.get(CoreAnnotations.NamedEntityTagAnnotation.class));
                    if (currentNe.equals(background) && currToken.getNe().equals(background)) {
                        Tag tag = getTag(token);
                        if (tag != null) {
                            newSentence.addTag(tag);
                            newSentence.addOccurrence(token.beginPosition(), token.endPosition(), tag);
                        }
                    } else if (currentNe.equals(background) && !currToken.getNe().equals(background)) {
                        Tag newTag = new Tag(currToken.getToken());
                        newTag.setNe(currToken.getNe());
                        newSentence.addTag(newTag);
                        newSentence.addOccurrence(currToken.getBeginPosition(), currToken.getEndPosition(), newTag);
                        currToken.reset();
                        Tag tag = getTag(token);
                        if (tag != null) {
                            newSentence.addTag(tag);
                            newSentence.addOccurrence(token.beginPosition(), token.endPosition(), tag);
                        }
                    } else if (!currentNe.equals(currToken.getNe()) && !currToken.getNe().equals(background)) {
                        Tag tag = new Tag(currToken.getToken());
                        tag.setNe(currToken.getNe());
                        newSentence.addTag(tag);
                        newSentence.addOccurrence(currToken.getBeginPosition(), currToken.getEndPosition(), tag);
                        currToken.reset();
                        currToken.updateToken(StringUtils.getNotNullString(token.get(CoreAnnotations.OriginalTextAnnotation.class)));
                        currToken.setBeginPosition(token.beginPosition());
                        currToken.setEndPosition(token.endPosition());
                    } else if (!currentNe.equals(background) && currToken.getNe().equals(background)) {
                        currToken.updateToken(StringUtils.getNotNullString(token.get(CoreAnnotations.OriginalTextAnnotation.class)));
                        currToken.setBeginPosition(token.beginPosition());
                        currToken.setEndPosition(token.endPosition());
                    } else {
                        String before = StringUtils.getNotNullString(token.get(CoreAnnotations.BeforeAnnotation.class));
                        String currentText = StringUtils.getNotNullString(token.get(CoreAnnotations.OriginalTextAnnotation.class));
                        currToken.updateToken(before);
                        currToken.updateToken(currentText);
                        currToken.setBeginPosition(token.beginPosition());
                        currToken.setEndPosition(token.endPosition());
                    }
                    return currentNe;
                }).forEach((currentNe) -> {
            currToken.setNe(currentNe);
        });

        if (currToken.getToken().length() > 0) {
            Tag tag = new Tag(currToken.getToken());
            tag.setNe(currToken.getNe());
            newSentence.addTag(tag);
            newSentence.addOccurrence(currToken.getBeginPosition(), currToken.getEndPosition(), tag);
        }
    }

    public AnnotatedText sentiment(AnnotatedText annotated) {
        StanfordCoreNLP pipeline = pipelines.get(PIPELINE.SENTIMENT);
        annotated.getSentences().parallelStream().forEach((item) -> {
            Annotation document = new Annotation(item.getSentence());
            pipeline.annotate(document);
            List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
            Optional<CoreMap> sentence = sentences.stream().findFirst();
            if (sentence != null && sentence.isPresent()) {
                extractSentiment(sentence.get(), item);
            }
        });
        return annotated;
    }

    private int extractSentiment(CoreMap sentence) {
        Tree tree = sentence
                .get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
        int score = RNNCoreAnnotations.getPredictedClass(tree);
        return score;
    }

    public Tag annotateSentence(String text) {
        Annotation document = new Annotation(text);
        pipelines.get(PIPELINE.SENTIMENT).annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        Optional<CoreMap> sentence = sentences.stream().findFirst();
        if (sentence.isPresent()) {
            Optional<Tag> oTag = sentence.get().get(CoreAnnotations.TokensAnnotation.class).stream()
                    .map((token) -> getTag(token))
                    .filter((tag) -> (tag != null) && checkPuntuation(tag.getLemma()))
                    .findFirst();
            if (oTag.isPresent()) {
                return oTag.get();
            }
        }
        return null;
    }

    public Tag annotateTag(String text) {
        Annotation document = new Annotation(text);
        pipelines.get(PIPELINE.BASIC).annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        Optional<CoreMap> sentence = sentences.stream().findFirst();
        if (sentence.isPresent()) {
            Optional<Tag> oTag = sentence.get().get(CoreAnnotations.TokensAnnotation.class).stream()
                    .map((token) -> getTag(token))
                    .filter((tag) -> (tag != null) && checkPuntuation(tag.getLemma()))
                    .findFirst();
            if (oTag.isPresent()) {
                return oTag.get();
            }
        }
        return null;
    }

    private Tag getTag(CoreLabel token) {
        Pair<Boolean, Boolean> stopword = token.get(StopwordAnnotator.class);
        if (stopword.first()) {
            return null;
        }
        String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
        String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
        String lemma;

        if (ne.equals(backgroundSymbol)) {
            lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
        } else {
            lemma = token.get(CoreAnnotations.OriginalTextAnnotation.class);
        }

        Tag tag = new Tag(lemma);
        tag.setPos(pos);
        tag.setNe(ne);
        LOG.info("POS: " + pos + " ne: " + ne + " lemma: " + lemma);
        return tag;
    }

    public boolean checkPuntuation(String value) {
        Matcher match = patternCheck.matcher(value);
        return !match.find();
    }

    class TokenHolder {

        private String ne;
        private StringBuilder sb;
        private int beginPosition;
        private int endPosition;

        public TokenHolder() {
            reset();
        }

        public String getNe() {
            return ne;
        }

        public String getToken() {
            if (sb == null) {
                return " - ";
            }
            return sb.toString();
        }

        public int getBeginPosition() {
            return beginPosition;
        }

        public int getEndPosition() {
            return endPosition;
        }

        public void setNe(String ne) {
            this.ne = ne;
        }

        public void updateToken(String tknStr) {
            this.sb.append(tknStr);
        }

        public void setBeginPosition(int beginPosition) {
            if (this.beginPosition < 0) {
                this.beginPosition = beginPosition;
            }
        }

        public void setEndPosition(int endPosition) {
            this.endPosition = endPosition;
        }

        public final void reset() {
            sb = new StringBuilder();
            beginPosition = -1;
            endPosition = -1;
        }
    }

    static class PipelineBuilder {

        private final Properties properties = new Properties();
        private final StringBuilder annotattors = new StringBuilder(); //basics annotators
        private int threadsNumber = 4;

        public PipelineBuilder tokenize() {
            checkForExistingAnnotators();
            annotattors.append("tokenize, ssplit, pos, lemma, ner");
            return this;
        }

        private void checkForExistingAnnotators() {
            if (annotattors.toString().length() > 0) {
                annotattors.append(", ");
            }
        }

        public PipelineBuilder extractSentiment() {
            checkForExistingAnnotators();
            annotattors.append("parse, sentiment");
            return this;
        }

        public PipelineBuilder extractRelations() {
            checkForExistingAnnotators();
            annotattors.append("relation");
            return this;
        }

        public PipelineBuilder defaultStopWordAnnotator() {
            checkForExistingAnnotators();
            annotattors.append("stopword");
            properties.setProperty("customAnnotatorClass.stopword", "com.graphaware.nlp.processor.StopwordAnnotator");
            properties.setProperty(StopwordAnnotator.STOPWORDS_LIST, CUSTOM_STOP_WORD_LIST);
            return this;
        }

        public PipelineBuilder stopWordAnnotator(Properties properties) {
            properties.entrySet().stream().forEach((entry) -> {
                this.properties.setProperty((String) entry.getKey(), (String) entry.getValue());
            });
            return this;
        }

        public PipelineBuilder threadNumber(int threads) {
            this.threadsNumber = threads;
            return this;
        }

        public StanfordCoreNLP build() {
            properties.setProperty("annotators", annotattors.toString());
            properties.setProperty("threads", String.valueOf(threadsNumber));
            StanfordCoreNLP pipeline = new StanfordCoreNLP(properties);
            return pipeline;
        }
    }
}
