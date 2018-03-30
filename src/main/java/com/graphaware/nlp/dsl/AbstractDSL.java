/*
 * Copyright (c) 2013-2018 GraphAware
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
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.module.NLPModule;
import com.graphaware.nlp.workflow.WorkflowManager;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.procedure.Context;

import java.util.Map;

import static com.graphaware.runtime.RuntimeRegistry.getStartedRuntime;

public abstract class AbstractDSL {

    @Context
    public GraphDatabaseService database;
    
    public static ObjectMapper mapper = new ObjectMapper();

    protected NLPManager getNLPManager() {
        return NLPManager.getInstance();
    }
    
    protected WorkflowManager getWorkflowManager() {
        return getStartedRuntime(database).getModule(NLPModule.class).getPipelineManager();
    }

    protected DynamicConfiguration getConfiguration() {
        return getNLPManager().getConfiguration();
    }

    protected void checkStringNotBlankOrFail(String s, String k) {
        if (!StringUtils.isNotBlank(s)) {
            throw new RuntimeException("Invalid string for " + k);
        }
    }

    protected void checkMapContainsValueAndNotBlank(String k, Map<String, Object> map) {
        if (!map.containsKey(k)) {
            throw new RuntimeException("Missing " + k + " in query parameters");
        }
        checkStringNotBlankOrFail(map.get(k).toString(), k);
    }
}
