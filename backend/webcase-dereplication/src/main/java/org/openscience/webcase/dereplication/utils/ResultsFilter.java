package org.openscience.webcase.dereplication.utils;

import casekit.nmr.analysis.MultiplicitySectionsBuilder;
import casekit.nmr.model.Assignment;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import casekit.nmr.similarity.Similarity;
import org.openscience.cdk.fingerprint.BitSetFingerprint;
import org.openscience.webcase.dereplication.model.DereplicationOptions;

import java.util.List;
import java.util.stream.Collectors;

public class ResultsFilter {


    public static List<DataSet> filterBySpectralSimilarity(List<DataSet> dataSetList, final Spectrum querySpectrum,
                                                           final DereplicationOptions dereplicationOptions,
                                                           final MultiplicitySectionsBuilder multiplicitySectionsBuilder) {
        final double shiftTolerance = dereplicationOptions.getShiftTolerances()
                                                          .get("C");
        final boolean checkMultiplicity = dereplicationOptions.isCheckMultiplicity();
        final boolean checkEquivalencesCount = dereplicationOptions.isCheckEquivalencesCount();
        final double maxAverageDeviation = dereplicationOptions.getMaxAverageDeviation();

        if (querySpectrum.getNDim()
                == 1
                && querySpectrum.getNuclei()[0].equals("13C")) {
            // @TODO get shift tolerance as arguments
            final BitSetFingerprint bitSetFingerprintQuerySpectrum = Similarity.getBitSetFingerprint(querySpectrum, 0,
                                                                                                     multiplicitySectionsBuilder);
            dataSetList = dataSetList.stream()
                                     .filter(dataSet -> {
                                         final Assignment matchAssignment = Similarity.matchSpectra(
                                                 dataSet.getSpectrum(), querySpectrum, 0, 0, shiftTolerance,
                                                 checkMultiplicity, checkEquivalencesCount, false);

                                         dataSet.addMetaInfo("querySpectrumSignalCount",
                                                             String.valueOf(querySpectrum.getSignalCount()));
                                         dataSet.addMetaInfo("querySpectrumSignalCountWithEquivalences", String.valueOf(
                                                 querySpectrum.getSignalCountWithEquivalences()));
                                         dataSet.addMetaInfo("setAssignmentsCountWithEquivalences", String.valueOf(
                                                 matchAssignment.getSetAssignmentsCountWithEquivalences(0)));
                                         final boolean isCompleteSpectralMatch = querySpectrum.getSignalCount()
                                                 == matchAssignment.getSetAssignmentsCount(0);
                                         final boolean isCompleteSpectralMatchWithEquivalences = querySpectrum.getSignalCountWithEquivalences()
                                                 == matchAssignment.getSetAssignmentsCountWithEquivalences(0);
                                         dataSet.addMetaInfo("setAssignmentsCount",
                                                             String.valueOf(matchAssignment.getSetAssignmentsCount(0)));
                                         dataSet.addMetaInfo("setAssignmentsCountWithEquivalences", String.valueOf(
                                                 matchAssignment.getSetAssignmentsCountWithEquivalences(0)));
                                         dataSet.addMetaInfo("isCompleteSpectralMatch",
                                                             String.valueOf(isCompleteSpectralMatch));
                                         dataSet.addMetaInfo("isCompleteSpectralMatchWithEquivalences",
                                                             String.valueOf(isCompleteSpectralMatchWithEquivalences));

                                         if (checkEquivalencesCount
                                             ? isCompleteSpectralMatchWithEquivalences
                                             : isCompleteSpectralMatch) {

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

                                                 final BitSetFingerprint bitSetFingerprintDataSet = Similarity.getBitSetFingerprint(
                                                         dataSet.getSpectrum(), 0, multiplicitySectionsBuilder);
                                                 final Double tanimotoCoefficient = Similarity.calculateTanimotoCoefficient(
                                                         bitSetFingerprintQuerySpectrum, bitSetFingerprintDataSet);
                                                 dataSet.addMetaInfo("tanimoto", String.valueOf(tanimotoCoefficient));

                                                 return true;
                                             }
                                             return false;
                                         }

                                         return false;
                                     })
                                     .collect(Collectors.toList());
        }

        return dataSetList;
    }
}
