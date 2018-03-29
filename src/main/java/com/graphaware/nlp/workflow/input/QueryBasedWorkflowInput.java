/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.workflow.input;

import com.graphaware.nlp.annotation.NLPInput;
import java.util.Iterator;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;

@NLPInput(name = "QueryBasedPipelineIput")
public class QueryBasedWorkflowInput
        extends WorkflowInput<WorkflowInputQueryConfiguration, String> {

    public QueryBasedWorkflowInput(String name, GraphDatabaseService database) {
        super(name, database);
    }

    @Override
    public void init(Map<String, Object> parameters) {
        setConfiguration(new WorkflowInputQueryConfiguration(parameters));
    }

    @Override
    public Iterator<WorkflowInputEntry<String>> iterator() {
        String query = getConfiguration().getQuery();
        Result rs = getDatabase().execute(query);
        return new IteratorWrapper(rs);
    }

    @Override
    public void handle(Void entry) {
        //The input is the starting point 
    }

    
    class IteratorWrapper implements Iterator<WorkflowInputEntry<String>> {
        
        private final Result rs;
        private boolean endOfQueue;

        public IteratorWrapper(Result rs) {
            this.rs = rs;
            this.endOfQueue = false;
        }
        
        @Override
        public boolean hasNext() {
            if (endOfQueue) {
                return false;
            }
            boolean hasNext = rs.hasNext();
            if (!hasNext) {
                endOfQueue = true;
                rs.close();
            }
            return true;
        }

        @Override
        public WorkflowInputEntry<String> next() {
            if (endOfQueue) {
                return new WorkflowInputEndOfQueueEntry<>();
            }
            Map<String, Object> nextElement = rs.next();
            return new WorkflowInputEntry<>(
                    (String)nextElement.get("text"), 
                    (String)nextElement.get("id"));
        }        
    }
}
