package com.graphaware.nlp.parser.procedure;

import com.graphaware.nlp.dsl.AbstractDSL;
import com.graphaware.nlp.parser.domain.Page;
import com.graphaware.nlp.parser.pdf.TikaPDFParser;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class PdfParserProcedure extends AbstractDSL {

    @Procedure(name = "ga.nlp.parser.pdf")
    public Stream<Page> parsePdf(@Name("file") String filename, @Name(value = "filterPatterns", defaultValue = "") List<String> filterPatterns) {
        TikaPDFParser parser = (TikaPDFParser) getNLPManager().getExtension(TikaPDFParser.class);
        List<String> filters = filterPatterns.equals("") ? new ArrayList<>() : filterPatterns;
        try {
            List<Page> pages = parser.parse(filename, filters);

            return pages.stream();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
