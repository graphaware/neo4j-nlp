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
package com.graphaware.nlp.util;

import com.graphaware.nlp.domain.Tag;

import java.util.Collections;
import java.util.List;

public class TagUtils {

    public static String getNamedEntityValue(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    public static Tag newTag(String lemma) {
        return new Tag(lemma, "en");
    }

    public static Tag newTag(String lemma, String language, String namedEntity) {
        Tag tag = new Tag(lemma, language);
        tag.setNe(Collections.singletonList(namedEntity));

        return tag;
    }

    public static Tag newTag(String lemma, List<String> namedEntities, List<String> pos) {
        Tag tag = new Tag(lemma, "en");
        tag.setNe(namedEntities);
        tag.setPos(pos);

        return tag;
    }
}
