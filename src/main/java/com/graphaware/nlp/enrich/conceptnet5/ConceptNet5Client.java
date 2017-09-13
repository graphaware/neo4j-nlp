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
package com.graphaware.nlp.enrich.conceptnet5;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ConceptNet5Client {
    private static final Logger LOG = LoggerFactory.getLogger(ConceptNet5Client.class);

    private final String conceptNet5EndPoint;
    private final ClientConfig cfg;

    private final Cache<String, ConceptNet5EdgeResult> cache = CacheBuilder
            .newBuilder()
            .maximumSize(10000)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    public ConceptNet5Client(String conceptNet5EndPoint) {
        this.conceptNet5EndPoint = conceptNet5EndPoint;
        this.cfg = new DefaultClientConfig();
        cfg.getClasses().add(JacksonJsonProvider.class);
    }

    public ConceptNet5EdgeResult getValues(String concept, String lang, int limit) {
        String url = conceptNet5EndPoint + "/c/" + lang + "/" + concept + "?limit=" + limit;
        ConceptNet5EdgeResult value;
        try {
            value = cache.get(url, () -> cachedUrl(url));
        } catch (ExecutionException ex) {
            LOG.error("Error while getting value for concept " + concept + " lang " + lang, ex);
            throw new RuntimeException("Error while getting value for concept " + concept + " lang " + lang);
        }
        return value;
    }

    private ConceptNet5EdgeResult cachedUrl(String url) {
        WebResource resource = Client.create(cfg).resource(url);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .get(ClientResponse.class);
        ConceptNet5EdgeResult result = response.getEntity(ConceptNet5EdgeResult.class);
        return result;
    }

    public ConceptNet5EdgeResult queryByStart(String concept, String rel, String lang, int limit) {
        String url = conceptNet5EndPoint + "/query?rel=/r/" + rel + "&start=/c/" + lang + "/" + concept + "&limit=" + limit;
        ConceptNet5EdgeResult value;
        try {
            value = cache.get(url, () -> cachedUrl(url));
        } catch (ExecutionException ex) {
            String error = "Error while getting query for concept " + concept + " lang " + lang + " and relationship " + rel;
            LOG.error(error, ex);
            throw new RuntimeException(error, ex);
        }
        return value;
    }

    public String getConceptNet5EndPoint() {
        return conceptNet5EndPoint;
    }
}
