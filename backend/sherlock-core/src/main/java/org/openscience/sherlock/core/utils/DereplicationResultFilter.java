package org.openscience.sherlock.core.utils;

import casekit.nmr.analysis.MultiplicitySectionsBuilder;
import casekit.nmr.model.Assignment;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import casekit.nmr.similarity.Similarity;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.BitSetFingerprint;
import org.openscience.cdk.io.MDLV3000Writer;
import org.openscience.sherlock.core.model.DereplicationOptions;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Collectors;

public class DereplicationResultFilter {


    public static List<DataSet> filterBySpectralSimilarity(List<DataSet> dataSetList, final Spectrum querySpectrum,
                                                           final DereplicationOptions dereplicationOptions,
                                                           final MultiplicitySectionsBuilder multiplicitySectionsBuilder) {
        final double shiftTolerance = dereplicationOptions.getShiftTolerance();
        final double maxAverageDeviation = dereplicationOptions.getMaxAverageDeviation();
        final boolean checkMultiplicity = dereplicationOptions.isCheckMultiplicity();
        final boolean checkEquivalencesCount = dereplicationOptions.isCheckEquivalencesCount();

        if (querySpectrum.getNDim()
                == 1
                && querySpectrum.getNuclei()[0].equals("13C")) {
            // @TODO get shift tolerance as arguments
            final BitSetFingerprint bitSetFingerprintQuerySpectrum = Similarity.getBitSetFingerprint(querySpectrum, 0,
                                                                                                     multiplicitySectionsBuilder);
            dataSetList = dataSetList.stream()
                                     .filter(dataSet -> {
                                         try {
                                             final Spectrum spectrum = dataSet.getSpectrum()
                                                                              .toSpectrum();
                                             final Assignment spectralMatchAssignment = Similarity.matchSpectra(
                                                     spectrum, querySpectrum, 0, 0, shiftTolerance, checkMultiplicity,
                                                     checkEquivalencesCount, false);

                                             dataSet.addMetaInfo("querySpectrumSignalCount",
                                                                 String.valueOf(querySpectrum.getSignalCount()));
                                             dataSet.addMetaInfo("querySpectrumSignalCountWithEquivalences",
                                                                 String.valueOf(
                                                                         querySpectrum.getSignalCountWithEquivalences()));
                                             dataSet.addMetaInfo("setAssignmentsCountWithEquivalences", String.valueOf(
                                                     spectralMatchAssignment.getSetAssignmentsCountWithEquivalences(
                                                             0)));
                                             final boolean isCompleteSpectralMatch = querySpectrum.getSignalCount()
                                                     == spectralMatchAssignment.getSetAssignmentsCount(0);
                                             final boolean isCompleteSpectralMatchWithEquivalences = querySpectrum.getSignalCountWithEquivalences()
                                                     == spectralMatchAssignment.getSetAssignmentsCountWithEquivalences(
                                                     0);
                                             dataSet.addMetaInfo("setAssignmentsCount", String.valueOf(
                                                     spectralMatchAssignment.getSetAssignmentsCount(0)));
                                             dataSet.addMetaInfo("setAssignmentsCountWithEquivalences", String.valueOf(
                                                     spectralMatchAssignment.getSetAssignmentsCountWithEquivalences(
                                                             0)));
                                             dataSet.addMetaInfo("isCompleteSpectralMatch",
                                                                 String.valueOf(isCompleteSpectralMatch));
                                             dataSet.addMetaInfo("isCompleteSpectralMatchWithEquivalences",
                                                                 String.valueOf(
                                                                         isCompleteSpectralMatchWithEquivalences));
                                             dataSet.addAttachment("spectralMatchAssignment", spectralMatchAssignment);


                                             // store as MOL file
                                             final MDLV3000Writer mdlv3000Writer = new MDLV3000Writer();
                                             final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                             mdlv3000Writer.setWriter(byteArrayOutputStream);
                                             mdlv3000Writer.write(dataSet.getStructure()
                                                                         .toAtomContainer());
                                             dataSet.addMetaInfo("molfile", byteArrayOutputStream.toString());


                                             if (checkEquivalencesCount
                                                 ? isCompleteSpectralMatchWithEquivalences
                                                 : isCompleteSpectralMatch) {

                                                 final Double averageDeviation = Similarity.calculateAverageDeviation(
                                                         spectrum, querySpectrum, 0, 0, spectralMatchAssignment);
                                                 if (averageDeviation
                                                         != null
                                                         && averageDeviation
                                                         <= maxAverageDeviation) {
                                                     dataSet.addMetaInfo("averageDeviation",
                                                                         String.valueOf(averageDeviation));
                                                     final Double rmsd = Similarity.calculateRMSD(spectrum,
                                                                                                  querySpectrum, 0, 0,
                                                                                                  spectralMatchAssignment);
                                                     dataSet.addMetaInfo("rmsd", String.valueOf(rmsd));

                                                     final BitSetFingerprint bitSetFingerprintDataSet = Similarity.getBitSetFingerprint(
                                                             spectrum, 0, multiplicitySectionsBuilder);
                                                     final Double tanimotoCoefficient = Similarity.calculateTanimotoCoefficient(
                                                             bitSetFingerprintQuerySpectrum, bitSetFingerprintDataSet);
                                                     dataSet.addMetaInfo("tanimoto",
                                                                         String.valueOf(tanimotoCoefficient));

                                                     return true;
                                                 }
                                             }
                                         } catch (final CDKException e) {
                                             e.printStackTrace();
                                         }

                                         return false;
                                     })
                                     .collect(Collectors.toList());
        }

        return dataSetList;
    }
}
