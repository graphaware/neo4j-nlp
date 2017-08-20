package com.graphaware.nlp.util.unit;

import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.domain.Sentence;
import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.util.TestAnnotatedText;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.graphaware.nlp.util.TagUtils.newTag;
import static java.util.Collections.singletonList;
import static java.util.Collections.emptyList;

public class TestAnnotatedTesterUnitTest {

    @Test
    public void testSentencesCountIsCorrect() {
        AnnotatedText annotatedText = new AnnotatedText();
        TestAnnotatedText test = new TestAnnotatedText(annotatedText);
        Sentence sentence = new Sentence("hello");
        sentence.addTag(new Tag("hello", "en"));
        annotatedText.addSentence(sentence);
        test.assertSentencesCount(1);
    }

    @Test(expected = AssertionError.class)
    public void testSentenceCountThrowsExceptionWhenNotCorrect() {
        AnnotatedText annotatedText = new AnnotatedText();
        TestAnnotatedText tester = new TestAnnotatedText(annotatedText);
        tester.assertSentencesCount(1);
    }

    @Test
    public void testTagIsFound() {
        AnnotatedText annotatedText = new AnnotatedText();
        TestAnnotatedText test = new TestAnnotatedText(annotatedText);
        Sentence sentence = new Sentence("hello");
        sentence.addTag(new Tag("hello", "en"));
        annotatedText.addSentence(sentence);
        test.assertSentencesCount(1);
        test.assertTagWithLemma("hello");
    }

    @Test(expected = AssertionError.class)
    public void testTagIsNotFound() {
        AnnotatedText annotatedText = new AnnotatedText();
        TestAnnotatedText test = new TestAnnotatedText(annotatedText);
        Sentence sentence = new Sentence("hello it is me");
        sentence.addTag(new Tag("hello", "en"));
        annotatedText.addSentence(sentence);
        test.assertTagWithLemma("hella");
    }

    @Test
    public void testSentenceWithText() {
        AnnotatedText annotatedText = new AnnotatedText();
        TestAnnotatedText test = new TestAnnotatedText(annotatedText);
        Sentence sentence = new Sentence("hello it is me");
        sentence.addTag(new Tag("hello", "en"));
        annotatedText.addSentence(sentence);
        test.assertSentenceWithText("hello it is me");
    }

    @Test(expected = AssertionError.class)
    public void testSentenceWithNoText() {
        AnnotatedText annotatedText = new AnnotatedText();
        TestAnnotatedText test = new TestAnnotatedText(annotatedText);
        Sentence sentence = new Sentence("hello it is me");
        sentence.addTag(new Tag("hello", "en"));
        annotatedText.addSentence(sentence);
        test.assertSentenceWithText("hello it is not me");
    }

    @Test
    public void assertTagEquals() {
        AnnotatedText annotatedText = new AnnotatedText();
        TestAnnotatedText test = new TestAnnotatedText(annotatedText);
        Sentence sentence = new Sentence("John is working in Denver for IBM");
        sentence.addTag(newTag("John", singletonList("PERSON"), emptyList()));
        sentence.addTag(newTag("Denver", singletonList("LOCATION"), emptyList()));
        sentence.addTag(newTag("IBM", singletonList("ORGANIZATION"), emptyList()));
        sentence.addTag(newTag("work"));
        annotatedText.addSentence(sentence);
        test.assertTag(newTag("John", singletonList("PERSON"), emptyList()));
        test.assertTag(newTag("IBM", singletonList("ORGANIZATION"), emptyList()));
        test.assertTag(newTag("work"));
    }

    @Test(expected = AssertionError.class)
    public void assertTagNotEquals() {
        AnnotatedText annotatedText = new AnnotatedText();
        TestAnnotatedText test = new TestAnnotatedText(annotatedText);
        Sentence sentence = new Sentence("John is working in Denver for IBM");
        sentence.addTag(newTag("John", singletonList("PERSON"), emptyList()));
        sentence.addTag(newTag("Denver", singletonList("LOCATION"), emptyList()));
        sentence.addTag(newTag("IBM", singletonList("ORGANIZATION"), emptyList()));
        sentence.addTag(newTag("work"));
        annotatedText.addSentence(sentence);
        test.assertTag(newTag("John"));
    }
}
