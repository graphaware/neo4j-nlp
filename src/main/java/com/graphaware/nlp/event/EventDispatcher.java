package com.graphaware.nlp.event;

import com.graphaware.nlp.Events;

import java.util.*;
import java.util.function.Consumer;

public class EventDispatcher {

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
