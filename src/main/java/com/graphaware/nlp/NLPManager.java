package com.graphaware.nlp;

import com.graphaware.common.log.LoggerFactory;
import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.dsl.PipelineSpecification;
import com.graphaware.nlp.dsl.AnnotationRequest;
import com.graphaware.nlp.dsl.FilterRequest;
import com.graphaware.nlp.dsl.result.ProcessorsList;
import com.graphaware.nlp.enrich.Enricher;
import com.graphaware.nlp.enrich.EnrichmentRegistry;
import com.graphaware.nlp.enrich.conceptnet5.ConceptNet5Enricher;
import com.graphaware.nlp.language.LanguageManager;
import com.graphaware.nlp.ml.pagerank.PageRankProcessor;
import com.graphaware.nlp.ml.similarity.FeatureBasedProcessLogic;
import com.graphaware.nlp.ml.similarity.SimilarityProcess;
import com.graphaware.nlp.ml.textrank.TextRankProcessor;
import com.graphaware.nlp.module.NLPConfiguration;
import com.graphaware.nlp.persistence.PersistenceRegistry;
import com.graphaware.nlp.persistence.constants.Properties;
import com.graphaware.nlp.persistence.persisters.Persister;
import com.graphaware.nlp.processor.PipelineInfo;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.processor.TextProcessorsManager;

import java.util.*;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.logging.Log;

public class NLPManager {

    private static final Log LOG = LoggerFactory.getLogger(NLPManager.class);


    private final NLPConfiguration nlpConfiguration;

    private final TextProcessorsManager textProcessorsManager;
    
    private final PageRankProcessor pageRankProcessor;
    
    private final TextRankProcessor textRankProcessor;
    
    private final SimilarityProcess similarityProcess;

    private final GraphDatabaseService database;

    private final DynamicConfiguration configuration;

    private final PersistenceRegistry persistenceRegistry;

    private final EnrichmentRegistry enrichmentRegistry;

    public NLPManager(GraphDatabaseService database, NLPConfiguration nlpConfiguration) {
        this.nlpConfiguration = nlpConfiguration;
        this.configuration = new DynamicConfiguration(database);
        this.textProcessorsManager = new TextProcessorsManager(database);
        this.database = database;
        this.persistenceRegistry = new PersistenceRegistry(database, configuration);
        this.enrichmentRegistry = buildAndRegisterEnrichers();
        this.pageRankProcessor = new PageRankProcessor(database);
        this.textRankProcessor = new TextRankProcessor(database);
        this.similarityProcess = new SimilarityProcess(new FeatureBasedProcessLogic(database));
    }

    public TextProcessorsManager getTextProcessorsManager() {
        return textProcessorsManager;
    }

    public Persister getPersister(Class clazz) {
        return persistenceRegistry.getPersister(clazz);
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

        return persistAnnotatedText(annotatedText, id, String.valueOf(System.currentTimeMillis()));
    }

    public Node persistAnnotatedText(AnnotatedText annotatedText, String id, String txId) {
        return getPersister(annotatedText.getClass()).persist(annotatedText, id, txId);
    }

    public DynamicConfiguration getConfiguration() {
        return configuration;
    }

    public List<PipelineInfo> getPipelineInformations(String pipelineName) {
        List<PipelineInfo> list = new ArrayList<>();
        textProcessorsManager.getTextProcessors().values().forEach((processor) -> {
            processor.getPipelineInfos().forEach(pipelineInfo -> {
                if (pipelineName.equals("") || pipelineInfo.getName().equals(pipelineName)) {
                    list.add(pipelineInfo);
                }
            });
        });
        return list;
    }

    public void removePipeline(String pipeline, String processor) {
        getTextProcessorsManager().getTextProcessor(processor).removePipeline(pipeline);
        configuration.removePipeline(pipeline, processor);
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
        TextProcessor currentTP = textProcessorsManager.retrieveTextProcessor(filterRequest.getProcessor(), filterRequest.getPipeline());
        AnnotatedText annotatedText = currentTP.annotateText(text, "tokenizer", lang, null);
        return annotatedText.filter(filter);

    }

    public void applySentiment(Node node, String textProcessor) {
        TextProcessor processor = textProcessor.equals("")
                ? getTextProcessorsManager().getDefaultProcessor()
                : getTextProcessorsManager().getTextProcessor(textProcessor);

        AnnotatedText annotatedText = (AnnotatedText) getPersister(AnnotatedText.class).fromNode(node);
        processor.sentiment(annotatedText);
        getPersister(AnnotatedText.class).persist(
                annotatedText,
                node.getProperty(configuration.getPropertyKeyFor(Properties.PROPERTY_ID)).toString(),
                String.valueOf(System.currentTimeMillis())
        );
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

    public Set<ProcessorsList> getProcessors() {
        Set<String> textProcessors = textProcessorsManager.getTextProcessorNames();
        Set<ProcessorsList> result = new HashSet<>();
        textProcessors.forEach(row -> {
            ProcessorsList processor = new ProcessorsList(row);
            result.add(processor);
        });
        return result;
    }

    public void addPipeline(PipelineSpecification request) {
        textProcessorsManager.createPipeline(request);
        configuration.storeCustomPipeline(request);
    }

    public Enricher getEnricher(String name) {
        return enrichmentRegistry.get(name);
    }

    private EnrichmentRegistry buildAndRegisterEnrichers() {
        EnrichmentRegistry registry = new EnrichmentRegistry();
        registry.register(new ConceptNet5Enricher(database, persistenceRegistry, configuration, textProcessorsManager));

        return registry;
    }

    public PageRankProcessor getPageRankProcessor() {
        return pageRankProcessor;
    }

    public TextRankProcessor getTextRankProcessor() {
        return textRankProcessor;
    }

    public SimilarityProcess getSimilarityProcess() {
        return similarityProcess;
    }
}
