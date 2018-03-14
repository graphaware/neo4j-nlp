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
package com.graphaware.nlp.workflow.processor;

import com.graphaware.nlp.workflow.WorkflowItem;
import com.graphaware.nlp.workflow.input.WorkflowInputEntry;
import org.neo4j.graphdb.GraphDatabaseService;
import com.graphaware.nlp.workflow.WorkflowConfiguration;

public abstract class WorkflowProcessor<C extends WorkflowConfiguration> extends WorkflowItem<C> {

    public static final String PIPELINE_PROCESSOR_KEY_PREFIX = "PIPELINE_PROCESSOR_";

    public WorkflowProcessor(String name, GraphDatabaseService database) {
        super(name, database);
    }

    public abstract WorkflowProcessorOutputEntry process(WorkflowInputEntry entry);

    @Override
    public String getPrefix() {
        return PIPELINE_PROCESSOR_KEY_PREFIX;
    }
}
