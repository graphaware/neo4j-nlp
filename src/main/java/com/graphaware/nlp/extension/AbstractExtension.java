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
package com.graphaware.nlp.extension;

import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.event.EventDispatcher;
import com.graphaware.nlp.persistence.persisters.Persister;
import com.graphaware.nlp.processor.TextProcessorsManager;
import org.neo4j.graphdb.GraphDatabaseService;

public abstract class AbstractExtension implements NLPExtension {

    protected NLPManager getNLPManager() {
        return NLPManager.getInstance();
    }

    protected DynamicConfiguration configuration() {
        return getNLPManager().getConfiguration();
    }

    protected Persister getPersister(Class clazz) {
        return getNLPManager().getPersister(clazz);
    }

    protected TextProcessorsManager getTextProcessorsManager() {
        return getNLPManager().getTextProcessorsManager();
    }

    protected GraphDatabaseService getDatabase() {
        return getNLPManager().getDatabase();
    }
    
    protected DynamicConfiguration getConfiguration() {
        return getNLPManager().getConfiguration();
    }

    @Override
    public void registerEventListeners(EventDispatcher eventDispatcher) {

    }

    @Override
    public void postLoaded() {
        
    }
}
