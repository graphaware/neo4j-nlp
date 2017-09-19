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

import com.graphaware.nlp.persistence.constants.Labels;
import org.neo4j.graphdb.Label;

public class SentenceUtils {

    public static Label getDefaultLabelForSentimentLevel(int level) {
        switch (level) {
            case 0:
                return Labels.VeryNegative;
            case 1:
                return Labels.Negative;
            case 2:
                return Labels.Neutral;
            case 3:
                return Labels.Positive;
            case 4:
                return Labels.VeryPositive;
        }

        return null;
    }

    public static int getSentimentLevelForLabel(Label label) {
        switch (Labels.valueOf(label.name())) {
            case VeryPositive:
                return 4;
            case Positive:
                return 3;
            case Neutral:
                return 2;
            case Negative:
                return 1;
            case VeryNegative:
                return 0;
        }

        throw new IllegalArgumentException("Unrecognized label");
    }
}
