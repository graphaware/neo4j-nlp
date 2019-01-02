package com.graphaware.nlp.enrich.microsoft;

import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.enrich.AbstractImporter;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

import javax.ws.rs.core.MediaType;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MicrosoftConteptImporter extends AbstractImporter {
    private final ClientConfig cfg = new DefaultClientConfig();

    public MicrosoftConteptImporter() {
        cfg.getClasses().add(JacksonJsonProvider.class);
    }

    public List<Tag> importHierarchy(Tag tag, int limit, String ENRICHER_NAME) {
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
            Tag annotatedTag = tryToAnnotate(cleanImportedConcept(k), "en");
            tag.addParent("IS_RELATED_TO", annotatedTag, map.get(k).floatValue(), ENRICHER_NAME);
            concepts.add(annotatedTag);
        });

        return concepts;

    }


}
