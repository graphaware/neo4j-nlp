/*
 * Copyright (c) 2013-2016 GraphAware
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
package com.graphaware.nlp.procedure;

import com.graphaware.nlp.application.search.SearchProcedure;
import com.graphaware.nlp.conceptnet5.ConceptProcedure;
import com.graphaware.nlp.ml.lda.LDAProcedure;
import com.graphaware.nlp.ml.similarity.FeatureBasedProcessLogic;
import com.graphaware.nlp.processor.TextProcessorProcedure;
import com.graphaware.nlp.ml.similarity.SimilarityProcedure;
import com.graphaware.nlp.ml.queue.SimilarityQueueProcessor;
import com.graphaware.nlp.module.NLPConfiguration;
import com.graphaware.nlp.processor.TextProcessorsManager;
import java.util.concurrent.Executors;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.api.exceptions.ProcedureException;
import org.neo4j.kernel.impl.proc.Procedures;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import org.neo4j.kernel.api.exceptions.KernelException;

@Component
public class NLPProcedures {

    private final GraphDatabaseService database;
    private final Procedures procedures;

    @Autowired
    private SimilarityQueueProcessor queueProcessor;

    @Autowired
    private FeatureBasedProcessLogic featureBusinessLogic;
    
    @Autowired
    private TextProcessorsManager processorsManager;
    
    private NLPConfiguration nlpConfiguration;

    @Autowired
    public NLPProcedures(GraphDatabaseService database, Procedures procedures) {
        this.database = database;
        this.procedures = procedures;
    }

    @PostConstruct
    public void init() throws ProcedureException, KernelException {
        TextProcessorProcedure textProcedures = new TextProcessorProcedure(database, processorsManager);
        procedures.register(textProcedures.annotate());
        procedures.register(textProcedures.sentiment());
        procedures.register(textProcedures.language());
        procedures.register(textProcedures.filter());
        //Managing Processor
        procedures.register(textProcedures.getProcessors());
        procedures.register(textProcedures.getPipelines());
        procedures.register(textProcedures.addPipeline());
        procedures.register(textProcedures.removePipeline());
        
        ConceptProcedure conceptProcedures = new ConceptProcedure(database, processorsManager);
        procedures.register(conceptProcedures.concept());
        
        SimilarityProcedure similarityProcedures = new SimilarityProcedure(featureBusinessLogic);
        procedures.register(similarityProcedures.computeAll());
        
        LDAProcedure ldaProcedures = new LDAProcedure(database, processorsManager);
        procedures.register(ldaProcedures.lda());
        procedures.register(ldaProcedures.topicDistribution());
        
        SearchProcedure searchProcedures = new SearchProcedure(database, processorsManager);
        procedures.register(searchProcedures.search());
        
        Executors.newSingleThreadExecutor().execute(queueProcessor);
    }
}
