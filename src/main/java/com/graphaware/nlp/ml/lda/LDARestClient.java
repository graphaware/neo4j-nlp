/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.ml.lda;

import com.graphaware.nlp.ml.client.SparkRestClient;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import javax.ws.rs.core.MediaType;

public class LDARestClient {
    protected static final String LDA_BUILD_URL = "/nlp/engine/lda/build";

    public SparkRestClient sparkRestClient;

    public LDARestClient(SparkRestClient sparkRestClient) {
        this.sparkRestClient = sparkRestClient;
    }

    public LDAResponse computeLDA(LDARequest request) {
        WebResource resource = sparkRestClient.getWebResource(LDA_BUILD_URL);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .post(ClientResponse.class, request);
        LDAResponse result = response.getEntity(LDAResponse.class);
        return result;
    }

}
