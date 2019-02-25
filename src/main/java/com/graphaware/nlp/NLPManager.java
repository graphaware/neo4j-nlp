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
package com.graphaware.nlp;

import com.graphaware.common.log.LoggerFactory;
import com.graphaware.nlp.annotation.NLPModuleExtension;
import com.graphaware.nlp.annotation.NLPSummarizer;
import com.graphaware.nlp.annotation.NLPVectorComputationProcess;
import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.configuration.SettingsConstants;
import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.domain.VectorContainer;
import com.graphaware.nlp.dsl.request.*;
import com.graphaware.nlp.dsl.result.TextProcessorItem;
import com.graphaware.nlp.enrich.Enricher;
import com.graphaware.nlp.enrich.EnrichmentRegistry;
import com.graphaware.nlp.enrich.conceptnet5.ConceptNet5Enricher;
import com.graphaware.nlp.enrich.microsoft.MicrosoftConceptEnricher;
import com.graphaware.nlp.event.EventDispatcher;
import com.graphaware.nlp.event.TextAnnotationEvent;
import com.graphaware.nlp.extension.NLPExtension;
import com.graphaware.nlp.language.LanguageManager;
import com.graphaware.nlp.ml.textrank.TextRankSummarizer;
import com.graphaware.nlp.ml.word2vec.Word2VecProcessor;
import com.graphaware.nlp.persistence.PersistenceRegistry;
import com.graphaware.nlp.persistence.constants.Properties;
import com.graphaware.nlp.persistence.persisters.Persister;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.processor.TextProcessorsManager;
import com.graphaware.nlp.summatization.Summarizer;
import com.graphaware.nlp.util.ServiceLoader;
import com.graphaware.nlp.vector.VectorComputation;
import com.graphaware.nlp.vector.VectorHandler;
import org.apache.http.MethodNotSupportedException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.logging.Log;

import javax.ws.rs.NotSupportedException;
import java.util.*;

public final class NLPManager {

    private static final Log LOG = LoggerFactory.getLogger(NLPManager.class);

    private static final String NEO4j_HOME = "unsupported.dbms.directories.neo4j_home";

    private static NLPManager instance = null;

    private TextProcessorsManager textProcessorsManager;

    protected GraphDatabaseService database;

    protected DynamicConfiguration configuration;

    private PersistenceRegistry persistenceRegistry;

    private EnrichmentRegistry enrichmentRegistry;

    private Map<String, VectorComputation> vectorComputationProcesses = new HashMap<>();

    private Map<String, Summarizer> summarizers = new HashMap<>();

    private final Map<Class, NLPExtension> extensions = new HashMap<>();

    private EventDispatcher eventDispatcher;

    private boolean initialized = false;

    private LanguageManager languageManager;

    protected NLPManager() {
    }

    public static NLPManager getInstance() {
        if (NLPManager.instance == null) {
            synchronized (NLPManager.class) {
                if (NLPManager.instance == null) {
                    NLPManager.instance = new NLPManager();
                }
            }
        }

        return NLPManager.instance;
    }

    public void init(GraphDatabaseService database, DynamicConfiguration configuration) {
        if (initialized) {
            return;
        }
        this.configuration = configuration;

        this.languageManager = new LanguageManager();
        this.database = database;
        this.persistenceRegistry = new PersistenceRegistry(database);
        this.enrichmentRegistry = buildAndRegisterEnrichers();
        this.eventDispatcher = new EventDispatcher();
        loadExtensions();
        if (textProcessorsManager == null) {
            this.textProcessorsManager = new TextProcessorsManager(configuration);
        }
        this.textProcessorsManager.registerPipelinesFromConfig();
        loadVectorComputationProcesses();
        loadSummarizers();
        registerWord2VecModelFromConfig();

        initialized = true;
    }

    public TextProcessorsManager getTextProcessorsManager() {
        return textProcessorsManager;
    }

    public <T extends Persister> T getPersister(Class clazz) {
        return (T) persistenceRegistry.getPersister(clazz);
    }

    public GraphDatabaseService getDatabase() {
        return database;
    }

    public Node annotateTextAndPersist(AnnotationRequest annotationRequest) {
        return annotateTextAndPersist(annotationRequest.getText(), annotationRequest.getId(),
                annotationRequest.getPipeline());
    }

    public Node annotateTextAndPersist(String text, String id, String pipelineName) {
        PipelineSpecification pipelineSpecification = textProcessorsManager.getPipelineSpecification(pipelineName);
        LOG.info("Annotating with ID " + id);
        AnnotatedText at = textProcessorsManager.annotate(text, pipelineSpecification);
        return processAnnotationPersist(id, text, at, pipelineSpecification);
    }

    public Node annotateTextAndPersist(String text, String id, PipelineSpecification pipelineSpecification) {
        TextProcessor processor = textProcessorsManager.getTextProcessor(pipelineSpecification.getTextProcessor());
        LOG.info("Annotating with ID " + id);
        AnnotatedText annotatedText = processor.annotateText(text, pipelineSpecification);
        return processAnnotationPersist(id, text, annotatedText, pipelineSpecification);
    }

    public Node processAnnotationPersist(String id, String text, AnnotatedText annotatedText, PipelineSpecification pipelineSpecification) {
        String txId = String.valueOf(System.currentTimeMillis());
        TextAnnotationEvent preStorageEvent = new TextAnnotationEvent(annotatedText, txId, pipelineSpecification);
        eventDispatcher.notify(NLPEvents.PRE_ANNOTATION_STORAGE, preStorageEvent);
        Node annotatedNode = persistAnnotatedText(annotatedText, id, txId);
        TextAnnotationEvent event = new TextAnnotationEvent(annotatedNode, annotatedText, id, txId, pipelineSpecification);
        annotatedText.setText(text);
        eventDispatcher.notify(NLPEvents.POST_TEXT_ANNOTATION, event);

        return annotatedNode;
    }

    public Node persistAnnotatedText(AnnotatedText annotatedText, String id, String txId) {
        return getPersister(annotatedText.getClass()).persist(annotatedText, id, txId);
    }

    public DynamicConfiguration getConfiguration() {
        return configuration;
    }

    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }


    public Boolean filter(FilterRequest filterRequest) {
        String text = filterRequest.getText();
        String filter = filterRequest.getFilter();
        PipelineSpecification pipelineSpecification = textProcessorsManager.getPipelineSpecification(filterRequest.getPipeline());
        TextProcessor currentTP = textProcessorsManager.getTextProcessor(pipelineSpecification.getTextProcessor());
        AnnotatedText annotatedText = currentTP.annotateText(text, pipelineSpecification);
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

    public Set<TextProcessorItem> getProcessors() {
        Set<String> textProcessors = textProcessorsManager.getTextProcessorNames();
        Set<TextProcessorItem> result = new HashSet<>();
        textProcessors.forEach(row -> {
            TextProcessorItem processor = new TextProcessorItem(row);
            result.add(processor);
        });
        return result;
    }

    public void addWord2VecModel(Word2VecModelSpecification request) {
        Word2VecProcessor word2VecProcessor = (Word2VecProcessor) getExtension(Word2VecProcessor.class);
        word2VecProcessor.getWord2VecModel().createModelFromPaths(
                request.getSourcePath(),
                request.getDestinationPath(),
                request.getModelName(),
                request.getLanguage());
        configuration.storeWord2VecModel(request);
    }

    private void registerWord2VecModelFromConfig() {

        configuration.loadWord2VecModel().forEach(word2VecModelSpecification -> {
            try {
                Word2VecProcessor word2VecProcessor = (Word2VecProcessor) getExtension(Word2VecProcessor.class);
                word2VecProcessor.getWord2VecModel().createModelFromPaths(
                        word2VecModelSpecification.getSourcePath(),
                        word2VecModelSpecification.getDestinationPath(),
                        word2VecModelSpecification.getModelName(),
                        word2VecModelSpecification.getLanguage());
            } catch (Exception ex) {
                LOG.error("Error while loading the model: " + word2VecModelSpecification.getModelName(), ex);
                configuration.removeWord2VecModel(word2VecModelSpecification.getModelName());
            }
        });

    }


    public Enricher getEnricher(String name) {
        return enrichmentRegistry.resolve(name);
    }

    private EnrichmentRegistry buildAndRegisterEnrichers() {
        EnrichmentRegistry registry = new EnrichmentRegistry();
        registry.register(new ConceptNet5Enricher(database, persistenceRegistry, textProcessorsManager));
        registry.register(new MicrosoftConceptEnricher(database, persistenceRegistry, textProcessorsManager));

        return registry;
    }

    public EnrichmentRegistry getEnrichmentRegistry() {
        return enrichmentRegistry;
    }

    public NLPExtension getExtension(Class clazz) {
        if (extensions.containsKey(clazz)) {
            return extensions.get(clazz);
        }

        return null;
    }

    public Node computeVectorAndPersist(ComputeVectorRequest request) {
        try {
            VectorComputation vectorComputation = vectorComputationProcesses.get(request.getType());
            if (vectorComputation == null) {
                throw new RuntimeException("Cannot find the VectorComputation instance with type: " + request.getType());
            }
            VectorHandler vector
                    = vectorComputation.computeSparseVector(request.getInput().getId(), request.getParameters());
            if (vector != null) {
                VectorContainer vectorNode = new VectorContainer(request.getInput().getId(), request.getPropertyName(), vector);
                getPersister(vectorNode.getClass()).persist(vectorNode, request.getLabel(), null);
            }
            return request.getInput();
        } catch (Exception ex) {
            LOG.error("Error in computeVectorAndPersist", ex);
            throw ex;
        }
    }

    public boolean summarize(SummaryRequest request) {
        try {
            Summarizer summarizer = summarizers.get(request.getType());
            if (summarizer == null) {
                throw new RuntimeException("Cannot find the Summarizer instance with type: " + request.getType());
            }
            boolean res = summarizer.evaluate(request.getParameters());
            if (summarizer == null) {
                throw new RuntimeException("Cannot find the VectorComputation instance with type: " + request.getType());
            }
            return res;
        } catch (Exception ex) {
            LOG.error("Error in summarization", ex);
            throw ex;
        }
    }

    public void computeVectorTrainAndPersist(ComputeVectorTrainRequest request) {
        VectorComputation vectorComputation = vectorComputationProcesses.get(request.getType());
        if (vectorComputation == null) {
            throw new RuntimeException("Cannot find the VectorComputation instance with type: " + request.getType());
        }
        vectorComputation.train(request.getParameters());
    }

    public String train(CustomModelsRequest request) {
        TextProcessor processor = textProcessorsManager.getTextProcessor(request.getTextProcessor());
        return processor.train(request.getAlg(), request.getModelID(), request.getInputFile(), request.getLanguage(), request.getTrainingParameters());
    }

    public String test(CustomModelsRequest request) {
        TextProcessor processor = textProcessorsManager.getTextProcessor(request.getTextProcessor());
        return processor.test(request.getAlg(), request.getModelID(), request.getInputFile(), request.getLanguage());
    }

    public String getDefaultModelWorkdir() {
        String p = configuration.getSettingValueFor(SettingsConstants.DEFAULT_MODEL_WORKDIR).toString();
        if (p == null) {
            throw new RuntimeException("No default model wordking directory set in configuration");
        }

        return p;
    }

    public boolean hasDefaultModelWorkdir() {
        String p = configuration.getSettingValueFor(SettingsConstants.DEFAULT_MODEL_WORKDIR).toString();

        return p != null;
    }

    public void addModel(String modelId, String modelPath) {
        configuration.saveModelPath(modelId, modelPath);
    }

    private void loadExtensions() {
        Map<String, NLPExtension> extensionMap = ServiceLoader.loadInstances(NLPModuleExtension.class);

        extensionMap.keySet().forEach(k -> {
            NLPExtension extension = extensionMap.get(k);
            extension.postLoaded();
            extensions.put(extension.getClass(), extensionMap.get(k));
        });
    }

    private void loadVectorComputationProcesses() {
        Map<String, VectorComputation> extensionMap = ServiceLoader.loadInstances(NLPVectorComputationProcess.class);
        extensionMap.keySet().forEach(k -> {
            VectorComputation extension = extensionMap.get(k);
            extension.setDatabase(database);
            vectorComputationProcesses.put(extension.getType(), extension);
        });
    }

    private void loadSummarizers() {
        Map<String, Summarizer> extensionMap = ServiceLoader.loadInstances(NLPSummarizer.class);
        extensionMap.keySet().forEach(k -> {
            Summarizer extension = extensionMap.get(k);
            extension.setDatabase(database);
            summarizers.put(extension.getType(), extension);
        });
    }

    public VectorComputation getVectorComputationProcesses(String type) {
        return vectorComputationProcesses.get(type);
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public void setTextProcessorsManager(TextProcessorsManager textProcessorsManager) {
        this.textProcessorsManager = textProcessorsManager;
    }
}
