package com.graphaware.nlp.util;

import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.domain.Sentence;
import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.domain.TagOccurrence;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class TestAnnotatedText {

    private final AnnotatedText annotatedText;

    public TestAnnotatedText(AnnotatedText annotatedText) {
        this.annotatedText = annotatedText;
    }

    public void assertSentencesCount(int count) {
        assertEquals(count, annotatedText.getSentences().size());
    }

    public void assertSentenceWithText(String text) {
        boolean found = false;
        for (Sentence sentence : annotatedText.getSentences()) {
            if (sentence.getSentence().equals(text)) {
                found = true;
            }
        }

        assertTrue(found);
    }

    public void assertTagsCountInSentence(int count, int sentenceNumber) {
        Sentence sentence = annotatedText.getSentencesSorted().get(sentenceNumber);

        assertEquals(count, sentence.getTags().size());
    }

    public void assertTagWithLemma(String value) {
        boolean found = false;
        for (Sentence sentence : annotatedText.getSentences()) {
            for (Tag tag : sentence.getTags()) {
                if (tag.getLemma().equals(value)) {
                    found = true;
                    break;
                }
            }
        }

        assertTrue(found);
    }

    public void assertNotTag(Tag tag) {
        try {
            assertTag(tag);
            assertFalse(true);
        } catch (AssertionError e) {
            assertTrue(true);
        }
    }

    public void assertTag(Tag tag) {
        boolean found = false;
        mainloop:
        for (Sentence sentence : annotatedText.getSentences()) {
            for (Tag t : sentence.getTags()) {
                if (checkTagEquality(t, tag)) {
                    found = true;
                    break mainloop;
                }
            }
        }

        assertTrue(found);
    }

    public Tag getTagAtPosition(int sentenceNumber, int begin) {
        if (begin < 0) {
            throw new RuntimeException("Begin cannot be negative");
        }
        List<TagOccurrence> occurrences = annotatedText.getSentencesSorted().get(sentenceNumber).getTagOccurrences().get(begin);
        if (occurrences != null) {
            // @TODO: take into account that more than one PartOfTextOccurrence is possible
            return occurrences.get(0).getElement();
        } else {
            return null;
        }
    }
    
    public TagOccurrence getTagOccurrenceAtPosition(int sentenceNumber, int begin) {
        if (begin < 0) {
            throw new RuntimeException("Begin cannot be negative");
        }
        List<TagOccurrence> occurrences = annotatedText.getSentencesSorted().get(sentenceNumber).getTagOccurrences().get(begin);
        if (occurrences != null) {
            // @TODO: take into account that more than one PartOfTextOccurrence is possible
            return occurrences.get(0);
        } else {
            return null;
        }
    }

    public boolean checkTagEquality(Tag a, Tag b) {
        if (!a.getLemma().equals(b.getLemma())) {
            return false;
        }

        final Set<String> s1 = new HashSet<>(a.getNeAsList());
        final Set<String> s2 = new HashSet<>(b.getNeAsList());

        if (!s1.containsAll(s2)) {
            return false;
        }

        final Set<String> p1 = new HashSet<>(a.getPosAsList());
        final Set<String> p2 = new HashSet<>(b.getPosAsList());

        if (!p1.containsAll(p2)) {
            return false;
        }

        return true;
    }

}
