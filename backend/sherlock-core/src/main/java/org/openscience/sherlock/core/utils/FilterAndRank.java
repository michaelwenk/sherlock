package org.openscience.sherlock.core.utils;

import casekit.nmr.analysis.MultiplicitySectionsBuilder;
import casekit.nmr.model.Assignment;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import casekit.nmr.similarity.Similarity;
import casekit.nmr.utils.Statistics;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.BitSetFingerprint;
import org.openscience.cdk.io.MDLV3000Writer;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FilterAndRank {

    public static List<DataSet> filterAndRank(final List<DataSet> dataSetList, final Spectrum querySpectrum,
                                              final double shiftTolerance, final double maxAverageDeviation,
                                              final boolean checkMultiplicity, final boolean checkEquivalencesCount,
                                              final MultiplicitySectionsBuilder multiplicitySectionsBuilder,
                                              final boolean allowIncompleteMatch) {
        return rank(filter(dataSetList, querySpectrum, shiftTolerance, maxAverageDeviation, checkMultiplicity,
                           checkEquivalencesCount, multiplicitySectionsBuilder, allowIncompleteMatch));
    }

    public static List<DataSet> filter(List<DataSet> dataSetList, final Spectrum querySpectrum,
                                       final double shiftTolerance, final double maxAverageDeviation,
                                       final boolean checkMultiplicity, final boolean checkEquivalencesCount,
                                       final MultiplicitySectionsBuilder multiplicitySectionsBuilder,
                                       final boolean allowIncompleteMatch) {
        if (querySpectrum.getNDim()
                == 1
                && querySpectrum.getNuclei()[0].equals("13C")) {
            // @TODO get shift tolerance as arguments

            dataSetList = dataSetList.stream()
                                     .filter(dataSet -> checkDataSet(dataSet, querySpectrum, shiftTolerance,
                                                                     maxAverageDeviation, checkMultiplicity,
                                                                     checkEquivalencesCount,
                                                                     multiplicitySectionsBuilder, allowIncompleteMatch)
                                             != null)
                                     .collect(Collectors.toList());
        }

        return dataSetList;
    }

    public static DataSet checkDataSet(final DataSet dataSet, final Spectrum querySpectrum, final double shiftTolerance,
                                       final double maxAverageDeviation, final boolean checkMultiplicity,
                                       final boolean checkEquivalencesCount,
                                       final MultiplicitySectionsBuilder multiplicitySectionsBuilder,
                                       final boolean allowIncompleteMatch) {
        try {
            final BitSetFingerprint bitSetFingerprintQuerySpectrum = Similarity.getBitSetFingerprint(querySpectrum, 0,
                                                                                                     multiplicitySectionsBuilder);
            final Spectrum spectrum = dataSet.getSpectrum()
                                             .toSpectrum();
            final Assignment spectralMatchAssignment = Similarity.matchSpectra(spectrum, querySpectrum, 0, 0,
                                                                               shiftTolerance, checkMultiplicity,
                                                                               checkEquivalencesCount, false);

            dataSet.addMetaInfo("querySpectrumSignalCount", String.valueOf(querySpectrum.getSignalCount()));
            dataSet.addMetaInfo("querySpectrumSignalCountWithEquivalences",
                                String.valueOf(querySpectrum.getSignalCountWithEquivalences()));
            dataSet.addMetaInfo("setAssignmentsCountWithEquivalences",
                                String.valueOf(spectralMatchAssignment.getSetAssignmentsCountWithEquivalences(0)));
            final boolean isCompleteSpectralMatch = querySpectrum.getSignalCount()
                    == spectralMatchAssignment.getSetAssignmentsCount(0);
            final boolean isCompleteSpectralMatchWithEquivalences = querySpectrum.getSignalCountWithEquivalences()
                    == spectralMatchAssignment.getSetAssignmentsCountWithEquivalences(0);
            dataSet.addMetaInfo("setAssignmentsCount",
                                String.valueOf(spectralMatchAssignment.getSetAssignmentsCount(0)));
            dataSet.addMetaInfo("setAssignmentsCountWithEquivalences",
                                String.valueOf(spectralMatchAssignment.getSetAssignmentsCountWithEquivalences(0)));
            dataSet.addMetaInfo("isCompleteSpectralMatch", String.valueOf(isCompleteSpectralMatch));
            dataSet.addMetaInfo("isCompleteSpectralMatchWithEquivalences",
                                String.valueOf(isCompleteSpectralMatchWithEquivalences));
            dataSet.addAttachment("spectralMatchAssignment", spectralMatchAssignment);


            // store as MOL file
            final MDLV3000Writer mdlv3000Writer = new MDLV3000Writer();
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            mdlv3000Writer.setWriter(byteArrayOutputStream);
            mdlv3000Writer.write(dataSet.getStructure()
                                        .toAtomContainer());
            dataSet.addMetaInfo("molfile", byteArrayOutputStream.toString());

            Double[] deviations = Similarity.getDeviations(spectrum, querySpectrum, 0, 0, spectralMatchAssignment);
            if (allowIncompleteMatch) {
                deviations = Arrays.stream(deviations)
                                   .filter(Objects::nonNull)
                                   .toArray(Double[]::new);
            }
            final Double averageDeviation = Statistics.calculateAverageDeviation(deviations);
            if (averageDeviation
                    != null
                    && averageDeviation
                    <= maxAverageDeviation) {
                dataSet.addMetaInfo("averageDeviation", String.valueOf(averageDeviation));
                final Double rmsd = Statistics.calculateRMSD(deviations);
                dataSet.addMetaInfo("rmsd", String.valueOf(rmsd));

                final BitSetFingerprint bitSetFingerprintDataSet = Similarity.getBitSetFingerprint(spectrum, 0,
                                                                                                   multiplicitySectionsBuilder);
                final Double tanimotoCoefficient = Similarity.calculateTanimotoCoefficient(
                        bitSetFingerprintQuerySpectrum, bitSetFingerprintDataSet);
                dataSet.addMetaInfo("tanimoto", String.valueOf(tanimotoCoefficient));

                return dataSet;
            }
        } catch (final CDKException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static List<DataSet> rank(final List<DataSet> dataSetList) {
        dataSetList.sort((dataSet1, dataSet2) -> {
            final int avgDevComparison = compareNumericDataSetMetaKey(dataSet1, dataSet2, "averageDeviation");
            if (avgDevComparison
                    != 0) {
                return avgDevComparison;
            }

            final boolean isCompleteSpectralMatchDataSet1 = Boolean.parseBoolean(dataSet1.getMeta()
                                                                                         .get("isCompleteSpectralMatch"));
            final boolean isCompleteSpectralMatchDataSet2 = Boolean.parseBoolean(dataSet2.getMeta()
                                                                                         .get("isCompleteSpectralMatch"));
            if (isCompleteSpectralMatchDataSet1
                    && !isCompleteSpectralMatchDataSet2) {
                return -1;
            } else if (!isCompleteSpectralMatchDataSet1
                    && isCompleteSpectralMatchDataSet2) {
                return 1;
            }
            final int setAssignmentsCountComparison = compareNumericDataSetMetaKey(dataSet1, dataSet2,
                                                                                   "setAssignmentsCount");
            if (setAssignmentsCountComparison
                    != 0) {
                return -1
                        * setAssignmentsCountComparison;
            }

            return 0;
        });

        return dataSetList;
    }

    private static int compareNumericDataSetMetaKey(final DataSet dataSet1, final DataSet dataSet2,
                                                    final String metaKey) {
        Double valueDataSet1 = null;
        Double valueDataSet2 = null;
        try {
            valueDataSet1 = Double.parseDouble(dataSet1.getMeta()
                                                       .get(metaKey));
        } catch (final NullPointerException | NumberFormatException e) {
            //                e.printStackTrace();
        }
        try {
            valueDataSet2 = Double.parseDouble(dataSet2.getMeta()
                                                       .get(metaKey));
        } catch (final NullPointerException | NumberFormatException e) {
            //                e.printStackTrace();
        }

        if (valueDataSet1
                != null
                && valueDataSet2
                != null) {
            if (valueDataSet1
                    < valueDataSet2) {
                return -1;
            } else if (valueDataSet1
                    > valueDataSet2) {
                return 1;
            }
            return 0;
        }
        if (valueDataSet1
                != null) {
            return -1;
        } else if (valueDataSet2
                != null) {
            return 1;
        }

        return 0;
    }
}
