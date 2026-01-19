package org.openscience.sherlock.utils.detection;

import casekit.nmr.elucidation.Utilities;
import casekit.nmr.elucidation.model.Detections;
import casekit.nmr.elucidation.model.Grouping;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.nmrium.Correlation;
import casekit.nmr.model.nmrium.Correlations;

import org.openscience.sherlock.dbservice.statistics.controller.HeavyAtomStatisticsController;
import org.openscience.sherlock.model.exchange.Transfer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class Detection {

        @Autowired
        private HeavyAtomStatisticsController heavyAtomStatisticsController;
        @Autowired
        private HybridizationDetection hybridizationDetection;
        @Autowired
        private ConnectivityDetection connectivityDetection;
        @Autowired
        private FragmentsDetection fragmentsDetection;

        public Transfer detect(final Transfer requestTransfer) {
                final Transfer responseTransfer = new Transfer();
                final int shiftTolHybridization = 2;
                final int shiftTolDetection = 2;
                final int nThreads = 2;
                final List<Correlation> correlationList = requestTransfer.getCorrelations()
                                .getValues();
                // HYBRIDIZATION
                final Map<Integer, List<Integer>> detectedHybridizations = hybridizationDetection.detectHybridizations(
                                correlationList, requestTransfer.getMf(),
                                requestTransfer.getDetectionOptions()
                                                .getHybridizationDetectionThreshold(),
                                shiftTolHybridization);
                System.out.println("detectedHybridizations: "
                                + detectedHybridizations);
                // set hybridization of correlations from detection
                Set<Integer> hybridizationSet;
                for (final Map.Entry<Integer, List<Integer>> entry : detectedHybridizations.entrySet()) {
                        hybridizationSet = new HashSet<>(entry.getValue());
                        if (correlationList.get(entry.getKey())
                                        .getHybridization() != null) {
                                hybridizationSet.addAll(correlationList.get(entry.getKey())
                                                .getHybridization());
                        }
                        correlationList.get(entry.getKey())
                                        .setHybridization(new ArrayList<>(hybridizationSet));
                }

                // FRAGMENTS
                final List<DataSet> detectedFragments = requestTransfer.getDetectionOptions()
                                .isDetectFragments()
                                                ? fragmentsDetection.detect(
                                                                requestTransfer.getQuerySpectrum(),
                                                                correlationList, requestTransfer.getMf(),
                                                                requestTransfer.getDetectionOptions()
                                                                                .getShiftToleranceFragmentDetection(),
                                                                requestTransfer.getDetectionOptions()
                                                                                .getMaximumAverageDeviationFragmentDetection(),
                                                                nThreads)
                                                : new ArrayList<>();

                // HEAVY ATOM STATISTICS
                final Map<String, Integer> detectedHeavyAtomStatistics = HeavyAtomStatisticsDetection.detect(
                                heavyAtomStatisticsController,
                                requestTransfer.getMf());
                System.out.println("detectedHeavyAtomStatistics: "
                                + detectedHeavyAtomStatistics);
                responseTransfer.setElucidationOptions(requestTransfer.getElucidationOptions());
                responseTransfer.getElucidationOptions()
                                .setAllowHeteroHeteroBonds(
                                                HeavyAtomStatisticsDetection.checkAllowanceOfHeteroHeteroBonds(
                                                                heavyAtomStatisticsController,
                                                                requestTransfer.getMf(),
                                                                0.01));

                // DETECTIONS
                final Map<Integer, Map<String, Map<Integer, Set<Integer>>>> detectedOccurrenceForbidden = connectivityDetection
                                .detectByOccurrenceCounts(
                                                correlationList, shiftTolDetection,
                                                requestTransfer.getDetectionOptions()
                                                                .getLowerElementCountThreshold(),
                                                requestTransfer.getMf(), "lowerLimit");

                System.out.println("detectedOccurrenceForbidden: "
                                + detectedOccurrenceForbidden);
                final Map<Integer, Map<String, Map<Integer, Set<Integer>>>> detectedOccurrenceAllowed = connectivityDetection
                                .detectByOccurrenceCounts(
                                                correlationList, shiftTolDetection,
                                                requestTransfer.getDetectionOptions()
                                                                .getUpperElementCountThreshold(),
                                                requestTransfer.getMf(), "upperLimit");

                System.out.println("detectedOccurrenceAllowed: "
                                + detectedOccurrenceAllowed);

                final Map<Integer, Set<Integer>> fixedNeighbors = requestTransfer.getDetections() != null
                                && requestTransfer.getDetections()
                                                .getFixedNeighbors() != null
                                                                ? requestTransfer.getDetections()
                                                                                .getFixedNeighbors()
                                                                : new HashMap<>();
                final Map<Integer, Set<Integer>> fixedNeighborsByINADEQUATE = Utilities.buildFixedNeighborsByINADEQUATE(
                                correlationList);
                for (final Map.Entry<Integer, Set<Integer>> entry : fixedNeighborsByINADEQUATE.entrySet()) {
                        fixedNeighbors.putIfAbsent(entry.getKey(), new HashSet<>());
                        fixedNeighbors.get(entry.getKey())
                                        .addAll(entry.getValue());
                }
                System.out.println("fixedNeighbors: "
                                + fixedNeighbors);

                responseTransfer.setCorrelations(requestTransfer.getCorrelations());
                responseTransfer.setDetections(
                                new Detections(detectedHybridizations, new HashMap<>(), detectedOccurrenceForbidden,
                                                detectedOccurrenceAllowed, fixedNeighbors, detectedFragments));
                responseTransfer.setDetectionOptions(requestTransfer.getDetectionOptions());
                responseTransfer.setDetected(true);

                // // in case of no hetero-hetero bonds are allowed then reduce the
                // hybridization states and proton counts by carbon neighborhood statistics
                // if (responseTransfer.getDetectionOptions()
                // .isUseNeighborDetections()
                // && responseTransfer.getDetections()
                // != null
                // && !requestTransfer.getElucidationOptions()
                // .isAllowHeteroHeteroBonds()) {
                // Utilities.reduceDefaultHybridizationsAndProtonCountsOfHeteroAtoms(responseTransfer.getCorrelations()
                // .getValues(),
                // responseTransfer.getDetections()
                // .getDetectedConnectivities(),
                // responseTransfer.getDetections()
                // .getDetectedHybridizations());
                // }

                responseTransfer.setGrouping(detectGroups(responseTransfer.getCorrelations()));
                System.out.println("grouping: "
                                + responseTransfer.getGrouping());
                boolean multipleGroupMembersExist = false;
                for (final Map.Entry<String, Map<Integer, List<Integer>>> entryPerAtomType : responseTransfer
                                .getGrouping()
                                .getGroups()
                                .entrySet()) {
                        for (final Map.Entry<Integer, List<Integer>> entryPerGroup : entryPerAtomType.getValue()
                                        .entrySet()) {
                                if (entryPerGroup.getValue()
                                                .size() > 1) {
                                        responseTransfer.getElucidationOptions()
                                                        .setUseCombinatorics(true);
                                        multipleGroupMembersExist = true;
                                        break;
                                }
                                if (multipleGroupMembersExist) {
                                        break;
                                }
                        }
                }
                if (!multipleGroupMembersExist) {
                        responseTransfer.getElucidationOptions()
                                        .setUseCombinatorics(false);
                }

                return responseTransfer;
        }

        public Grouping detectGroups(final Correlations correlations) {
                return Utilities.buildGroups(correlations.getValues(), (Map<String, Double>) correlations.getOptions()
                                .get("tolerance"));
        }
}
