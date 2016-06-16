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
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import static edu.stanford.nlp.sequences.SeqClassifierFlags.DEFAULT_BACKGROUND_SYMBOL;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.StringUtils;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextProcessor {

    public String backgroundSymbol = DEFAULT_BACKGROUND_SYMBOL;
    private static final String CUSTOM_STOP_WORD_LIST = "start,starts,period,periods,a,an,and,are,as,at,be,but,by,for,if,in,into,is,it,no,not,of,o,on,or,such,that,the,their,then,there,these,they,this,to,was,will,with";

    private final StanfordCoreNLP pipeline;
    private final Pattern patternCheck;

    public TextProcessor() {
        Properties props = new Properties();
        //props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
        //props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner");
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, stopword");
        props.setProperty("customAnnotatorClass.stopword", "com.graphaware.nlp.processor.StopwordAnnotator");
        props.setProperty(StopwordAnnotator.STOPWORDS_LIST, CUSTOM_STOP_WORD_LIST);
        pipeline = new StanfordCoreNLP(props);
        String pattern = "\\p{Punct}";
        patternCheck = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    }

    public AnnotatedText annotateText(String text, Object id) {
        AnnotatedText result = new AnnotatedText(text, id);
        Annotation document = new Annotation(text);
        pipeline.annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        final String background = backgroundSymbol;
        sentences.stream().map((sentence) -> {
            return sentence;
        }).forEach((sentence) -> {
            final Sentence newSentence = new Sentence(sentence.toString());
            final AtomicReference<String> prevNe = new AtomicReference<>();
            prevNe.set(background);
            final AtomicReference<StringBuilder> sb = new AtomicReference<>();
            sb.set(new StringBuilder());
            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
            tokens.stream()
                    .filter((token) -> (token != null) && checkPuntuation(token.get(CoreAnnotations.LemmaAnnotation.class)))
                    .map((token) -> {
                        //
                        String currentNe = StringUtils.getNotNullString(token.get(CoreAnnotations.NamedEntityTagAnnotation.class));
                        if (currentNe.equals(background) && prevNe.get().equals(background)) {
                            Tag tag = getTag(token);
                            if (tag != null) {
                                newSentence.addTag(tag);
                            }
                        } else if (currentNe.equals(background) && !prevNe.get().equals(background)) {
                            Tag newTag = new Tag(sb.get().toString());
                            newTag.setNe(prevNe.get());
                            newSentence.addTag(newTag);
                            sb.set(new StringBuilder());
                            Tag tag = getTag(token);
                            if (tag != null) {
                                newSentence.addTag(tag);
                            }
                        } else if (!currentNe.equals(prevNe.get()) && !prevNe.get().equals(background)) {
                            Tag newTag = new Tag(sb.get().toString());
                            newTag.setNe(prevNe.get());
                            newSentence.addTag(newTag);
                            sb.set(new StringBuilder());
                            sb.get().append(StringUtils.getNotNullString(token.get(CoreAnnotations.OriginalTextAnnotation.class)));
                        } else if (!currentNe.equals(background) && prevNe.get().equals(background)) {
                            sb.get().append(StringUtils.getNotNullString(token.get(CoreAnnotations.OriginalTextAnnotation.class)));
                        } else {
                            String before = StringUtils.getNotNullString(token.get(CoreAnnotations.BeforeAnnotation.class));
                            String currentText = StringUtils.getNotNullString(token.get(CoreAnnotations.OriginalTextAnnotation.class));
                            sb.get().append(before);
                            sb.get().append(currentText);
                        }
                        return currentNe;
                    }).forEach((currentNe) -> {
                prevNe.set(currentNe);
            });

            if (sb.get().length() > 0) {
                Tag tag = new Tag(sb.get().toString());
                tag.setNe(prevNe.get());
                newSentence.addTag(tag);
            }
            result.addSentence(newSentence);

        });
        return result;
    }

    public Tag annotateTag(String text) {
        Annotation document = new Annotation(text);
        pipeline.annotate(document);
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
        System.out.println("POS: " + pos + " ne: " + ne + " lemma: " + lemma);
        return tag;
    }

    public boolean checkPuntuation(String value) {
        Matcher match = patternCheck.matcher(value);
        return !match.find();
    }

}
