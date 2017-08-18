package com.graphaware.nlp;

import com.graphaware.common.log.LoggerFactory;
import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.dsl.PipelineSpecification;
import com.graphaware.nlp.dsl.AnnotationRequest;
import com.graphaware.nlp.dsl.FilterRequest;
import com.graphaware.nlp.dsl.result.ProcessorsList;
import com.graphaware.nlp.language.LanguageManager;
import com.graphaware.nlp.module.NLPConfiguration;
import com.graphaware.nlp.persistence.AnnotatedTextPersister;
import com.graphaware.nlp.processor.PipelineInfo;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.processor.TextProcessorsManager;

import java.util.*;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.kernel.impl.proc.Procedures;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.logging.Log;

public class NLPManager {

    private static final Log LOG = LoggerFactory.getLogger(NLPManager.class);

    private final NLPConfiguration nlpConfiguration;

    private final TextProcessorsManager textProcessorsManager;

    private final GraphDatabaseService database;

    private final AnnotatedTextPersister persister;

    private final DynamicConfiguration configuration;

    public NLPManager(GraphDatabaseService database, NLPConfiguration nlpConfiguration) {
        this.nlpConfiguration = nlpConfiguration;
        this.configuration = new DynamicConfiguration(database);
        this.textProcessorsManager = new TextProcessorsManager(database);
        this.database = database;
        this.persister = new AnnotatedTextPersister(database, configuration);
        registerProcedures();
    }

    public TextProcessorsManager getTextProcessorsManager() {
        return textProcessorsManager;
    }

    public Node annotateTextAndPersist(AnnotationRequest annotationRequest) {
        String processorClass = annotationRequest.getTextProcessor() != null ? annotationRequest.getTextProcessor() : textProcessorsManager.getDefaultProcessorName();
        if (processorClass == null) {
            throw new RuntimeException("Unable to find a processor for " + processorClass);
        }

        return annotateTextAndPersist(annotationRequest.getText(), annotationRequest.getId(), processorClass,
                annotationRequest.getPipeline(), annotationRequest.isForce(), annotationRequest.shouldCheckLanguage());
    }

    public Node annotateTextAndPersist(String text, String id, String textProcessor, String pipelineName, boolean force, boolean checkForLanguage) {
        if (checkForLanguage) {
            checkTextLanguage(text);
        }
        AnnotatedText annotatedText = textProcessorsManager.getTextProcessor(textProcessor).annotateText(
                text, pipelineName, "lang", null
        );

        return persistAnnotatedText(annotatedText, id, force);
    }

    public Node persistAnnotatedText(AnnotatedText annotatedText, String id, boolean force) {
        return persister.persist(annotatedText, id, force);
    }

    public DynamicConfiguration getConfiguration() {
        return configuration;
    }

    public List<PipelineInfo> getPipelineInformations() {
        List<PipelineInfo> list = new ArrayList<>();
        textProcessorsManager.getTextProcessors().values().forEach((processor) -> {
            list.addAll(processor.getPipelineInfos());
        });
        return list;
    }

    public Boolean filter(FilterRequest filterRequest) {
        String text = filterRequest.getText();
        if (text == null) {
            LOG.info("text is null");
            throw new RuntimeException("text is null or language not supported or unable to detect the language");
        }
        checkTextLanguage(text);
        String lang = LanguageManager.getInstance().detectLanguage(text);
        String filter = filterRequest.getFilter();
        if (filter == null) {
            throw new RuntimeException("A filter value needs to be provided");
        }
        TextProcessor currentTP = retrieveTextProcessor(filterRequest.getProcessor(), filterRequest.getPipeline());
        AnnotatedText annotatedText = currentTP.annotateText(text, "tokenizer", lang, null);
        return annotatedText.filter(filter);

    }

    private boolean checkTextLanguage(String text) {
        LanguageManager languageManager = LanguageManager.getInstance();
        if (!languageManager.isTextLanguageSupported(text)) {
            String detectedLanguage = languageManager.detectLanguage(text);
            String msg = String.format("Unsupported language : %s", detectedLanguage);
            LOG.error(msg);
            throw new RuntimeException(msg);
        }

        return true;
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

    public Set<ProcessorsList> getProcessors() {
        Set<String> textProcessors = textProcessorsManager.getTextProcessorNames();
        Set<ProcessorsList> result = new HashSet<>();
        textProcessors.forEach(row -> {
            ProcessorsList processor = new ProcessorsList(row);
            result.add(processor);
        });
        return result;
    }

    public Node addPipeline(PipelineSpecification request) {
        TextProcessorsManager.PipelineCreationResult creationResult = textProcessorsManager.createPipeline(request);
        if (creationResult.getResult() == 0) {
            Node storedPipeline = textProcessorsManager.storePipeline(request);
            return storedPipeline;
        }
        return null;
    }

    private TextProcessor retrieveTextProcessor(String processor, String pipeline) {
        TextProcessor newTP;
        if (processor != null && processor.length() > 0) {
            newTP = textProcessorsManager.getTextProcessor(processor);
            if (newTP == null) {
                throw new RuntimeException("Text processor " + processor + " doesn't exist");
            }
        } else {
            newTP = textProcessorsManager.getDefaultProcessor();
        }
        if (pipeline != null && pipeline.length() > 0) {
            if (!newTP.checkPipeline(pipeline)) {
                throw new RuntimeException("Pipeline with name " + pipeline
                        + " doesn't exist for processor " + newTP.getClass().getName());
            }
        }
        LOG.info("Using text processor: " + newTP.getClass().getName());

        return newTP;
    }
}
