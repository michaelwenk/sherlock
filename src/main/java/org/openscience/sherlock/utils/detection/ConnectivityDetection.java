package org.openscience.sherlock.utils.detection;

import casekit.nmr.elucidation.Constants;
import casekit.nmr.model.Signal;
import casekit.nmr.model.nmrium.Correlation;
import casekit.nmr.utils.Utils;

import org.openscience.sherlock.dbservice.statistics.controller.ConnectivityController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ConnectivityDetection {

        @Autowired
        private ConnectivityController connectivityController;

        public Map<Integer, Map<String, Map<Integer, Set<Integer>>>> detectByOccurrenceCounts(
                        final List<Correlation> correlationList,
                        final int shiftTol,
                        final double elementCountThreshold, final String mf, final String mode) {
                final Map<Integer, Map<String, Map<Integer, Set<Integer>>>> detectedOccurrences = new HashMap<>();
                Map<String, Integer[]> detectedOccurrencesTemp;
                Map<String, Map<Integer, Set<Integer>>> transformed;
                Correlation correlation;
                String multiplicity;
                Signal signal;
                for (int i = 0; i < correlationList.size(); i++) {
                        correlation = correlationList.get(i);
                        multiplicity = Utils.getMultiplicityFromProtonsCount(correlation);
                        signal = Utils.extractFirstSignalFromCorrelation(correlation);
                        if (!correlation.getAtomType()
                                        .equals("H")
                                        && multiplicity != null
                                        && signal != null
                                        && !correlation.getHybridization()
                                                        .isEmpty()) {
                                detectedOccurrencesTemp = connectivityController.detectOccurrenceCounts(
                                                Constants.nucleiMap.get(correlation.getAtomType()),
                                                correlation.getHybridization().stream().mapToInt(v -> v).toArray(),
                                                multiplicity, signal.getShift(0)
                                                                .intValue()
                                                                - shiftTol,
                                                signal.getShift(0)
                                                                .intValue()
                                                                + shiftTol,
                                                mf);
                                if (detectedOccurrencesTemp != null) {
                                        transformed = new HashMap<>();
                                        for (final String neighborAtomType : detectedOccurrencesTemp.keySet()) {
                                                final int totalCount = Arrays
                                                                .stream(detectedOccurrencesTemp.get(neighborAtomType))
                                                                .reduce(0, Integer::sum);
                                                if (mode.equals("upperLimit")
                                                                && detectedOccurrencesTemp.get(neighborAtomType)[0]
                                                                                / (double) totalCount >= elementCountThreshold) {
                                                        transformed.put(neighborAtomType, new HashMap<>());
                                                } else if (mode.equals("lowerLimit")
                                                                && detectedOccurrencesTemp.get(neighborAtomType)[0]
                                                                                / (double) totalCount < elementCountThreshold) {
                                                        transformed.put(neighborAtomType, new HashMap<>());
                                                }
                                        }
                                        detectedOccurrences.put(i, transformed);
                                }
                        }
                }

                return detectedOccurrences;
        }
}
