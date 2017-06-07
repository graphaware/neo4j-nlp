package com.graphaware.nlp.ml.client;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparkRestClient {
    
    private static final Logger LOG = LoggerFactory.getLogger(SparkRestClient.class);

    private final String sparkEndPoint;
    private final ClientConfig cfg;

    public SparkRestClient(String url) {
        this.sparkEndPoint = url;
        this.cfg = new DefaultClientConfig();
        cfg.getClasses().add(JacksonJsonProvider.class);
    }
    
    public WebResource getWebResource(String baseURL) {
        String url = sparkEndPoint + "/" + baseURL;
        return Client.create(cfg).resource(url);
    }
    
//    public LDAResponse getValues() {
//        WebResource resource = Client.create(cfg).resource(url);
//        ClientResponse response = resource
//                .accept(MediaType.APPLICATION_JSON)
//                .type(MediaType.APPLICATION_JSON)
//                .get(ClientResponse.class);
//        ConceptNet5EdgeResult result = response.getEntity(ConceptNet5EdgeResult.class);
//        return result;
//    }
}
