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
package com.graphaware.nlp.enrich.conceptnet5;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.language.LanguageManager;
import com.graphaware.nlp.processor.TextProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static com.graphaware.nlp.util.TextUtils.removeApices;
import static com.graphaware.nlp.util.TextUtils.removeParenthesis;


public class ConceptNet5Importer {

    private static final Logger LOG = LoggerFactory.getLogger(ConceptNet5Importer.class);

    public static final String[] DEFAULT_ADMITTED_RELATIONSHIP = {"RelatedTo", "IsA", "PartOf", "AtLocation", "Synonym", "MemberOf", "HasA", "CausesDesire"};
    public static final String DEFAULT_LANGUAGE = "en";

    private final ConceptNet5Client client;
    private int depthSearch = 2;

    private final Cache<String, Tag> cache = CacheBuilder
            .newBuilder()
            .maximumSize(10000)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    public ConceptNet5Importer(String conceptNet5EndPoint, int depth, String... admittedRelations) {
        this(new ConceptNet5Client(conceptNet5EndPoint), depth, admittedRelations);
    }

    public ConceptNet5Importer(ConceptNet5Client client, int depth, String... admittedRelations) {
        this.client = client;
        this.depthSearch = depth;
    }

    private ConceptNet5Importer(Builder builder) {
        this.client = builder.client;
        this.depthSearch = builder.depthSearch;
    }

//    public List<Tag> importHierarchy(Tag source, String lang, boolean filterLang, int depth, TextProcessor nlpProcessor) {
//        return importHierarchy(source, lang, filterLang, depth, nlpProcessor, DEFAULT_ADMITTED_RELATIONSHIP);
//    }
//
//    public List<Tag> importHierarchy(Tag source, String lang, boolean filterLang, TextProcessor nlpProcessor) {
//        return importHierarchy(source, lang, filterLang, depthSearch, nlpProcessor, DEFAULT_ADMITTED_RELATIONSHIP);
//    }
//
//    public List<Tag> importHierarchy(Tag source, String lang, boolean filterLang, int depth, TextProcessor nlpProcessor, String... admittedRelations) {
//        return importHierarchy(source, lang, filterLang, depth, nlpProcessor, Arrays.asList(admittedRelations));
//    }
    public List<Tag> importHierarchy(Tag source, String lang, boolean filterLang, int depth, TextProcessor nlpProcessor, List<String> admittedRelations, List<String> admittedPOS, int limit) {
        if (null == admittedRelations || admittedRelations.isEmpty()) {
            throw new RuntimeException("Admitted Relationships is empty");
        }
        List<Tag> res = new CopyOnWriteArrayList<>();
        String word = source.getLemma().toLowerCase().replace(" ", "_");
        word = removeParenthesis(word);
        word = removeApices(word);
        final String finalWord = word;
        try {
            admittedRelations.forEach(rel -> {
                ConceptNet5EdgeResult values;
                values = client.queryByStart(finalWord, rel, lang, limit);
                values.getEdges().stream().forEach((concept) -> {
                    if (checkAdmittedRelations(concept, admittedRelations)
                            && (concept.getStart().equalsIgnoreCase(source.getLemma()) || concept.getEnd().equalsIgnoreCase(source.getLemma()))
                            && (!filterLang || (filterLang && concept.getEndLanguage().equalsIgnoreCase(lang) && concept.getStartLanguage().equalsIgnoreCase(lang)))) {

                        if (concept.getStart().equalsIgnoreCase(source.getLemma()) &&
                                !concept.getStart().equalsIgnoreCase(concept.getEnd())) {
                            String value = concept.getEnd();
                            value = removeApices(value);
                            value = removeParenthesis(value);
                            Tag annotateTag = tryToAnnotate(value, concept.getEndLanguage(), nlpProcessor);
                            List<String> posList = annotateTag.getPosAsList();
                            if (admittedPOS == null
                                    || admittedPOS.isEmpty()
                                    || posList == null
                                    || posList.isEmpty()
                                    || posList.stream().filter((pos) -> (admittedPOS.contains(pos))).count() > 0) {
                                if (depth > 1) {
                                    importHierarchy(annotateTag, lang, filterLang, depth - 1, nlpProcessor, admittedRelations, admittedPOS, limit);
                                }
                                source.addParent(concept.getRel(), annotateTag, concept.getWeight(), ConceptNet5Enricher.ENRICHER_NAME);
                                res.add(annotateTag);
                            }
                        } else {
                            Tag annotateTag = tryToAnnotate(concept.getStart(), concept.getStartLanguage(), nlpProcessor);
                            annotateTag.addParent(concept.getRel(), source, concept.getWeight(), ConceptNet5Enricher.ENRICHER_NAME);
                            res.add(annotateTag);
                        }
                    }
                });
            });

        } catch (Exception ex) {
            LOG.error("Error while improting hierarchy for " + word + " (" + lang + "). Ignored!", ex);
        }
        return res;
    }

//    private synchronized Tag tryToAnnotate(final String parentConcept, final String language) {
//        Tag value;
//        String id = parentConcept + "_" + language;
//        try {
//            value = cache.get(id, new Callable<Tag>() {
//                @Override
//                public Tag call() throws Exception {
//                    return tryToAnnotateAux(parentConcept, language);
//                }
//            });
//        } catch (Exception ex) {
//            LOG.error("Error while try to annotate concept " + parentConcept + " lang " + language, ex);
//            throw new RuntimeException("Error while try to annotate concept " + parentConcept + " lang " + language);
//        }
//        return value;
//    }

    private Tag tryToAnnotate(String parentConcept, String language, TextProcessor nlpProcessor) {
        Tag annotateTag = null;
        if (LanguageManager.getInstance().isLanguageSupported(language)) {
            annotateTag = nlpProcessor.annotateTag(parentConcept, language);
        }
        if (annotateTag == null) {
            annotateTag = new Tag(parentConcept, language);
        }
        return annotateTag;
    }

    private boolean checkAdmittedRelations(ConceptNet5Concept concept, List<String> admittedRelations) {
        if (admittedRelations == null) {
            return true;
        }
        return admittedRelations.stream().anyMatch((rel) -> (concept.getRel().contains(rel)));
    }

    public static class Builder {

        private final ConceptNet5Client client;
        private int depthSearch = 2;

        public Builder(String cnet5Host) {
            this(new ConceptNet5Client(cnet5Host));
        }

        public Builder(ConceptNet5Client client) {
            this.client = client;
        }

        public Builder setDepthSearch(int depthSearch) {
            this.depthSearch = depthSearch;
            return this;
        }

        public ConceptNet5Importer build() {
            return new ConceptNet5Importer(this);
        }
    }

    public ConceptNet5Client getClient() {
        return client;
    }
}
