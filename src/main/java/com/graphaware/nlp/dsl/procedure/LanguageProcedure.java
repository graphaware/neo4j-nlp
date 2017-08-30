package com.graphaware.nlp.dsl.procedure;

import com.graphaware.nlp.dsl.AbstractDSL;
import com.graphaware.nlp.dsl.result.SingleResult;
import com.graphaware.nlp.language.LanguageManager;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.stream.Stream;

public class LanguageProcedure extends AbstractDSL {

    @Procedure(name = "ga.nlp.detectLanguage")
    @Description("Returns the language detected for the given text, 'n/a' when no language could be detected")
    public Stream<SingleResult> detectLanguage(@Name("text") String text) {
        String language = LanguageManager.getInstance().detectLanguage(text);

        return Stream.of(new SingleResult(language));
    }

}
