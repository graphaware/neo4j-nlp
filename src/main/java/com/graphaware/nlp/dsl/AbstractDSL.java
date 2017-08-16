package com.graphaware.nlp.dsl;

import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.module.NLPModule;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.procedure.Context;

import static com.graphaware.runtime.RuntimeRegistry.getStartedRuntime;

public abstract class AbstractDSL {

    @Context
    public GraphDatabaseService database;

    protected NLPManager getNLPManager() {
        return getStartedRuntime(database).getModule(NLPModule.class).getNlpManager();
    }

}
