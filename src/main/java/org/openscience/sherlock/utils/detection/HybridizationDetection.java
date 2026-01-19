package org.openscience.sherlock.utils.detection;

import casekit.nmr.elucidation.Constants;
import casekit.nmr.model.Signal;
import casekit.nmr.model.nmrium.Correlation;
import casekit.nmr.utils.Utils;

import org.openscience.sherlock.dbservice.statistics.controller.HybridizationController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class HybridizationDetection {

        @Autowired
        private HybridizationController hybridizationController;

        public Map<Integer, List<Integer>> detectHybridizations(
                        final List<Correlation> correlationList,
                        final String mf, final float threshold,
                        final int shiftTol) {
                final Map<Integer, List<Integer>> detectedHybridizations = new HashMap<>();
                List<Integer> hybridizations;
                Correlation correlation;
                String multiplicity;
                Signal signal;
                for (int i = 0; i < correlationList.size(); i++) {
                        correlation = correlationList.get(i);
                        multiplicity = Utils.getMultiplicityFromProtonsCount(correlation);
                        signal = Utils.extractFirstSignalFromCorrelation(correlation);
                        if (!correlation.getAtomType()
                                        .equals("H")
                                        && Constants.nucleiMap.containsKey(correlation.getAtomType())
                                        && multiplicity != null
                                        && signal != null) {
                                hybridizations = hybridizationController.detectHybridizations(
                                                Constants.nucleiMap.get(correlation.getAtomType()),
                                                multiplicity,
                                                signal.getShift(0)
                                                                .intValue()
                                                                - shiftTol,
                                                signal.getShift(0)
                                                                .intValue()
                                                                + shiftTol,
                                                threshold,
                                                mf);

                                detectedHybridizations.put(i, hybridizations != null
                                                ? new ArrayList<>(new HashSet<>(hybridizations))
                                                : new ArrayList<>());
                        }
                }

                return detectedHybridizations;
        }
}
