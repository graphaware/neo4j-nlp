package com.graphaware.nlp.summatization;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

import java.util.Map;

public interface Summarizer {
    public String getType();
    public void setDatabase(GraphDatabaseService database);
    public boolean evaluate(Map<String, Object> params);
}
