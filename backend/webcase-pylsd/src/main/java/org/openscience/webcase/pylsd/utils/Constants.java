package org.openscience.webcase.pylsd.utils;

import java.util.Map;

public class Constants {

    // valid strings from LSD webpage: C N N5 O S S4 S6 F Cl Br I P P5 Si B X
    public static final Map<String, String> nucleiMap = createNucleiMap();

    private static Map<String, String> createNucleiMap() {
        return Map.of("C", "13C", "N", "15N", "H", "1H");
    }
}
