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
package com.graphaware.nlp.processor;

import com.sun.org.apache.xpath.internal.operations.Bool;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PipelineInfo {

    public String name;

    public String textProcessorClass;

    public Map<String, Object> options;

    public Map<String, Object> specifications;

    public long numberOfThreads;

    public List<String> stopwords;

    public PipelineInfo(String name, String textProcessorClass, Map<String, Object> options, Map<String, Object> specifications, int numberOfThreads, List<String> stopwords) {
        this.name = name;
        this.textProcessorClass = textProcessorClass;
        this.options = options;
        this.specifications = specifications;
        this.numberOfThreads = numberOfThreads;
        this.stopwords = stopwords;
    }

    public String getName() {
        return name;
    }

    public String getTextProcessorClass() {
        return textProcessorClass;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public Map<String, Object> getSpecifications() {
        return specifications;
    }

    public long getNumberOfThreads() {
        return numberOfThreads;
    }

    public List<String> getStopwords() {
        return stopwords;
    }
}
