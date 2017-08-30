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
package com.graphaware.nlp.domain;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AnnotatedText {

    private static final Logger LOG = LoggerFactory.getLogger(AnnotatedText.class);
    private static final long serialVersionUID = -1L;

    private List<Sentence> sentences = new ArrayList<>();

    public List<Sentence> getSentences() {
        return sentences;
    }

    public void addSentence(Sentence sentence) {
        sentences.add(sentence);
    }

    public List<String> getTokens() {
        List<String> result = new ArrayList<>();
        sentences.forEach((sentence) -> {
            sentence.getTags().forEach((tag) -> {
                result.add(tag.getLemma());
            });
        });
        return result;
    }

    public List<Tag> getTags() {
        List<Tag> result = new ArrayList<>();
        sentences.forEach((sentence) -> {
            sentence.getTags().forEach((tag) -> {
                result.add(tag);
            });
        });
        return result;
    }

    public boolean filter(String filterQuery) {
        Map<String, FilterQueryTerm> filterQueryTerms = getFilterQueryTerms(filterQuery);
        List<Tag> tags = getTags();
        for (Tag tag : tags) {
            FilterQueryTerm query = filterQueryTerms.get(tag.getLemma().toLowerCase());
            if (query != null && query.evaluate(tag)) {
                return true;
            }
        }

        return false;
    }

    //Query example "Nice/Location, attack"
    private Map<String, FilterQueryTerm> getFilterQueryTerms(String query) {
        Map<String, FilterQueryTerm> result = new HashMap<>();
        if (query != null) {
            String[] terms = query.split(",");
            for (String term : terms) {
                FilterQueryTerm filterQueryTerm = new FilterQueryTerm(term);
                result.put(filterQueryTerm.getValue().toLowerCase(), filterQueryTerm);
            }
        }

        return result;
    }

    public List<Sentence> getSentencesSorted() {
        sentences.sort((Sentence o1, Sentence o2) -> o1.compareTo(o2));

        return sentences;
    }

    private class FilterQueryTerm {

        private final String value;
        private final String NE;

        FilterQueryTerm(String query) {
            String[] parts = query.split("/");
            this.value = parts[0];
            this.NE = parts.length > 1 ? parts[1] : null;

        }

        public String getValue() {
            return value;
        }

        private boolean evaluate(Tag tag) {
            if (NE != null) {
                return tag.getNeAsList().contains(NE) && tag.getLemma().equalsIgnoreCase(value);
            } else {
                return tag.getLemma().equalsIgnoreCase(value);
            }
        }

    }
//
//    private void writeObject(ObjectOutputStream s) throws IOException {
//        s.defaultWriteObject();
//        s.writeInt(sentences.size());
//        for (Sentence sentence : sentences)
//            s.writeObject(sentence);
//    }
//
//    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
//        s.defaultReadObject();
//        int sentencesSize = s.readInt();
//        this.sentences = new HashSet<>();
//        for (int i = 0; i < sentencesSize; i++) {
//            addSentence((Sentence)s.readObject());
//        }
//    }

//    public static AnnotatedText load(Node node) {
//        Object id = node.getProperty(Properties.PROPERTY_ID);
//        AnnotatedText result = new AnnotatedText();
//        result.node = node;
//        Iterable<Relationship> relationships = node.getRelationships(CONTAINS_SENTENCE);
//        for (Relationship rel : relationships) {
//            Node sentenceNode = rel.getOtherNode(node);
//            Sentence sentence = Sentence.load(sentenceNode);
//            result.addSentence(sentence);
//        }
//        return result;
//    }
//
//    private Node checkIfExist(GraphDatabaseService database, Object id) {
//        if (id != null) {
//            ResourceIterator<Node> findNodes = database.findNodes(Labels.AnnotatedText, Properties.PROPERTY_ID, id);
//            if (findNodes.hasNext()) {
//                return findNodes.next();
//            }
//        }
//        return null;
//    }


//    @Override
//    public Node storeOnGraph(GraphDatabaseService database, boolean force) {
//        LOG.info("Start storing annotatedText " + id);
//        Node tmpAnnotatedNode = checkIfExist(database, id);
//        if (tmpAnnotatedNode == null || force) {
//            final Node annotatedTextNode;
//            if ( tmpAnnotatedNode != null)
//                annotatedTextNode = tmpAnnotatedNode;
//            else
//                annotatedTextNode = database.createNode(AnnotatedText);
//            annotatedTextNode.setProperty(Properties.PROPERTY_ID, id);
//            annotatedTextNode.setProperty(Properties.NUM_TERMS, getTokens().size());
//            final AtomicReference<Node> previousSentenceReference = new AtomicReference<>();
//            sentences.sort((Sentence o1, Sentence o2) -> o1.compareTo(o2));
//
//            sentences.stream().forEach((sentence) -> {
//                Node sentenceNode = sentence.storeOnGraph(database, force);
//                annotatedTextNode.createRelationshipTo(sentenceNode, CONTAINS_SENTENCE);
//                Node previousSentence = previousSentenceReference.get();
//                if (previousSentence == null) {
//                    annotatedTextNode.createRelationshipTo(sentenceNode, FIRST_SENTENCE);
//                } else {
//                    previousSentence.createRelationshipTo(sentenceNode, NEXT_SENTENCE);
//                }
//                previousSentenceReference.set(sentenceNode);
//                List<Phrase> phraseOccurrences = sentence.getPhraseOccurrence();
//                phraseOccurrences.stream().forEach((phrase) -> {
//                    if (phrase.getReference() != null) {
//                        Node phraseNode = phrase.getOrCreate(database, force);
//                        Node referredPhraseNode = phrase.getReference().getOrCreate(database, force);
//                        phraseNode.createRelationshipTo(referredPhraseNode, REFER_TO);
//                    }
//                });
//            });
//            tmpAnnotatedNode = annotatedTextNode;
//        } else {
//            /*
//            * Currently only labels could change so if the AnnotatedText already exist
//            * only the Sentence are updated
//             */
//            sentences.stream().forEach((sentence) -> {
//                sentence.storeOnGraph(database, force);
//            });
//        }
//        node = tmpAnnotatedNode;
//        LOG.info("end storing annotatedText " + id);
//        return tmpAnnotatedNode;
//    }

}
