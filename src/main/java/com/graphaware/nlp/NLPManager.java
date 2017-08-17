package com.graphaware.nlp;

import com.graphaware.nlp.conceptnet5.ConceptProcedure;
import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.module.NLPConfiguration;
import com.graphaware.nlp.persistence.AnnotatedTextPersister;
import com.graphaware.nlp.processor.PipelineSpecification;
import com.graphaware.nlp.processor.TextProcessorProcedure;
import com.graphaware.nlp.processor.TextProcessorsManager;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.kernel.impl.proc.Procedures;
import org.neo4j.kernel.internal.GraphDatabaseAPI;

public class NLPManager {

    private final NLPConfiguration nlpConfiguration;

    private final TextProcessorsManager textProcessorsManager;

    private final GraphDatabaseService database;

    private final AnnotatedTextPersister persister;

    public NLPManager(GraphDatabaseService database, NLPConfiguration nlpConfiguration) {
        this.nlpConfiguration = nlpConfiguration;
        this.textProcessorsManager = new TextProcessorsManager(database);
        this.database = database;
        this.persister = new AnnotatedTextPersister(database);
        registerProcedures();
    }

    public TextProcessorsManager getTextProcessorsManager() {
        return textProcessorsManager;
    }

    public Node annotateTextAndPersist(String text, String id, PipelineSpecification pipelineSpecification, boolean force) {
        AnnotatedText annotatedText = textProcessorsManager.getTextProcessor(pipelineSpecification.getTextProcessor()).annotateText(
                text, pipelineSpecification.getName(), "lang", null
        );

        return persistAnnotatedText(annotatedText, id, force);
    }

    public Node persistAnnotatedText(AnnotatedText annotatedText, String id, boolean force) {
        return persister.persist(annotatedText, id, force);
    }

    public void updateConfigurationSetting(String key, Object value) {
        persister.updateConfigurationSetting(key, value);
    }

    private void registerProcedures() {
//        // temporary as procedures should move to official neo procedures
//        TextProcessorProcedure textProcedures = new TextProcessorProcedure(database, textProcessorsManager);
//        ConceptProcedure conceptProcedures = new ConceptProcedure(database, textProcessorsManager);

        Procedures procedures = ((GraphDatabaseAPI) database).getDependencyResolver().resolveDependency(Procedures.class);

        try {
//            procedures.register(conceptProcedures.concept());
//            procedures.register(textProcedures.annotate());
//            procedures.register(textProcedures.sentiment());
//            procedures.register(textProcedures.language());
//            procedures.register(textProcedures.filter());
//            procedures.register(textProcedures.train());
//            procedures.register(textProcedures.test());
//            //Managing Processor
//            procedures.register(textProcedures.getProcessors());
//            procedures.register(textProcedures.getPipelines());
//            procedures.register(textProcedures.getPipelineInfos());
//            procedures.register(textProcedures.addPipeline());
//            procedures.register(textProcedures.removePipeline());
        } catch (Exception e) {
            //
        }
    }
}
