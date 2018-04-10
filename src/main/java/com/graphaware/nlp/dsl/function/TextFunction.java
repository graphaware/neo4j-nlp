package com.graphaware.nlp.dsl.function;

import com.graphaware.nlp.dsl.AbstractDSL;
import com.graphaware.nlp.util.TextUtils;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

import java.util.List;

public class TextFunction extends AbstractDSL {

    @UserFunction(name = "ga.nlp.text.replaceAll")
    public String replaceAll(@Name("searches") List<String> searches, @Name("original") String original) {
        return TextUtils.replaceEach(searches, original);
    }
}
