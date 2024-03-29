package org.openscience.sherlock.core.utils.detection;

import casekit.nmr.elucidation.Utilities;
import casekit.nmr.utils.Utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ForbiddenNeighborDetection {

    public static Map<Integer, Map<String, Map<Integer, Set<Integer>>>> detectForbiddenNeighbors(
            final Map<Integer, Map<String, Map<Integer, Set<Integer>>>> detectedConnectivities, final String mf) {
        final Set<String> atomTypesByMf = new HashSet<>(Utils.getMolecularFormulaElementCounts(mf)
                                                             .keySet());
        final Map<Integer, Map<String, Map<Integer, Set<Integer>>>> forbiddenNeighbors = new HashMap<>();
        for (final Map.Entry<Integer, Map<String, Map<Integer, Set<Integer>>>> entry : detectedConnectivities.entrySet()) {
            forbiddenNeighbors.put(entry.getKey(), Utilities.buildForbiddenNeighbors(entry.getValue(), atomTypesByMf));
        }

        return forbiddenNeighbors;
    }
}
