/*
 * Copyright (c) 2013-2018 GraphAware
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
package com.graphaware.nlp.module;

import com.graphaware.common.log.LoggerFactory;
import com.graphaware.nlp.NLPEvents;
import com.graphaware.nlp.NLPManager;
import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.configuration.MigrationHandler;
import com.graphaware.nlp.event.DatabaseTransactionEvent;
import com.graphaware.nlp.workflow.WorkflowManager;
import com.graphaware.runtime.module.BaseTxDrivenModule;
import com.graphaware.runtime.module.DeliberateTransactionRollbackException;
import com.graphaware.tx.event.improved.api.ImprovedTransactionData;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.logging.Log;

/**
 * {@link com.graphaware.runtime.module.TxDrivenModule} that assigns UUID's to
 * nodes in the graph.
 */
public class NLPModule extends BaseTxDrivenModule<Void> {

    private static final Log LOG = LoggerFactory.getLogger(NLPModule.class);
    
    protected final NLPConfiguration nlpMLConfiguration;

    protected final GraphDatabaseService database;

    protected NLPManager nlpManager;
    
    protected WorkflowManager pipelineManager;

    protected DynamicConfiguration dynamicConfiguration;

    public NLPModule(String moduleId, NLPConfiguration configuration, GraphDatabaseService database) {
        super(moduleId);
        this.nlpMLConfiguration = configuration;
        this.database = database;
    }

    @Override
    public void initialize(GraphDatabaseService database) {
        LOG.info("Initializing Community NLP Module");
        super.initialize(database);
        createConfiguration();
        checkMigrations(dynamicConfiguration);
        nlpManager = NLPManager.getInstance();
        nlpManager.init(database, nlpMLConfiguration, dynamicConfiguration);
        pipelineManager = WorkflowManager.getInstance();
        pipelineManager.init(database, nlpMLConfiguration, dynamicConfiguration);
    }

    protected void createConfiguration() {
        dynamicConfiguration = new DynamicConfiguration(database);
    }

    public NLPConfiguration getNlpMLConfiguration() {
        return nlpMLConfiguration;
    }

    public NLPManager getNlpManager() {
        return nlpManager;
    }

    public WorkflowManager getPipelineManager() {
        return pipelineManager;
    }
    
    @Override
    public Void beforeCommit(ImprovedTransactionData itd) throws DeliberateTransactionRollbackException {
        getNlpManager().getEventDispatcher().notify(NLPEvents.TRANSACTION_BEFORE_COMMIT, new DatabaseTransactionEvent(itd));
        return null;
    }

    protected void checkMigrations(DynamicConfiguration dynamicConfiguration) {
        MigrationHandler migrationHandler = new MigrationHandler(database, dynamicConfiguration);
        migrationHandler.migrate();
    }

}
