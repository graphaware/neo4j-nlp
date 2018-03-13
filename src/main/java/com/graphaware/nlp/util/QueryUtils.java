package com.graphaware.nlp.util;

import com.graphaware.nlp.configuration.DynamicConfiguration;
import com.graphaware.nlp.persistence.constants.Labels;

import java.util.HashMap;
import java.util.Map;

public class QueryUtils {

    public static String processQuery(String query, DynamicConfiguration configuration) {
        Map<String, String> labels = getAllDynamicLabels(configuration);
        for (String k : labels.keySet()) {
            query = query.replaceAll(k, labels.get(k));
        }

        return query;
    }

    public static Map<String, String> getAllDynamicLabels(DynamicConfiguration configuration) {
        Map<String, String> labels = new HashMap<>();
        labels.put("__ANNOTATED_TEXT__", configuration.getLabelFor(Labels.AnnotatedText).name());
        labels.put("__SENTENCE__", configuration.getLabelFor(Labels.Sentence).name());
        labels.put("__TAG_OCCURRENCE__", configuration.getLabelFor(Labels.TagOccurrence).name());
        labels.put("__TAG__", configuration.getLabelFor(Labels.Tag).name());

        return labels;
    }

}
