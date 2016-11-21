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
package com.graphaware.nlp.conceptnet5;

import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.language.LanguageManager;
import com.graphaware.nlp.processor.TextProcessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConceptNet5Importer {

    private static final Logger LOG = LoggerFactory.getLogger(ConceptNet5Importer.class);

    public static final String[] DEFAULT_ADMITTED_RELATIONSHIP = {"RelatedTo", "IsA", "PartOf", "AtLocation", "Synonym", "MemberOf", "HasA", "CausesDesire"};
    public static final String DEFAULT_LANGUAGE = "en";

    private final ConceptNet5Client client;
    private final TextProcessor nlpProcessor;
    private int depthSearch = 2;

    public ConceptNet5Importer(String conceptNet5EndPoint, TextProcessor nlpProcessor, int depth, String... admittedRelations) {
        this(new ConceptNet5Client(conceptNet5EndPoint), nlpProcessor, depth, admittedRelations);
    }

    public ConceptNet5Importer(ConceptNet5Client client, TextProcessor nlpProcessor, int depth, String... admittedRelations) {
        this.client = client;
        this.nlpProcessor = nlpProcessor;
        this.depthSearch = depth;
    }

    private ConceptNet5Importer(Builder builder) {
        this.client = builder.client;
        this.nlpProcessor = builder.nlpProcessor;
        this.depthSearch = builder.depthSearch;
    }

    public List<Tag> importHierarchy(Tag source, String lang, int depth) {
        return importHierarchy(source, lang, depth, DEFAULT_ADMITTED_RELATIONSHIP);
    }

    public List<Tag> importHierarchy(Tag source, String lang) {
        return importHierarchy(source, lang, depthSearch, DEFAULT_ADMITTED_RELATIONSHIP);
    }

    public List<Tag> importHierarchy(Tag source, String lang, int depth, String... admittedRelations) {
        return importHierarchy(source, lang, depth, Arrays.asList(admittedRelations));
    }

    public List<Tag> importHierarchy(Tag source, String lang, int depth, List<String> admittedRelations) {
        List<Tag> res = new ArrayList<>();
        String word = source.getLemma().toLowerCase().replace(" ", "_");
        try {
            ConceptNet5EdgeResult values = client.getValues(word, lang);
            values.getEdges().stream().forEach((concept) -> {
                if (checkAdmittedRelations(concept, admittedRelations)
                        && (concept.getStart().equalsIgnoreCase(word)
                        || concept.getEnd().equalsIgnoreCase(word))) {
                    if (concept.getStart().equalsIgnoreCase(word)) {
                        Tag annotateTag = tryToAnnotate(concept.getEnd(), concept.getEndLanguage());
                        if (depth > 1) {
                            importHierarchy(annotateTag, lang, depth - 1, admittedRelations);
                        }
                        source.addParent(concept.getRel(), annotateTag, concept.getWeight());
                        res.add(annotateTag);
                    } else {
                        Tag annotateTag = tryToAnnotate(concept.getStart(), concept.getStartLanguage());
                        //TODO evaluate if also in this case could be useful go in deep
//                        if (depth > 1) {
//                            importHierarchy(annotateTag, lang, depth - 1, admittedRelations);
//                        }
                        annotateTag.addParent(concept.getRel(), source, concept.getWeight());
                        res.add(annotateTag);
                    }
                }
            });
        } catch (Exception ex) {
            LOG.error("Error while improting hierarchy for " + word + "(" + lang + "). Ignored!", ex);
        }
        return res;
    }

    private Tag tryToAnnotate(String parentConcept, String language) {
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
        private final TextProcessor nlpProcessor;
        private int depthSearch = 2;

        public Builder(String cnet5Host, TextProcessor nlpProcessor) {
            this(new ConceptNet5Client(cnet5Host), nlpProcessor);
        }

        public Builder(ConceptNet5Client client, TextProcessor nlpProcessor) {
            this.client = client;
            this.nlpProcessor = nlpProcessor;
        }

        public Builder setDepthSearch(int depthSearch) {
            this.depthSearch = depthSearch;
            return this;
        }

        public ConceptNet5Importer build() {
            return new ConceptNet5Importer(this);
        }
    }
}
