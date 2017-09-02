/*
 * Copyright (c) 2013-2017 GraphAware
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
