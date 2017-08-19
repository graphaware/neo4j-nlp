package com.graphaware.nlp.processor;

import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.domain.Sentence;
import com.graphaware.nlp.domain.Tag;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class AnnotatedTextTest {

    private static final String SHORT_TEXT_1 = "You knew China's cities were growing. But the real numbers are stunning http://wef.ch/29IxY7w  #China";

    @Test
    public void testFilter() {
        AnnotatedText annotatedText = new AnnotatedText();
        Sentence sentence = new Sentence(SHORT_TEXT_1, 0);
        sentence.addTag(getTag("BBC", null));
        sentence.addTag(getTag("China", "LOCATION"));
        annotatedText.addSentence(sentence);

        assertTrue(annotatedText.filter("BBC"));
        assertTrue(annotatedText.filter("China/LOCATION"));
    }

    @Test
    public void testComplexFilter() {
        AnnotatedText annotatedText = new AnnotatedText();
        Sentence sentence = new Sentence("Owen Bennet Jones is working for the BBC in Pakistan", 0);
        sentence.addTag(getTag("Owen Bennet Jones", "PERSON"));
        sentence.addTag(getTag("work", null));
        sentence.addTag(getTag("the", null));
        sentence.addTag(getTag("BBC", null));
        sentence.addTag(getTag("Pakistan", "LOCATION"));
        annotatedText.addSentence(sentence);

        assertTrue(annotatedText.filter("Owen Bennet Jones/PERSON,BBC,Pakistan/LOCATION"));

    }

    private Tag getTag(String lemma, String namedEntity) {
        Tag tag = new Tag(lemma, "en");
        if (namedEntity != null) {
            tag.setNe(Arrays.asList(namedEntity));
        }

        return tag;
    }

}
