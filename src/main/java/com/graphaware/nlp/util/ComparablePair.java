package com.graphaware.nlp.util;

import com.graphaware.common.util.Pair;
import org.jetbrains.annotations.NotNull;

public class ComparablePair extends Pair implements Comparable<Pair> {

    public ComparablePair(Object o, Object o2) {
        super(o, o2);
    }

    @Override
    public int compareTo(@NotNull Pair o) {
        if ((double) o.second() == (double) this.second()) {
            return 0;
        }
        return
                (double) o.second() < (double) this.second()
                ? 1
                : -1;
    }
}
