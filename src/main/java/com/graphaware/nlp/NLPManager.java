package com.graphaware.nlp;

import com.graphaware.nlp.conceptnet5.ConceptProcedure;
import com.graphaware.nlp.module.NLPConfiguration;
import com.graphaware.nlp.processor.TextProcessorProcedure;
import com.graphaware.nlp.processor.TextProcessorsManager;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.impl.proc.Procedures;
import org.neo4j.kernel.internal.GraphDatabaseAPI;

public class NLPManager {

    private final NLPConfiguration nlpConfiguration;

    private final TextProcessorsManager textProcessorsManager;

    private final GraphDatabaseService database;

    public NLPManager(GraphDatabaseService database, NLPConfiguration nlpConfiguration) {
        this.nlpConfiguration = nlpConfiguration;
        this.textProcessorsManager = new TextProcessorsManager(database);
        this.database = database;
        registerProcedures();
    }

    public TextProcessorsManager getTextProcessorsManager() {
        return textProcessorsManager;
    }

    private void registerProcedures() {
        // temporary as procedures should move to official neo procedures
        TextProcessorProcedure textProcedures = new TextProcessorProcedure(database, textProcessorsManager);
        ConceptProcedure conceptProcedures = new ConceptProcedure(database, textProcessorsManager);

        Procedures procedures = ((GraphDatabaseAPI) database).getDependencyResolver().resolveDependency(Procedures.class);

        try {
            procedures.register(conceptProcedures.concept());
            procedures.register(textProcedures.annotate());
            procedures.register(textProcedures.sentiment());
            procedures.register(textProcedures.language());
            procedures.register(textProcedures.filter());
            procedures.register(textProcedures.train());
            procedures.register(textProcedures.test());
            //Managing Processor
            procedures.register(textProcedures.getProcessors());
            procedures.register(textProcedures.getPipelines());
            procedures.register(textProcedures.getPipelineInfos());
            procedures.register(textProcedures.addPipeline());
            procedures.register(textProcedures.removePipeline());
        } catch (Exception e) {
            //
        }
    }
}
