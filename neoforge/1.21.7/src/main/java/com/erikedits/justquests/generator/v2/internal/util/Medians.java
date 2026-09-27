package com.erikedits.justquests.generator.v2.internal.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Small statistics helpers. */
public final class Medians {
    private Medians() {
    }

    /** Median, or NaN for an empty list. */
    public static double median(List<Double> values) {
        if (values.isEmpty()) {
            return Double.NaN;
        }
        List<Double> s = new ArrayList<>(values);
        Collections.sort(s);
        int n = s.size();
        return n % 2 == 1 ? s.get(n / 2) : (s.get(n / 2 - 1) + s.get(n / 2)) / 2.0;
    }
}
