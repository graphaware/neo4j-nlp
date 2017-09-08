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
package com.graphaware.nlp.event;

import com.graphaware.common.log.LoggerFactory;
import org.neo4j.logging.Log;

import java.util.*;
import java.util.function.Consumer;

public class EventDispatcher {

    private static final Log LOG = LoggerFactory.getLogger(EventDispatcher.class);

    private static final int DEFAULT_PRIORITY = 100;
    private final Map<Events, SortedMap<Integer, List<Consumer>>> listeners = new HashMap<>();

    public void registerListener(Events eventName, Consumer consumer) {
        registerListener(eventName, consumer, DEFAULT_PRIORITY);
    }

    public void registerListener(Events eventName, Consumer consumer, int priority) {
        if (!listeners.containsKey(eventName)) {
            listeners.put(eventName, new TreeMap<>());
        }

        if (!listeners.get(eventName).containsKey(priority)) {
            listeners.get(eventName).put(priority, new ArrayList<>());
        }

        listeners.get(eventName).get(priority).add(consumer);
    }

    public void notify(Events eventName, Event event) {
        LOG.info("Notifying listeners for event {}", eventName.toString());
        if (!listeners.containsKey(eventName)) {
            return;
        }

        listeners.get(eventName).keySet().forEach(k -> {
            listeners.get(eventName).get(k).forEach(consumer -> {
                consumer.accept(event);
            });
        });
    }
}
