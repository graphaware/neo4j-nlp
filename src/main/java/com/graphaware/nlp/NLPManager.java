/*
 * Copyright (c) 2013-2017 GraphAware
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
import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.dsl.request.PipelineSpecification;
import com.graphaware.nlp.dsl.request.AnnotationRequest;
import com.graphaware.nlp.dsl.request.FilterRequest;
import com.graphaware.nlp.dsl.result.ProcessorsList;
import com.graphaware.nlp.enrich.Enricher;
import com.graphaware.nlp.enrich.EnrichmentRegistry;
import com.graphaware.nlp.enrich.conceptnet5.ConceptNet5Enricher;
import com.graphaware.nlp.event.EventDispatcher;
import com.graphaware.nlp.event.TextAnnotationEvent;
import com.graphaware.nlp.extension.NLPExtension;
import com.graphaware.nlp.language.LanguageManager;
import com.graphaware.nlp.module.NLPConfiguration;
import com.graphaware.nlp.persistence.PersistenceRegistry;
import com.graphaware.nlp.persistence.constants.Properties;
import com.graphaware.nlp.persistence.persisters.Persister;
import com.graphaware.nlp.processor.PipelineInfo;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.processor.TextProcessorsManager;

import java.util.*;

import com.graphaware.nlp.util.ServiceLoader;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.logging.Log;

public final class NLPManager {

    private static final Log LOG = LoggerFactory.getLogger(NLPManager.class);

    private static NLPManager instance = null;

    private NLPConfiguration nlpConfiguration;

    private TextProcessorsManager textProcessorsManager;

    private GraphDatabaseService database;

    private DynamicConfiguration configuration;

    private PersistenceRegistry persistenceRegistry;

    private EnrichmentRegistry enrichmentRegistry;

    private final Map<Class, NLPExtension> extensions = new HashMap<>();

    private EventDispatcher eventDispatcher;

    private boolean initialized = false;

    private NLPManager() {
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

    public void init(GraphDatabaseService database, NLPConfiguration nlpConfiguration) {
        if (initialized) {
            return;
        }
        this.nlpConfiguration = nlpConfiguration;
        this.configuration = new DynamicConfiguration(database);
        this.textProcessorsManager = new TextProcessorsManager(database);
        this.database = database;
        this.persistenceRegistry = new PersistenceRegistry(database, configuration);
        this.enrichmentRegistry = buildAndRegisterEnrichers();
        this.eventDispatcher = new EventDispatcher();
        loadExtensions();
        registerEventListeners();
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
        String processorClass = annotationRequest.getTextProcessor() != null ? annotationRequest.getTextProcessor() : textProcessorsManager.getDefaultProcessorName();
        if (processorClass == null) {
            throw new RuntimeException("Unable to find a processor for " + processorClass);
        }

        return annotateTextAndPersist(annotationRequest.getText(), annotationRequest.getId(), processorClass,
                annotationRequest.getPipeline(), annotationRequest.isForce(), annotationRequest.shouldCheckLanguage());
    }

    public Node annotateTextAndPersist(String text, String id, String textProcessor, String pipelineName, boolean force, boolean checkForLanguage) {
        String lang = checkTextLanguage(text, checkForLanguage);
        AnnotatedText annotatedText = textProcessorsManager.getTextProcessor(textProcessor).annotateText(
                text, pipelineName, lang, null
        );

        String txId = String.valueOf(System.currentTimeMillis());
        Node annotatedNode = persistAnnotatedText(annotatedText, id, txId);
        TextAnnotationEvent event = new TextAnnotationEvent(annotatedNode, annotatedText, id, txId);
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
        checkTextLanguage(text, false);
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

    private String checkTextLanguage(String text, boolean failIfUnsupported) {
        LanguageManager languageManager = LanguageManager.getInstance();
        String detectedLanguage = languageManager.detectLanguage(text);
        if (!languageManager.isTextLanguageSupported(text) && failIfUnsupported) {
            String msg = String.format("Unsupported language : %s", detectedLanguage);
            LOG.error(msg);
            throw new RuntimeException(msg);
        }

        return detectedLanguage;
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

    public NLPExtension getExtension(Class clazz) {
        if (extensions.containsKey(clazz)) {
            return extensions.get(clazz);
        }

        return null;
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
}
