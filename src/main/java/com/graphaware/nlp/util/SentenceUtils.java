package com.graphaware.nlp.util;

import com.graphaware.nlp.persistence.Labels;
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
}
