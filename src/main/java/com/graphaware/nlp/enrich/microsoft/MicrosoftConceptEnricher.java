package com.graphaware.nlp.enrich.microsoft;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MicrosoftConceptEnricher extends AbstractEnricher implements Enricher {

    //https://concept.research.microsoft.com/api/Concept/ScoreByProb?instance=chief executive officer&topK=10

    public static final String ENRICHER_NAME = "MICROSOFT_CONCEPT";
    private final TextProcessorsManager textProcessorsManager;
    private final ClientConfig cfg = new DefaultClientConfig();

    public MicrosoftConceptEnricher(
            GraphDatabaseService database,
            PersistenceRegistry persistenceRegistry,
            DynamicConfiguration configuration,
            TextProcessorsManager textProcessorsManager) {
        super(database, persistenceRegistry, configuration);
        this.textProcessorsManager = textProcessorsManager;
        cfg.getClasses().add(JacksonJsonProvider.class);
    }

    @Override
    public String getName() {
        return ENRICHER_NAME;
    }

    @Override
    public Node importConcept(ConceptRequest request) {
        List<Tag> conceptTags = new ArrayList<>();
        Node annotatedNode = request.getAnnotatedNode();
        Node tagToBeAnnotated = null;
        if (annotatedNode == null) {
            tagToBeAnnotated = request.getTag();
        }
        Iterator<Node> tagsIterator;
        List<Node> proc = new ArrayList<>();
        proc.add(tagToBeAnnotated);
        tagsIterator = proc.iterator();
        TextProcessor processor = textProcessorsManager.retrieveTextProcessor(request.getProcessor(), TextProcessor.DEFAULT_PIPELINE);
        List<Tag> tags = new ArrayList<>();
        while (tagsIterator.hasNext()) {
            Tag tag = (Tag) getPersister(Tag.class).fromNode(tagsIterator.next());
            tags.add(tag);
        }

        tags.stream().forEach(tag -> {
            conceptTags.addAll(getConcepts(tag));
            conceptTags.add(tag);
        });

        conceptTags.stream().forEach((newTag) -> {
            if (newTag != null) {
                getPersister(Tag.class).getOrCreate(newTag, newTag.getId(), String.valueOf(System.currentTimeMillis()));
            }
        });

        return tagToBeAnnotated;
    }

    public List<Tag> getConcepts(Tag tag) {
        final List<Tag> concepts = new ArrayList<>();
        String url = "https://concept.research.microsoft.com/api/Concept/ScoreByProb?instance="+tag.getLemma()+"&topK=10";

        WebResource resource = Client.create(cfg).resource(url);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .get(ClientResponse.class);

        Map<String, Double> map = response.getEntity(Map.class);
        map.keySet().stream().forEach(k -> {
            Tag n = new Tag(k, "en");
            tag.addParent("IS_RELATED_TO", n, map.get(k).floatValue());
            concepts.add(n);
        });

        return concepts;

    }
}
