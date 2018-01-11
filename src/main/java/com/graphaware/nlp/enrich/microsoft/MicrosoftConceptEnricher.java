package com.graphaware.nlp.enrich.microsoft;

import com.graphaware.common.util.Pair;
import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.dsl.request.ConceptRequest;
import com.graphaware.nlp.enrich.AbstractEnricher;
import com.graphaware.nlp.enrich.Enricher;
import com.graphaware.nlp.persistence.PersistenceRegistry;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.processor.TextProcessorsManager;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

import javax.ws.rs.core.MediaType;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.graphaware.nlp.util.TextUtils.removeApices;
import static com.graphaware.nlp.util.TextUtils.removeParenthesis;

public class MicrosoftConceptEnricher extends AbstractEnricher implements Enricher {

    public static final String ENRICHER_NAME = "MICROSOFT_CONCEPT";
    private static final String ALIAS_NAME = "microsoft";
    private final TextProcessorsManager textProcessorsManager;
    private final ClientConfig cfg = new DefaultClientConfig();

    public MicrosoftConceptEnricher(
            GraphDatabaseService database,
            PersistenceRegistry persistenceRegistry,
            TextProcessorsManager textProcessorsManager) {
        super(database, persistenceRegistry);
        this.textProcessorsManager = textProcessorsManager;
        cfg.getClasses().add(JacksonJsonProvider.class);
    }

    @Override
    public String getName() {
        return ENRICHER_NAME;
    }

    @Override
    public String getAlias() {
        return ALIAS_NAME;
    }

    @Override
    public Node importConcept(ConceptRequest request) {
        List<Tag> conceptTags = new ArrayList<>();
        TextProcessor processor = textProcessorsManager.retrieveTextProcessor(request.getProcessor(), TextProcessor.DEFAULT_PIPELINE);
        List<Tag> tags = new ArrayList<>();
        Pair<Iterator<Node>, Node> pair = getTagsIteratorFromRequest(request);
        Iterator<Node> tagsIterator = pair.first();
        Node tagToBeAnnotated = pair.second();
        while (tagsIterator.hasNext()) {
            Tag tag = (Tag) getPersister(Tag.class).fromNode(tagsIterator.next());
            tags.add(tag);
        }

        tags.forEach(tag -> {
            getConcepts(tag, 20).forEach(conceptTag -> {
                Tag annotatedTag = tryToAnnotate(conceptTag.getLemma(), "en", processor);
                conceptTag.getParents().forEach(parent -> {annotatedTag.addParent(parent);});
                conceptTags.add(annotatedTag);
            });
            conceptTags.add(tag);
        });

        conceptTags.forEach((newTag) -> {
            if (newTag != null) {
                getPersister(Tag.class).getOrCreate(newTag, newTag.getId(), String.valueOf(System.currentTimeMillis()));
            }
        });

        return tagToBeAnnotated;
    }

    public List<Tag> getConcepts(Tag tag, int limit) {
        final List<Tag> concepts = new ArrayList<>();
        String param = tag.getLemma();
        try {
            param = URLEncoder.encode(tag.getLemma(), "UTF-8");
        } catch (Exception e) {
            //
            return concepts;
        }
        String url = "https://concept.research.microsoft.com/api/Concept/ScoreByProb?instance="+ param + "&topK=" + limit;

        WebResource resource = Client.create(cfg).resource(url);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .get(ClientResponse.class);

        Map<String, Double> map = response.getEntity(Map.class);
        map.keySet().stream().forEach(k -> {
            Tag n = new Tag(cleanImportedConcept(k), "en");
            tag.addParent("IS_RELATED_TO", n, map.get(k).floatValue(), ENRICHER_NAME);
            concepts.add(n);
        });

        return concepts;

    }

    private String cleanImportedConcept(String concept) {
        concept = removeApices(concept);
        concept = removeParenthesis(concept);

        return concept;
    }
}
