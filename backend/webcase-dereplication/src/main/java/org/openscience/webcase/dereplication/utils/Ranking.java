package org.openscience.webcase.dereplication.utils;

import casekit.nmr.model.Assignment;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import casekit.nmr.similarity.Similarity;
import org.openscience.webcase.dereplication.model.DereplicationOptions;

import java.util.List;
import java.util.stream.Collectors;

public class Ranking {

    public static List<DataSet> rankBySpectralSimilarity(List<DataSet> dataSetList, final Spectrum querySpectrum,
                                                         final DereplicationOptions dereplicationOptions) {
        final double shiftTolerance = dereplicationOptions.getShiftTolerances()
                                                          .get("C");
        final boolean checkMultiplicity = dereplicationOptions.isCheckMultiplicity();
        final boolean checkEquivalencesCount = dereplicationOptions.isCheckEquivalencesCount();
        final double maxAverageDeviation = dereplicationOptions.getMaxAverageDeviation();

        if (querySpectrum.getNDim()
                == 1
                && querySpectrum.getNuclei()[0].equals("13C")) {
            // @TODO get shift tolerance as arguments
            dataSetList = dataSetList.stream()
                                     .filter(dataSet -> {
                                         final Assignment matchAssignment = Similarity.matchSpectra(
                                                 dataSet.getSpectrum(), querySpectrum, 0, 0, shiftTolerance,
                                                 checkMultiplicity, checkEquivalencesCount, false);

                                         if (checkEquivalencesCount
                                             ? matchAssignment.getSetAssignmentsCountWithEquivalences(0)
                                                     == querySpectrum.getSignalCountWithEquivalences()
                                             : matchAssignment.getSetAssignmentsCount(0)
                                                     == querySpectrum.getSignalCount()) {

                                             final Double averageDeviation = Similarity.calculateAverageDeviation(
                                                     dataSet.getSpectrum(), querySpectrum, 0, 0, matchAssignment);
                                             if (averageDeviation
                                                     != null
                                                     && averageDeviation
                                                     <= maxAverageDeviation) {
                                                 dataSet.addMetaInfo("averageDeviation",
                                                                     String.valueOf(averageDeviation));
                                                 final Double rmsd = Similarity.calculateRMSD(dataSet.getSpectrum(),
                                                                                              querySpectrum, 0, 0,
                                                                                              matchAssignment);
                                                 dataSet.addMetaInfo("rmsd", String.valueOf(rmsd));

                                                 return true;
                                             }
                                             return false;
                                         }

                                         return false;
                                     })
                                     .collect(Collectors.toList());

            dataSetList.sort((dataSet1, dataSet2) -> {
                if (Double.parseDouble(dataSet1.getMeta()
                                               .get("rmsd"))
                        < Double.parseDouble(dataSet2.getMeta()
                                                     .get("rmsd"))) {
                    return -1;
                } else if (Double.parseDouble(dataSet1.getMeta()
                                                      .get("rmsd"))
                        > Double.parseDouble(dataSet2.getMeta()
                                                     .get("rmsd"))) {
                    return 1;
                }
                return 0;
            });
        }

        return dataSetList;
    }
}
