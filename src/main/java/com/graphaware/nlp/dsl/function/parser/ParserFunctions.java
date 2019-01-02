package com.graphaware.nlp.dsl.function.parser;

import com.graphaware.nlp.dsl.AbstractDSL;
import com.graphaware.nlp.parser.raw.RawFileParser;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

public class ParserFunctions extends AbstractDSL {

    @UserFunction(name = "ga.nlp.parse.raw")
    public String parseFileRaw(@Name("filePath") String filePath) {
        try {
            RawFileParser rawFileParser = (RawFileParser) getNLPManager().getExtension(RawFileParser.class);
            return rawFileParser.parse(filePath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
