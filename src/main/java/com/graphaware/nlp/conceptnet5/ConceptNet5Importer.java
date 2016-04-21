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
import com.graphaware.nlp.processor.TextProcessor;
import java.util.ArrayList;
import java.util.List;

public class ConceptNet5Importer {

    private ConceptNet5Client client;
    private TextProcessor nlpProcessor;

    public ConceptNet5Importer() {
    }

    public List<Tag> importConceptHierarchy(Tag source, String lang, int depth, String... admittedRelations) //filter
    {
        String word = source.getLemma().toLowerCase();
        ConceptNet5EdgeResult values = client.getValues(word, lang);
        List<Tag> res = new ArrayList<>();
        for (ConceptNet5Concept concept : values.getEdges()) {
            final String conceptPrefix = "/c/" + lang + "/";
            final String parentConcept = concept.getEnd().substring(conceptPrefix.length());
            if (!concept.getStart().equalsIgnoreCase(conceptPrefix + word)
                || !checkAdmittedRelations(concept, admittedRelations)) {
                continue;
            }
            //Try to annotate tag
            Tag annotateTag = nlpProcessor.annotateTag(parentConcept);

            if (annotateTag == null) {
                annotateTag = new Tag(parentConcept);
            }

            if (depth > 1) {
                importConceptHierarchy(annotateTag, lang, depth - 1, admittedRelations);
            }

            source.addParent(concept.getRel(), annotateTag);
            res.add(annotateTag);
        }
        return res;
    }

    private boolean checkAdmittedRelations(ConceptNet5Concept concept, String... admittedRelations) {
        if (admittedRelations == null) {
            return true;
        }
        for (String rel : admittedRelations) {
            if (concept.getRel().contains(rel)) {
                return true;
            }
        }
        return false;
    }

}
