/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.workflow.output;

import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.annotation.NLPOutput;
import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.workflow.processor.WorkflowProcessorOutputEntry;
import java.util.HashMap;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

@NLPOutput(name = "StoreAnnotatedTextPipelineOutput")
public class StoreAnnotatedTextWorkflowOutput extends WorkflowOutput<StoreAnnotatedTextWorkflowConfiguration> {

    public StoreAnnotatedTextWorkflowOutput(String name, GraphDatabaseService database) {
        super(name, database);
    }

    @Override
    public void init(Map<String, Object> parameters) {
        setConfiguration(new StoreAnnotatedTextWorkflowConfiguration(parameters));
    }

    @Override
    public void handle(WorkflowProcessorOutputEntry entry) {
        try {
            Node newAnnotatedNode = persistAnnotatedText(entry.getAnnotateText(), (String) entry.getId(), "");
            String query = getConfiguration().getQuery();
            if (query != null
                    || !query.isEmpty()) {
                Map<String, Object> parameters = new HashMap<>();
                parameters.put("annotatedTextId", newAnnotatedNode.getId());
                parameters.put("entryId", entry.getId());
                getDatabase().execute(query, parameters);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Node persistAnnotatedText(AnnotatedText annotatedText, String id, String txId) {
        return NLPManager.getInstance().getPersister(annotatedText.getClass()).persist(annotatedText, id, txId);
    }

}
