/*
 * Copyright (c) 2013-2018 GraphAware
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
import com.graphaware.nlp.enrich.AbstractImporter;
import org.neo4j.logging.Log;
import com.graphaware.common.log.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static com.graphaware.nlp.util.TextUtils.removeApices;
import static com.graphaware.nlp.util.TextUtils.removeParenthesis;


public class ConceptNet5Importer extends AbstractImporter {

    private static final Log LOG = LoggerFactory.getLogger(ConceptNet5Importer.class);

    public static final String[] DEFAULT_ADMITTED_RELATIONSHIP = {"RelatedTo", "IsA", "PartOf", "AtLocation", "Synonym", "MemberOf", "HasA", "CausesDesire"};

    private final ConceptNet5Client client;

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
    }

    private ConceptNet5Importer(Builder builder) {
        this.client = builder.client;
    }

    public List<Tag> importHierarchy(Tag source,
                                     ConceptNet5Enricher.RelDirection relDirection,
                                     boolean filterLang,
                                     List<String> outLang,
                                     int depth,
                                     List<String> admittedRelations,
                                     List<String> admittedPOS,
                                     int limit,
                                     double minWeight) {
        if (null == admittedRelations || admittedRelations.isEmpty()) {
            throw new RuntimeException("Admitted Relationships is empty");
        }
        List<Tag> res = new CopyOnWriteArrayList<>();
        final String startingWord = getCleanedLemma(source);
        try {
            admittedRelations.forEach(rel -> {
                ConceptNet5EdgeResult values = client.queryBy(relDirection.getValue(), startingWord, rel, source.getLanguage(), limit);
                values.getEdges().stream().forEach((concept) -> {
                    List<Tag> result = processConcept(source,
                            relDirection,
                            filterLang,
                            outLang,
                            depth,
                            admittedRelations,
                            admittedPOS,
                            limit,
                            minWeight,
                            concept);
                    res.addAll(result);
                });
            });
        } catch (Exception ex) {
            LOG.warn("Error while importing hierarchy for " + startingWord + " (" + source.getLanguage() + "). Ignored!", ex);
        }
        return res;
    }

    private List<Tag> processConcept(Tag source, ConceptNet5Enricher.RelDirection relDirection, boolean filterLang, List<String> outLang, int depth, List<String> admittedRelations, List<String> admittedPOS, int limit, double minWeight, ConceptNet5Concept concept) {
        List<Tag> res = new ArrayList<>();

        String conceptValue;
        String conceptLanguage;
        if (relDirection == ConceptNet5Enricher.RelDirection.OUT) {
            conceptValue = concept.getEnd();
            conceptLanguage = concept.getEndLanguage();
        } else {
            conceptValue = concept.getStart();
            conceptLanguage = concept.getStartLanguage();
        }
        if (checkAdmittedRelations(concept, admittedRelations)
                && concept.getWeight() > minWeight
                && checkLanguages(filterLang, source.getLanguage(), conceptLanguage, outLang)) {

            if (!concept.getStart().equalsIgnoreCase(concept.getEnd())) {
                conceptValue = removeApices(conceptValue);
                conceptValue = removeParenthesis(conceptValue);
                Tag annotateTag = tryToAnnotate(conceptValue, conceptLanguage);
                List<String> posList = annotateTag.getPos();
                if (admittedPOS == null
                        || admittedPOS.isEmpty()
                        || posList == null
                        || posList.isEmpty()
                        || posList.stream().filter((pos) -> (admittedPOS.contains(pos))).count() > 0) {
                    if (depth > 1) {
                        importHierarchy(annotateTag, relDirection, filterLang, outLang, depth - 1, admittedRelations, admittedPOS, limit, minWeight);
                    }
                    source.addParent(concept.getRel(), annotateTag, concept.getWeight(), ConceptNet5Enricher.ENRICHER_NAME);
                    res.add(annotateTag);
                }
            }
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

    private boolean checkAdmittedRelations(ConceptNet5Concept concept, List<String> admittedRelations) {
        if (admittedRelations == null) {
            return true;
        }
        return admittedRelations.stream().anyMatch((rel) -> (concept.getRel().contains(rel)));
    }

    public ConceptNet5Client getClient() {
        return client;
    }

    public static class Builder {

        private final ConceptNet5Client client;

        public Builder(String cnet5Host) {
            this(new ConceptNet5Client(cnet5Host));
        }

        public Builder(ConceptNet5Client client) {
            this.client = client;
        }

        public ConceptNet5Importer build() {
            return new ConceptNet5Importer(this);
        }
    }


}
