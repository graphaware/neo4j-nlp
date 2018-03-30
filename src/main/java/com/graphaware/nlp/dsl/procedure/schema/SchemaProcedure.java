package com.graphaware.nlp.dsl.procedure.schema;

import com.graphaware.nlp.dsl.AbstractDSL;
import com.graphaware.nlp.dsl.result.KeyValueResult;
import com.graphaware.nlp.persistence.constants.Labels;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Procedure;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class SchemaProcedure extends AbstractDSL {

    public static String CREATED = "CREATED";
    public static String EXISTED = "EXISTED";
    public static String UNIQUE = "UNIQUE";

    @Procedure(name = "ga.nlp.createSchema", mode = Mode.SCHEMA)
    public Stream<KeyValueResult> createSchema() {
        Label annotatedTextLabel = getConfiguration().getLabelFor(Labels.AnnotatedText);
        Label sentenceLabel = getConfiguration().getLabelFor(Labels.Sentence);
        Label tagLabel = getConfiguration().getLabelFor(Labels.Tag);
        Label keywordLabel = getConfiguration().getLabelFor(Labels.Keyword);
        String property = "id";
        String textProperty = "value";

        List<KeyValueResult> results = new ArrayList<>();
        results.add(new KeyValueResult(format(annotatedTextLabel, property, UNIQUE), createConstraint(annotatedTextLabel, property)));
        results.add(new KeyValueResult(format(sentenceLabel, property, UNIQUE), createConstraint(sentenceLabel, property)));
        results.add(new KeyValueResult(format(tagLabel, property, UNIQUE), createConstraint(tagLabel, property)));
        results.add(new KeyValueResult(format(keywordLabel, property, UNIQUE), createConstraint(keywordLabel, property)));
        results.add(new KeyValueResult(format(tagLabel, textProperty, ""), createIndex(tagLabel, textProperty)));

        return results.stream();

    }

    private String createConstraint(Label label, String property) {
        for (ConstraintDefinition constraintDefinition : database.schema().getConstraints(label)) {
            for (String s : constraintDefinition.getPropertyKeys()) {
                if (s.equals(property)) {
                    return EXISTED;
                }
            }
        }

        try {
            database.schema().constraintFor(label).assertPropertyIsUnique(property).create();
            return CREATED;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String createIndex(Label label, String property) {
        for (IndexDefinition indexDefinition : database.schema().getIndexes()) {
            for (String s : indexDefinition.getPropertyKeys()) {
                if (s.equals(property)) {
                    return EXISTED;
                }
            }
        }

        try {
            database.schema().indexFor(label).on(property).create();
            return CREATED;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String format(Label label, String s, String type) {
        String suffix = type.equals(UNIQUE) ? "("+type+")" : "";
        return label.name() + "::" + s + suffix;
    }
}
