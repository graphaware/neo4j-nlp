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
import com.graphaware.nlp.ml.word2vec.Word2VecProcessor;
import com.graphaware.nlp.module.NLPConfiguration;
import com.graphaware.nlp.persistence.PersistenceRegistry;
import com.graphaware.nlp.persistence.constants.Properties;
import com.graphaware.nlp.persistence.persisters.Persister;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.processor.TextProcessorsManager;
import com.graphaware.nlp.util.ProcessorUtils;
import com.graphaware.nlp.util.ServiceLoader;
import com.graphaware.nlp.vector.VectorComputation;
import com.graphaware.nlp.vector.VectorHandler;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.logging.Log;

import java.util.*;

public final class NLPManager {

    private static final Log LOG = LoggerFactory.getLogger(NLPManager.class);

    private static final String NEO4j_HOME = "unsupported.dbms.directories.neo4j_home";

    private static NLPManager instance = null;

    private NLPConfiguration nlpConfiguration;

    private TextProcessorsManager textProcessorsManager;

    protected GraphDatabaseService database;

    protected DynamicConfiguration configuration;

    private PersistenceRegistry persistenceRegistry;

    private EnrichmentRegistry enrichmentRegistry;

    private Map<String, VectorComputation> vectorComputationProcesses = new HashMap<>();

    private final Map<Class, NLPExtension> extensions = new HashMap<>();

    private EventDispatcher eventDispatcher;

    private boolean initialized = false;

    protected NLPManager() {
        super();
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

    public void init(GraphDatabaseService database, NLPConfiguration nlpConfiguration, DynamicConfiguration configuration) {
        if (initialized) {
            return;
        }
        this.nlpConfiguration = nlpConfiguration;
        this.textProcessorsManager = new TextProcessorsManager();
        this.configuration = configuration;
        this.database = database;
        this.persistenceRegistry = new PersistenceRegistry(database);
        this.enrichmentRegistry = buildAndRegisterEnrichers();
        this.eventDispatcher = new EventDispatcher();
        loadVectorComputationProcesses();
        loadExtensions();
        registerEventListeners();
        initialized = true;
        registerPipelinesFromConfig();
        registerWord2VecModelFromConfig();
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
        return annotateTextAndPersist(annotationRequest.getText(), annotationRequest.getId(), annotationRequest.getTextProcessor(),
                annotationRequest.getPipeline(), annotationRequest.isForce(), annotationRequest.shouldCheckLanguage());
    }

    public Node annotateTextAndPersist(String text, String id, String textProcessor, String pipelineName, boolean force, boolean checkForLanguage) {
        String lang = checkTextLanguage(text, checkForLanguage);
        String pipeline = getPipeline(pipelineName);
        PipelineSpecification pipelineSpecification = getConfiguration().loadPipeline(pipeline);
        if (null == pipelineSpecification) {
            throw new RuntimeException("No pipeline " + pipelineName + " found.");
        }
        TextProcessor processor = textProcessorsManager.getTextProcessor(pipelineSpecification.getTextProcessor());
        long startTime = -System.currentTimeMillis();
        AnnotatedText at = processor.annotateText(text, lang, pipelineSpecification);
        LOG.info("Time to annotate " + (System.currentTimeMillis() + startTime));

        return processAnnotationPersist(id, text, at, pipelineSpecification);
    }

    public Node annotateTextAndPersist(String text, String id, boolean checkForLanguage, PipelineSpecification pipelineSpecification) {
        String lang = checkTextLanguage(text, checkForLanguage);
        TextProcessor processor = textProcessorsManager.getTextProcessor(pipelineSpecification.getTextProcessor());
        AnnotatedText annotatedText = processor.annotateText(text, lang, pipelineSpecification);

        return processAnnotationPersist(id, text, annotatedText, pipelineSpecification);
    }

    public Node processAnnotationPersist(String id, String text, AnnotatedText annotatedText, PipelineSpecification pipelineSpecification) {
        String txId = String.valueOf(System.currentTimeMillis());
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

    public List<PipelineSpecification> getPipelineSpecifications() {
        return configuration.loadCustomPipelines();
    }

    public List<PipelineSpecification> getPipelineSpecifications(String name) {
        if (null == name || "".equals(name)) {
            return getPipelineSpecifications();
        }

        PipelineSpecification pipelineSpecification = configuration.loadPipeline(name);
        if (null != pipelineSpecification) {
            return Arrays.asList(pipelineSpecification);
        }

        return new ArrayList<>();
    }

    public void removePipeline(String pipeline, String processor) {
        configuration.removePipeline(pipeline, processor);
    }

    public Boolean filter(FilterRequest filterRequest) {
        String text = filterRequest.getText();
        checkTextLanguage(text, false);
        String lang = LanguageManager.getInstance().detectLanguage(text);
        String filter = filterRequest.getFilter();
        String pipeline = getPipeline(filterRequest.getPipeline());
        PipelineSpecification pipelineSpecification = configuration.loadPipeline(pipeline);
        TextProcessor currentTP = textProcessorsManager.getTextProcessor(pipelineSpecification.getTextProcessor());
        AnnotatedText annotatedText = currentTP.annotateText(text, lang, pipelineSpecification);
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

    public String checkTextLanguage(String text, boolean failIfUnsupported) {
        LanguageManager languageManager = LanguageManager.getInstance();
        String detectedLanguage = languageManager.detectLanguage(text);

        if (!languageManager.isTextLanguageSupported(text) && configuration.hasSettingValue(SettingsConstants.FALLBACK_LANGUAGE)) {
            return configuration.getSettingValueFor(SettingsConstants.FALLBACK_LANGUAGE).toString();
        }

        if (!languageManager.isTextLanguageSupported(text) && failIfUnsupported) {
            String msg = String.format("Unsupported language : %s", detectedLanguage);
            LOG.error(msg);
            throw new RuntimeException(msg);
        }

        return detectedLanguage;
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

    public void addPipeline(PipelineSpecification request) {
        // Check that the textProcessor exist !
        if (null == request.getTextProcessor() || textProcessorsManager.getTextProcessor(request.getTextProcessor()) == null) {
            throw new RuntimeException(String.format("Invalid text processor %s", request.getTextProcessor()));
        }
        PipelineSpecification pipelineSpecification = configuration.loadPipeline(request.getName());
        if (null != pipelineSpecification) {
            throw new RuntimeException("Pipeline with name " + request.getName() + " already exist");
        }
        configuration.storeCustomPipeline(request);
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

    public String getPipeline(String pipelineName) {
        return ProcessorUtils.getPipeline(pipelineName, configuration);
    }

    public boolean hasPipeline(String name) {
        List<PipelineSpecification> pipelines = getPipelineSpecifications(name);

        return pipelines.size() > 0;
    }

    public void setDefaultPipeline(String pipeline) {
        PipelineSpecification pipelineSpecification = configuration.loadPipeline(pipeline);
        if (null == pipelineSpecification) {
            throw new RuntimeException("No pipeline " + pipeline + " exist");
        }

        configuration.updateInternalSetting(SettingsConstants.DEFAULT_PIPELINE, pipeline);
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
        if (p.equals(SettingsConstants.DEFAULT_MODEL_WORKDIR)) {
            throw new RuntimeException("No default model wordking directory set in configuration");
        }

        return p;
    }

    private void loadExtensions() {
        Map<String, NLPExtension> extensionMap = ServiceLoader.loadInstances(NLPModuleExtension.class);

        extensionMap.keySet().forEach(k -> {
            NLPExtension extension = extensionMap.get(k);
            extension.postLoaded();
            extensions.put(extension.getClass(), extensionMap.get(k));
        });
    }

    private void registerEventListeners() {
        extensions.values().forEach(e -> {
            e.registerEventListeners(eventDispatcher);
        });
    }

    private void registerPipelinesFromConfig() {
        configuration.loadCustomPipelines().forEach(pipelineSpecification -> {
            // Check that the text processor exist, it can happen that the configuration
            // hold a reference to a processor that is not more registered, in order to avoid
            // this method to fail completely for valid pipelines, we just do not register
            // possible legacy pipelines
            if (textProcessorsManager.getTextProcessorNames().contains(pipelineSpecification.getTextProcessor())) {
                textProcessorsManager.createPipeline(pipelineSpecification);
            }
        });
    }

    private void loadVectorComputationProcesses() {
        Map<String, VectorComputation> extensionMap = ServiceLoader.loadInstances(NLPVectorComputationProcess.class);
        extensionMap.keySet().forEach(k -> {
            VectorComputation extension = extensionMap.get(k);
            extension.setDatabase(database);
            vectorComputationProcesses.put(extension.getType(), extensionMap.get(k));
        });
    }

    public VectorComputation getVectorComputationProcesses(String type) {
        return vectorComputationProcesses.get(type);
    }



}
