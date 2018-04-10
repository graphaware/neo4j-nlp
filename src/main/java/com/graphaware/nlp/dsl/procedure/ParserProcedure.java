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
import com.graphaware.nlp.parser.domain.Page;
import com.graphaware.nlp.parser.pdf.TikaPDFParser;
import com.graphaware.nlp.parser.powerpoint.PowerpointParser;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ParserProcedure extends AbstractDSL {

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

    @Procedure(name = "ga.nlp.parser.powerpoint")
    public Stream<Page> parsePowerpoint(@Name("file") String filename, @Name(value = "filterPatterns", defaultValue = "") List<String> filterPatterns) {
        PowerpointParser parser = (PowerpointParser) getNLPManager().getExtension(PowerpointParser.class);
        List<String> filters = filterPatterns.equals("") ? new ArrayList<>() : filterPatterns;
        try {
            List<Page> pages = parser.parse(filename, filters);

            return pages.stream();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
