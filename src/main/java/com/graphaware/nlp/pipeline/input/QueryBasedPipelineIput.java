/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.pipeline.input;

import com.graphaware.nlp.annotation.NLPInput;
import java.util.Iterator;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;

@NLPInput(name = "QueryBasedPipelineIput")
public class QueryBasedPipelineIput
        extends PipelineInput<PipelineInputQueryConfiguration, String> {

    public QueryBasedPipelineIput(String name, GraphDatabaseService database) {
        super(name, database);
    }

    @Override
    public void init(Map<String, Object> parameters) {
        setConfiguration(new PipelineInputQueryConfiguration(parameters));
    }

    @Override
    public Iterator<PipelineInputEntry<String>> iterator() {
        String query = getConfiguration().getQuery();
        Result rs = getDatabase().execute(query);
        return new IteratorWrapper(rs);
    }

    
    class IteratorWrapper implements Iterator<PipelineInputEntry<String>> {
        
        private final Result rs;

        public IteratorWrapper(Result rs) {
            this.rs = rs;
        }
        
        @Override
        public boolean hasNext() {
            boolean hasNext = rs.hasNext();
            if (!hasNext) {
                rs.close();
            }
            return hasNext;
        }

        @Override
        public PipelineInputEntry<String> next() {
            Map<String, Object> nextElement = rs.next();
            return new PipelineInputEntry<>(
                    (String)nextElement.get("text"), 
                    (String)nextElement.get("id"));
        }        
    }
}
