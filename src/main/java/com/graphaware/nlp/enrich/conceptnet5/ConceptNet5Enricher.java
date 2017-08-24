package com.graphaware.nlp.enrich.conceptnet5;

import com.graphaware.common.log.LoggerFactory;
import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.dsl.ConceptRequest;
import com.graphaware.nlp.enrich.AbstractEnricher;
import com.graphaware.nlp.enrich.Enricher;
import com.graphaware.nlp.persistence.PersistenceRegistry;
import com.graphaware.nlp.persistence.constants.Labels;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.processor.TextProcessorsManager;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;

import java.util.*;

public class ConceptNet5Enricher extends AbstractEnricher implements Enricher {

    private static final Log LOG = LoggerFactory.getLogger(ConceptNet5Enricher.class);
    private static final String DEFAULT_CONCEPTNET_URL = "http://api.conceptnet.io";

    private static final String RELATIONSHIP_IS_RELATED_TO_SUB_TAG = "subTag";

    public static final String ENRICHER_NAME = "CONCEPT_NET_5";

    private final TextProcessorsManager textProcessorsManager;

    private ConceptNet5Importer conceptnet5Importer;

    public ConceptNet5Enricher(
            GraphDatabaseService database,
            PersistenceRegistry persistenceRegistry,
            DynamicConfiguration configuration,
            TextProcessorsManager textProcessorsManager) {
        super(database, persistenceRegistry, configuration);
        this.textProcessorsManager = textProcessorsManager;
    }

    @Override
    public String getName() {
        return ENRICHER_NAME;
    }

    public Node importConcept(ConceptRequest request) {
        try {
            List<Tag> conceptTags = new ArrayList<>();
            Node annotatedNode = request.getAnnotatedNode();
            Node tagToBeAnnotated = null;
            if (annotatedNode == null) {
                tagToBeAnnotated = request.getTag();
            }
            int depth = request.getDepth();
            String lang = request.getLanguage();
            Boolean splitTags = request.isSplitTag();
            Boolean filterByLang = request.isFilterByLanguage();
            List<String> admittedRelationships = request.getAdmittedRelationships();
            List<String> admittedPos = request.getAdmittedPos();
            Iterator<Node> tagsIterator;
            if (annotatedNode != null) {
                tagsIterator = getAnnotatedTextTags(annotatedNode);
            } else if (tagToBeAnnotated != null) {
                List<Node> proc = new ArrayList<>();
                proc.add(tagToBeAnnotated);
                tagsIterator = proc.iterator();
            } else {
                throw new RuntimeException("You need to specify or an annotated text or a list of tags");
            }

            TextProcessor processor = textProcessorsManager.retrieveTextProcessor(request.getProcessor(), null);
            List<Tag> tags = new ArrayList<>();
            while (tagsIterator.hasNext()) {
                Tag tag = (Tag) getPersister(Tag.class).fromNode(tagsIterator.next());
                if (splitTags) {
                    List<Tag> annotateTags = processor.annotateTags(tag.getLemma(), lang);
                    if (annotateTags.size() == 1 && annotateTags.get(0).getLemma().equalsIgnoreCase(tag.getLemma())) {
                        tags.add(tag);
                    } else {
                        annotateTags.forEach((newTag) -> {
                            tags.add(newTag);
                            tag.addParent(RELATIONSHIP_IS_RELATED_TO_SUB_TAG, newTag, 0.0f);
                        });
                        conceptTags.add(tag);
                    }
                } else {
                    tags.add(tag);
                }
            }
            tags.stream().forEach((tag) -> {
                conceptTags.addAll(getImporter().importHierarchy(tag, lang, filterByLang, depth, processor, admittedRelationships, admittedPos));
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
        } catch (Exception ex) {
            LOG.error("error!!!! ", ex);
            throw new RuntimeException("Error while importing from concept net 5", ex);
        }

    }

    public ConceptNet5Importer getImporter() {
        if (conceptnet5Importer == null) {
            // @todo move url to config settings
            String url = DEFAULT_CONCEPTNET_URL;
            this.conceptnet5Importer = new ConceptNet5Importer.Builder(url).build();
        }
        return conceptnet5Importer;
    }

    private ResourceIterator<Node> getAnnotatedTextTags(Node annotatedNode) throws QueryExecutionException {
        Map<String, Object> params = new HashMap<>();
        params.put("id", annotatedNode.getId());
        Result queryRes = getDatabase().execute("MATCH (n)-[*..2]->"
                + "(t:" + getConfiguration().getLabelFor(Labels.Tag) + ") "
                + "where id(n) = {id} return distinct t", params);
        ResourceIterator<Node> tags = queryRes.columnAs("t");
        return tags;
    }

}
