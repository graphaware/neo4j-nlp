package com.graphaware.nlp;

import com.graphaware.common.log.LoggerFactory;
import com.graphaware.nlp.conceptnet5.ConceptNet5Importer;
import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.domain.AnnotatedText;
import com.graphaware.nlp.domain.Tag;
import com.graphaware.nlp.dsl.PipelineSpecification;
import com.graphaware.nlp.dsl.AnnotationRequest;
import com.graphaware.nlp.dsl.ConceptRequest;
import com.graphaware.nlp.dsl.FilterRequest;
import com.graphaware.nlp.dsl.result.ProcessorsList;
import com.graphaware.nlp.language.LanguageManager;
import com.graphaware.nlp.module.NLPConfiguration;
import com.graphaware.nlp.module.NLPModule;
import com.graphaware.nlp.persistence.AnnotatedTextPersister;
import com.graphaware.nlp.persistence.Labels;
import com.graphaware.nlp.processor.PipelineInfo;
import com.graphaware.nlp.processor.TextProcessor;
import com.graphaware.nlp.processor.TextProcessorsManager;
import com.graphaware.runtime.GraphAwareRuntime;
import com.graphaware.runtime.RuntimeRegistry;

import java.util.*;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.logging.Log;

public class NLPManager {

    private static final Log LOG = LoggerFactory.getLogger(NLPManager.class);
    private static final String RELATIONSHIP_IS_RELATED_TO_SUB_TAG = "subTag";


    private final NLPConfiguration nlpConfiguration;

    private final TextProcessorsManager textProcessorsManager;

    private final GraphDatabaseService database;

    private final AnnotatedTextPersister persister;

    private final DynamicConfiguration configuration;
    private ConceptNet5Importer conceptnet5Importer;

    public NLPManager(GraphDatabaseService database, NLPConfiguration nlpConfiguration) {
        this.nlpConfiguration = nlpConfiguration;
        this.configuration = new DynamicConfiguration(database);
        this.textProcessorsManager = new TextProcessorsManager(database);
        this.database = database;
        this.persister = new AnnotatedTextPersister(database, configuration);
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

    public Node importConcept(ConceptRequest request) {
        try {
            List<Tag> conceptTags = new ArrayList<>();
            Node annotatedNode = request.getAnnotatedNode();
            Node tagToBeAnnotated = null;
            if (annotatedNode == null) {
                tagToBeAnnotated = request.getTag();
            }
            int depth = request.getDepth();
            String lang = request.getLanguage();
            Boolean splitTags = request.isSplitTag();
            Boolean filterByLang = request.isFilterByLanguage();
            List<String> admittedRelationships = request.getAdmittedRelationships();
            List<String> admittedPos = request.getAdmittedPos();
            Iterator<Node> tagsIterator;
            if (annotatedNode != null) {
                tagsIterator = getAnnotatedTextTags(annotatedNode);
            } else if (tagToBeAnnotated != null) {
                List<Node> proc = new ArrayList<>();
                proc.add(tagToBeAnnotated);
                tagsIterator = proc.iterator();
            } else {
                throw new RuntimeException("You need to specify or an annotated text or a list of tags");
            }

            TextProcessor processor = retrieveTextProcessor(request.getProcessor(), null);
            List<Tag> tags = new ArrayList<>();
            while (tagsIterator.hasNext()) {
                Tag tag = persister.loadTag(tagsIterator.next());
                if (splitTags) {
                    List<Tag> annotateTags = processor.annotateTags(tag.getLemma(), lang);
                    if (annotateTags.size() == 1 && annotateTags.get(0).getLemma().equalsIgnoreCase(tag.getLemma())) {
                        tags.add(tag);
                    } else {
                        annotateTags.forEach((newTag) -> {
                            tags.add(newTag);
                            tag.addParent(RELATIONSHIP_IS_RELATED_TO_SUB_TAG, newTag, 0.0f);
                        });
                        conceptTags.add(tag);
                    }
                } else {
                    tags.add(tag);
                }
            }
            tags.stream().forEach((tag) -> {
                conceptTags.addAll(getImporter().importHierarchy(tag, lang, filterByLang, depth, processor, admittedRelationships, admittedPos));
                conceptTags.add(tag);
            });

            conceptTags.stream().forEach((newTag) -> {
                if (newTag != null) {
                    persister.getOrCreateTag(newTag, false);
                }
            });
            if (annotatedNode != null) {
                return annotatedNode;
            } else {
//                        Set<Object[]> result = new HashSet<>();
//                        conceptTags.stream().forEach((item) -> {
//                            result.add(new Object[]{item});
//                        });
//                        return Iterators.asRawIterator(result.iterator());
                return tagToBeAnnotated;
            }
        } catch (Exception ex) {
            LOG.error("error!!!! ", ex);
            throw new RuntimeException("Error while importing from concept net 5", ex);
        }

    }

    private ResourceIterator<Node> getAnnotatedTextTags(Node annotatedNode) throws QueryExecutionException {
        Map<String, Object> params = new HashMap<>();
        params.put("id", annotatedNode.getId());
        Result queryRes = database.execute("MATCH (n)-[*..2]->"
                + "(t:" + getConfiguration().getLabelFor(Labels.Tag) + ") "
                + "where id(n) = {id} return t", params);
        ResourceIterator<Node> tags = queryRes.columnAs("t");
        return tags;
    }
    
    public ConceptNet5Importer getImporter() {
        if (conceptnet5Importer == null) {
            String url = nlpConfiguration.getConceptNetUrl();
            this.conceptnet5Importer = new ConceptNet5Importer.Builder(url).build();
        }
        return conceptnet5Importer;
    }
}
