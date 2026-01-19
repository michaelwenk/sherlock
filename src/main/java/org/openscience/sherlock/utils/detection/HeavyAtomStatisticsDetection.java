package org.openscience.sherlock.utils.detection;

import casekit.nmr.utils.Utils;

import org.openscience.sherlock.dbservice.statistics.controller.HeavyAtomStatisticsController;
import org.openscience.sherlock.dbservice.statistics.service.model.HeavyAtomStatisticsRecord;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HeavyAtomStatisticsDetection {

        public static Map<String, Integer> detect(final HeavyAtomStatisticsController heavyAtomStatisticsController,
                        final String mf) {
                final Map<String, Integer> detectedHeavyAtomStatistics = new HashMap<>();
                final List<HeavyAtomStatisticsRecord> heavyAtomStatisticsRecordList = heavyAtomStatisticsController
                                .findByMf(mf)
                                .collectList()
                                .block();

                if (heavyAtomStatisticsRecordList != null) {
                        for (final HeavyAtomStatisticsRecord heavyAtomStatisticsRecord : heavyAtomStatisticsRecordList) {
                                detectedHeavyAtomStatistics.put(heavyAtomStatisticsRecord.getAtomPair(),
                                                heavyAtomStatisticsRecord.getCount());
                        }
                }

                return detectedHeavyAtomStatistics;
        }

        public static boolean checkAllowanceOfHeteroHeteroBonds(
                        final HeavyAtomStatisticsController heavyAtomStatisticsController,
                        final String mf,
                        final double threshold) {
                final Map<String, Integer> elementCounts = Utils.getMolecularFormulaElementCounts(mf);
                final int sumHeteroAtomsByMf = elementCounts.entrySet()
                                .stream()
                                .filter(entry -> !entry.getKey()
                                                .equals("C")
                                                && !entry.getKey()
                                                                .equals("H"))
                                .map(Map.Entry::getValue)
                                .reduce(0, Integer::sum);
                if (sumHeteroAtomsByMf <= 1) {
                        return false;
                }
                final Map<String, Integer> detectedHeavyAtomStatistics = detect(heavyAtomStatisticsController, mf);
                int sumHeteroAtoms = 0;
                String[] split;
                for (final Map.Entry<String, Integer> entry : detectedHeavyAtomStatistics.entrySet()) {
                        split = entry.getKey()
                                        .split("_");
                        if (!split[0].equals("C")
                                        && !split[1].equals("C")) {
                                sumHeteroAtoms += entry.getValue();
                        }
                }
                final int sumAll = detectedHeavyAtomStatistics.values()
                                .stream()
                                .reduce(0, Integer::sum);
                return sumHeteroAtoms
                                / (double) sumAll > threshold;
        }
}
