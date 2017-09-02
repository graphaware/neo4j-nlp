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
