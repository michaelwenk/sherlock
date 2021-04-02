package org.openscience.webcase.dereplication.utils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static String getAlphabeticMF(final String mf) {
        final StringBuilder mfAlphabeticStringBuilder = new StringBuilder();
        final Map<String, Integer> mfAlphabeticMap = new TreeMap<>(getMolecularFormulaElementCounts(mf));
        for (final Map.Entry<String, Integer> entry : mfAlphabeticMap.entrySet()) {
            mfAlphabeticStringBuilder.append(entry.getKey());
            if (entry.getValue()
                    > 1) {
                mfAlphabeticStringBuilder.append(entry.getValue());
            }
        }

        return mfAlphabeticStringBuilder.toString();
    }

    public static Map<String, Integer> getMolecularFormulaElementCounts(final String mf) {
        final LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
        final List<String> elements = new ArrayList<>();
        Matcher matcher = Pattern.compile("([A-Z][a-z]{0,1})")
                                 .matcher(mf);
        while (matcher.find()) {
            elements.add(matcher.group(1));
        }
        int count;
        for (final String element : elements) {
            matcher = Pattern.compile("("
                                              + element
                                              + "\\d+)")
                             .matcher(mf);
            count = 1;
            if (matcher.find()) {
                count = Integer.parseInt(matcher.group(1)
                                                .split(element)[1]);
            }
            counts.put(element, count);
        }

        return counts;
    }
}
