/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.nlp.pipeline.output;

import com.graphaware.nlp.annotation.NLPOutput;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;

@NLPOutput(name = "StoreAnnotatedTextPipelineOutput")
public class StoreAnnotatedTextPipelineOutput extends PipelineOutput<StoreAnnotatedTextPipelineConfiguration> {

    public StoreAnnotatedTextPipelineOutput(String name, GraphDatabaseService database) {
        super(name, database);
    }

    @Override
    public void init(Map<String, Object> parameters) {
        setConfiguration(new StoreAnnotatedTextPipelineConfiguration(parameters));
    }
    
}
