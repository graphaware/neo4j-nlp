/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.pipeline.output;

import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.annotation.NLPOutput;
import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.pipeline.processor.PipelineProcessorOutputEntry;
import java.util.HashMap;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

@NLPOutput(name = "StoreAnnotatedTextPipelineOutput")
public class StoreAnnotatedTextPipelineOutput extends PipelineOutput<StoreAnnotatedTextPipelineConfiguration> {

    public StoreAnnotatedTextPipelineOutput(String name, GraphDatabaseService database) {
        super(name, database);
    }

    @Override
    public void init(Map<String, Object> parameters) {
        setConfiguration(new StoreAnnotatedTextPipelineConfiguration(parameters));
    }

    @Override
    public void process(PipelineProcessorOutputEntry entry) {
        try (Transaction tx = getDatabase().beginTx()) {
            Node newAnnotatedNode = persistAnnotatedText(entry.getAnnotateText(), (String) entry.getId(), "");
            String query = getConfiguration().getQuery();
            if (query != null
                    || !query.isEmpty()) {
                Map<String, Object> parameters = new HashMap<>();
                parameters.put("annotatedTextId", newAnnotatedNode.getId());
                parameters.put("entryId", entry.getId());
                getDatabase().execute(query, parameters);
            }
            tx.success();
        } catch (Exception ex) {
            throw new RuntimeException("Error while storing annotated text", ex);
        }
    }

    public Node persistAnnotatedText(AnnotatedText annotatedText, String id, String txId) {
        return NLPManager.getInstance().getPersister(annotatedText.getClass()).persist(annotatedText, id, txId);
    }

}
