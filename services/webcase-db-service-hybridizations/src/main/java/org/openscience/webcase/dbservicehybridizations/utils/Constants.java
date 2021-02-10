package org.openscience.webcase.dbservicehybridizations.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Constants {

    // valid strings from LSD webpage: C N N5 O S S4 S6 F Cl Br I P P5 Si B X
    public static final Map<String, Map<String, Integer>> hybridizationConversionMap = createHybridizationConversionMap();

    private static Map<String, Map<String, Integer>> createHybridizationConversionMap() {
        // @TODO access this information from MongoDB and store it instead of hard coding it
        // possible command in MongoDB: db.hybridizations.aggregate([{$match: {nucleus: "15N"}}, {$group: {_id: null, set: {$addToSet: "$hybridization"}}}])
        // nucleus -> hybridization string -> number
        final Map<String, Map<String, Integer>> hybridizationConversionMap = new HashMap<>();
        hybridizationConversionMap.put("13C", new HashMap<>());
        hybridizationConversionMap.get("13C").put("PLANAR3", 3);
        hybridizationConversionMap.get("13C").put("SP3", 3);
        hybridizationConversionMap.get("13C").put("SP2", 2);
        hybridizationConversionMap.get("13C").put("SP1", 1);
        hybridizationConversionMap.put("15N", new HashMap<>());
        hybridizationConversionMap.get("15N").put("PLANAR3", 3);
        hybridizationConversionMap.get("15N").put("SP3", 3);
        hybridizationConversionMap.get("15N").put("SP2", 2);
        hybridizationConversionMap.get("15N").put("SP1", 1);

        return Collections.unmodifiableMap(hybridizationConversionMap);
    }
}
