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
package com.graphaware.nlp.dsl;

import com.graphaware.nlp.configuration.DynamicConfiguration;
import org.codehaus.jackson.map.ObjectMapper;
import com.graphaware.nlp.NLPManager;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.procedure.Context;

public abstract class AbstractDSL {

    @Context
    public GraphDatabaseService database;

    public static ObjectMapper mapper = new ObjectMapper();

    protected NLPManager getNLPManager() {
        return NLPManager.getInstance();
    }

    protected DynamicConfiguration getConfiguration() {
        return getNLPManager().getConfiguration();
    }
}
