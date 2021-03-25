package org.openscience.webcase.pylsd.createinputfile.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Constants {

    // valid strings from LSD webpage: C N N5 O S S4 S6 F Cl Br I P P5 Si B X
    public static final Map<String, String> nucleiMap = createNucleiMap();
    public static final Map<String, String> defaultHybridizationMap = createDefaultHybridizationMap();
    public static final Map<String, String> defaultProtonsCountPerValencyMap = createDefaultProtonsCountPerValencyMap();
    public static final Map<String, String> defaultAtomLabelMap = createDefaultAtomLabelMap();

    private static Map<String, String> createNucleiMap() {

        return Map.of("C", "13C", "N", "15N", "H", "1H");
    }

    private static Map<String, String> createDefaultHybridizationMap() {

        return Map.of("C", "(1 2 3)", "N", "(1 2 3)", "O", "(2 3)", "S", "(1 2 3)", "I", "3");
    }

    private static Map<String, String> createDefaultProtonsCountPerValencyMap() {
        final Map<String, String> defaultProtonsCountPerValency = new HashMap<>();
        defaultProtonsCountPerValency.put("C", "(0 1 2 3)");
        defaultProtonsCountPerValency.put("N", "(0 1 2)");
        defaultProtonsCountPerValency.put("N5", "(0 1 2 3)");
        defaultProtonsCountPerValency.put("N35", "(0 1 2 3)");
        defaultProtonsCountPerValency.put("S", "(0 1)");
        defaultProtonsCountPerValency.put("S4", "(0 1 2 3)");
        defaultProtonsCountPerValency.put("S6", "(0 1 2 3)");
        defaultProtonsCountPerValency.put("S246", "(0 1 2 3)");
        defaultProtonsCountPerValency.put("O", "(0 1)");
        defaultProtonsCountPerValency.put("I", "(0 1)");

        return defaultProtonsCountPerValency;
    }

    private static Map<String, String> createDefaultAtomLabelMap() {

        return Map.of("C", "C", "N", "N35", "O", "O", "S", "S246", "I", "I");
    }
}
