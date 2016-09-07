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
package com.graphaware.nlp.processor;

import com.graphaware.nlp.annotation.NLPTextProcessor;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.util.ServiceLoader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TextProcessorsManager {
    private static final Logger LOG = LoggerFactory.getLogger(TextProcessorsManager.class);

    private final GraphDatabaseService database;
    private Map<String, TextProcessor> textProcessors;
    
    @Autowired
    public TextProcessorsManager(GraphDatabaseService database) {
        this.database = database;
        loadTextProcessors();
    }

    private void loadTextProcessors() {
        textProcessors = ServiceLoader.loadInstances(NLPTextProcessor.class);
    }

    public Set<String> getTextProcessors() {
        return textProcessors.keySet();
    }
    
    
    public TextProcessor getTextProcessor(String name) {
        return textProcessors.get(name);
    }
    
}
