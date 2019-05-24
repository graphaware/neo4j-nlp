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

import com.graphaware.common.log.LoggerFactory;
import com.graphaware.common.util.Pair;
import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.dsl.request.ConceptRequest;
import com.graphaware.nlp.dsl.request.PipelineSpecification;
import com.graphaware.nlp.enrich.AbstractEnricher;
import com.graphaware.nlp.enrich.Enricher;
import com.graphaware.nlp.persistence.PersistenceRegistry;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.processor.TextProcessorsManager;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;

import java.util.*;

public class ConceptNet5Enricher extends AbstractEnricher implements Enricher {

    private static final Log LOG = LoggerFactory.getLogger(ConceptNet5Enricher.class);
    private static final String DEFAULT_CONCEPTNET_URL = "http://api.conceptnet.io";
    private static final String CONFIG_KEY_URL = "CONCEPT_NET_5_URL";

    private static final String RELATIONSHIP_IS_RELATED_TO_SUB_TAG = "subTag";

    public static final String ENRICHER_NAME = "CONCEPT_NET_5";

    private static final String ALIAS_NAME = "conceptnet5";

    private final TextProcessorsManager textProcessorsManager;

    private ConceptNet5Importer conceptnet5Importer;

    public ConceptNet5Enricher(
            GraphDatabaseService database,
            PersistenceRegistry persistenceRegistry,
            TextProcessorsManager textProcessorsManager) {
        super(database, persistenceRegistry);
        this.textProcessorsManager = textProcessorsManager;
    }

    @Override
    public String getName() {
        return ENRICHER_NAME;
    }

    @Override
    public String getAlias() {
        return ALIAS_NAME;
    }

    public Node importConcept(ConceptRequest request) {
        List<Tag> conceptTags = new ArrayList<>();
        Node annotatedNode = request.getAnnotatedNode();
        Pair<Iterator<Node>, Node> pair = getTagsIteratorFromRequest(request);
        Iterator<Node> tagsIterator = pair.first();
        Node tagToBeAnnotated = pair.second();
        int depth = request.getDepth();
        Boolean splitTags = request.isSplitTag();
        Boolean filterByLang = request.isFilterByLanguage();
        List<String> admittedRelationships = request.getAdmittedRelationships();
        List<String> admittedPos = request.getAdmittedPos();
        List<String> outputLanguages = request.getOutputLanguages();

        if (outputLanguages != null) {
            outputLanguages.replaceAll(String::toLowerCase);
        }
        RelDirection relDirection = RelDirection.getRelDirection(request.getRelDirection());
        List<Tag> tags = new ArrayList<>();
        while (tagsIterator.hasNext()) {
            Tag tag = (Tag) getPersister(Tag.class).fromNode(tagsIterator.next());
            if (splitTags) {
                List<Tag> annotateTags = NLPManager.getInstance().getTextProcessorsManager().annotateTags(tag.getLemma(), tag.getLanguage());
                if (annotateTags.size() == 1
                        && annotateTags.get(0).getLemma().equalsIgnoreCase(tag.getLemma())) {
                    tags.add(tag);
                } else {
                    annotateTags.forEach((newTag) -> {
                        tags.add(newTag);
                        tag.addParent(RELATIONSHIP_IS_RELATED_TO_SUB_TAG, newTag, 0.0f);
                    });
                    //conceptTags.add(tag);
                }
            } else {
                tags.add(tag);
            }
        }

        tags.stream().forEach((tag) -> {

            List<RelDirection> relDirections;
            if (relDirection == RelDirection.BOTH) {
                relDirections = Arrays.asList(RelDirection.IN, RelDirection.OUT);
            } else {
                relDirections = Arrays.asList(relDirection);
            }
            relDirections.stream().forEach(direction -> {
                conceptTags.addAll(getImporter().importHierarchy(
                        tag,
                        direction,
                        filterByLang,
                        outputLanguages,
                        depth,
                        admittedRelationships,
                        admittedPos,
                        request.getResultsLimit(),
                        request.getMinWeight()));
            });
            conceptTags.add(tag);
        });

        conceptTags.stream().forEach((newTag) -> {
            if (newTag != null) {
                getPersister(Tag.class).getOrCreate(newTag, newTag.getId(), String.valueOf(System.currentTimeMillis()));
            }
        });
        if (annotatedNode != null) {
            return annotatedNode;
        } else {
//                        Set<Object[]> result = new HashSet<>();
//                        conceptTags.stream().forEach((item) -> {
//                            result.add(new Object[]{item});
//                        });
//                        return Iterators.asRawIterator(result.iterator());
            return tagToBeAnnotated;
        }

    }

    private ConceptNet5Importer getImporter() {
        if (conceptnet5Importer == null || importerShouldBeReloaded()) {
            String url = getConceptNetUrl();
            this.conceptnet5Importer = new ConceptNet5Importer.Builder(url).build();
        }
        return conceptnet5Importer;
    }

    public String getConceptNetUrl() {
        String urlFromConfigOrDefault = (String)getConfiguration().getSettingValueFor(CONFIG_KEY_URL);

        return urlFromConfigOrDefault == null
                ? DEFAULT_CONCEPTNET_URL
                : urlFromConfigOrDefault;
    }

    public ConceptNet5Importer getConceptnet5Importer() {
        return conceptnet5Importer;
    }

    private boolean importerShouldBeReloaded() {
        return !conceptnet5Importer.getClient().getConceptNet5EndPoint().equals(getConceptNetUrl());
    }

    public enum RelDirection {
        IN("end"),
        OUT("start"),
        BOTH("both");

        private String value;

        RelDirection(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static RelDirection getRelDirection(String direction) {
            if (direction == null || direction.isEmpty()) {
                throw new RuntimeException("Direction is empty or null");
            }
            switch (direction.toLowerCase()) {
                case "in":
                    return IN;
                case "out":
                    return OUT;
                case "both":
                    return BOTH;
                default:
                    throw new RuntimeException("Relationship direction "
                            + direction
                            + " not supported. Please use one of these: in, out, both.");
            }
        }
    }

}
