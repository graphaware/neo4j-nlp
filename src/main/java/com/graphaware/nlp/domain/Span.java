package com.graphaware.nlp.domain;

import com.graphaware.common.util.Pair;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

public class Span extends Pair<Integer, Integer> {

    @JsonCreator
    public Span(@JsonProperty("first") Integer o, @JsonProperty("second") Integer o2) {
        super(o, o2);
    }
}
