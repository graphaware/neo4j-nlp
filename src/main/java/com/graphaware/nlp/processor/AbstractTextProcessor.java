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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractTextProcessor implements TextProcessor{

    public static final String PUNCT_REGEX_PATTERN = "^([a-z0-9]+)([-_][a-z0-9]+)?$";

    protected final Pattern patternCheck = Pattern.compile(PUNCT_REGEX_PATTERN, Pattern.CASE_INSENSITIVE);

    protected final Map<String, PipelineInfo> pipelineInfos = new HashMap<>();

    @Override
    public boolean checkLemmaIsValid(String value) {
        Matcher match = patternCheck.matcher(value);
        return match.find();
    }

    public static String getPunctRegexPattern() {
        return PUNCT_REGEX_PATTERN;
    }
}
