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
package com.graphaware.nlp.parser.pdf;

import org.apache.tika.sax.ToXMLContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class PageContentHandler extends ToXMLContentHandler {

    private final String pageTag = "div";
    private final String pageClass = "page";
    private final String paragraphTag = "p";
    private int currentPage = 0;
    private Map<Integer, StringBuilder> pageMap = new HashMap<>();
    private Map<Integer, List<String>> paraMaps = new HashMap<>();
    private final List<String> filterPatterns;
    private StringBuilder currentParagraph;

    public PageContentHandler(List<String> filterPatterns) {
        super();
        this.filterPatterns = filterPatterns;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if (qName.equals(pageTag) && atts.getValue("class").equals(pageClass)) {
            startPage();
        }

        if (qName.equals(paragraphTag)) {
            startParagraph();
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals(paragraphTag)) {
            endParagraph();
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (length > 0 && pageMap.containsKey(currentPage)) {
            pageMap.get(currentPage).append(ch);
            currentParagraph.append(ch);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (Integer i : paraMaps.keySet()) {
            sb.append("Page Number : ");
            sb.append(i);
            sb.append("\n");
            sb.append("Page Content : ");
            sb.append("\n");
            for (String s : getImprovedContent().get(i)) {
                sb.append(s);
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    public Map<Integer, List<String>> getImprovedContent() {
        Map<Integer, List<String>> content = new HashMap<>();

        boolean hasMergedFirstPageParagraph = false;
        for (Integer i : paraMaps.keySet()) {
            List<String> paragraphs = paraMaps.get(i);
            content.put(i, new ArrayList<>());
            for (int z = 0; z < paragraphs.size(); z++) {

                if (z == 0 && hasMergedFirstPageParagraph) {
                    hasMergedFirstPageParagraph = false;
                    continue;
                }

                String p = paragraphs.get(z).trim();
                if (!paraMaps.containsKey(i+1) || paraMaps.get(i+1).size() == 0) {
                    content.get(i).add(p);
                    continue;
                }
                String next = z == paragraphs.size() -1
                        ? paraMaps.get(i+1).get(0)
                        : paragraphs.get(z+1).trim();

                if (shouldMergeParagraphs(p, next)) {
                    if (z == paragraphs.size() - 1) {
                        hasMergedFirstPageParagraph = true;
                    }
                    String n = next.replaceFirst("^[0-9]+", " ");
                    content.get(i).add(p + " " + n);
                    ++z;
                } else {
                    content.get(i).add(p);
                }
            }
        }

        return content;
    }

    private void startPage() {
        currentPage++;
        pageMap.put(currentPage, new StringBuilder());
    }

    private void startParagraph() {
        currentParagraph = new StringBuilder("");
        paraMaps.put(currentPage, new ArrayList<>());
    }

    private void endParagraph() {
        if (currentParagraph != null && !currentParagraph.toString().equals("") && paraMaps.containsKey(currentPage-1)) {
            if (filtered()) {
                return;
            }
            paraMaps.get(currentPage-1).add(currentParagraph.toString());
        }
    }

    private boolean filtered() {
        for (String pattern : filterPatterns) {
            if (currentParagraph.toString().trim().replaceAll("\n", "").matches(pattern)) {
                return true;
            }
        }

        return false;
    }


    private boolean shouldMergeParagraphs(String p1, String p2) {
        if (!p1.endsWith(".") && (p2.endsWith(".") || p2.endsWith(".\n")) && !p2.endsWith("...")) {
            return true;
        }

        return false;
    }
}
