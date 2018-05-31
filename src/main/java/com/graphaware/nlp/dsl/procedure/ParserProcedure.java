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
package com.graphaware.nlp.dsl.procedure;

import com.graphaware.nlp.dsl.AbstractDSL;
import com.graphaware.nlp.parser.AbstractParser;
import com.graphaware.nlp.parser.Parser;
import com.graphaware.nlp.parser.domain.Page;
import com.graphaware.nlp.parser.pdf.TikaPDFParser;
import com.graphaware.nlp.parser.poi.PowerpointParser;
import com.graphaware.nlp.parser.poi.WordParser;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ParserProcedure extends AbstractDSL {

    @Procedure(name = "ga.nlp.parser.pdf")
    public Stream<Page> parsePdf(@Name("file") String filename, @Name(value = "filterPatterns", defaultValue = "") List<String> filterPatterns, @Name(value = "options", defaultValue = "") Map<String, Object> options) {
        TikaPDFParser parser = (TikaPDFParser) getNLPManager().getExtension(TikaPDFParser.class);

        return getPages(parser, filename, filterPatterns, options).stream();
    }

    @Procedure(name = "ga.nlp.parser.powerpoint")
    public Stream<Page> parsePowerpoint(@Name("file") String filename, @Name(value = "filterPatterns", defaultValue = "") List<String> filterPatterns,  @Name(value = "options", defaultValue = "") Map<String, Object> options) {
        PowerpointParser parser = (PowerpointParser) getNLPManager().getExtension(PowerpointParser.class);

        return getPages(parser, filename, filterPatterns, options).stream();
    }

    @Procedure(name = "ga.nlp.parser.word")
    public Stream<Page> parseWord(@Name("file") String filename, @Name(value = "filterPatterns", defaultValue = "") List<String> filterPatterns,  @Name(value = "options", defaultValue = "") Map<String, Object> options) {
        WordParser parser = (WordParser) getNLPManager().getExtension(WordParser.class);

        return getPages(parser, filename, filterPatterns, options).stream();
    }

    private List<Page> getPages(Parser parser, String filename, List<String> filterPatterns, Map<String, Object> options) {
        List<String> filters = filterPatterns.equals("") ? new ArrayList<>() : filterPatterns;
        Map<String, Object> parserOptions = options.equals("") ? new HashMap<>() : options;
        augmentParserOptions(parserOptions);
        try {
            List<Page> pages = parser.parse(filename, filters, parserOptions);

            return pages;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void augmentParserOptions(Map<String, Object> parserOptions) {
        if (getConfiguration().hasSettingValue("DEFAULT_UA")) {
            parserOptions.put(AbstractParser.USER_AGENT_OPTION, getConfiguration().getSettingValueFor("DEFAULT_UA"));
        }
    }
}
