/*
 * Copyright (c) 2013-2016 GraphAware
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

import com.graphaware.runtime.module.BaseRuntimeModule;
import com.graphaware.spark.ml.SparkConnection;
import java.io.File;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link com.graphaware.runtime.module.TxDrivenModule} that assigns UUID's to
 * nodes in the graph.
 */
public class NLPModule extends BaseRuntimeModule {

    private static final Logger LOG = LoggerFactory.getLogger(NLPModule.class);

    public static final String DEFAULT_MODULE_ID = "NLP";

    private final NLPConfiguration nlpMLConfiguration;

    public NLPModule(String moduleId, NLPConfiguration configuration, GraphDatabaseService database) {
        super(moduleId);
        this.nlpMLConfiguration = configuration;
        LOG.info("ConceptNet ULR: " + nlpMLConfiguration.getConceptNetUrl());
    }

    @Override
    public void shutdown() {
    }

}
