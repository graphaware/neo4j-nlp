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
package com.graphaware.nlp.module;

import com.graphaware.common.log.LoggerFactory;
import com.graphaware.runtime.module.BaseRuntimeModuleBootstrapper;
import com.graphaware.runtime.module.RuntimeModule;
import com.graphaware.runtime.module.RuntimeModuleBootstrapper;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.logging.Log;

import java.util.Map;

/**
 * Bootstraps the {@link NLPModule} in server mode.
 */
public class NLPBootstrapper extends BaseRuntimeModuleBootstrapper<NLPConfiguration> implements RuntimeModuleBootstrapper {

    private static final Log LOG = LoggerFactory.getLogger(NLPBootstrapper.class);

    private static final String CONCEPT_NET_URL = "conceptNetUrl";
    private static final String SPARK_REST_URL = "sparkRestUrl";

    /**
     * {@inheritDoc}
     */
    @Override
    protected NLPConfiguration defaultConfiguration() {
        return NLPConfiguration.defaultConfiguration();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected RuntimeModule doBootstrapModule(String moduleId, Map<String, String> config, GraphDatabaseService database, NLPConfiguration configuration) {
        if (config.get(CONCEPT_NET_URL) != null && config.get(CONCEPT_NET_URL).length() > 0) {
            configuration = configuration.withConceptNetUrl(config.get(CONCEPT_NET_URL));
            LOG.info("CONCEPT_NET_URL set to %s", configuration.getConceptNetUrl());
        }
        
        if (config.get(SPARK_REST_URL) != null && config.get(SPARK_REST_URL).length() > 0) {
            configuration = configuration.withSparkRestUrl(config.get(SPARK_REST_URL));
            LOG.info("SPARK_REST_URL set to %s", configuration.getSparkRestUrl());
        }
        return new NLPModule(moduleId, configuration, database);
    }
}
