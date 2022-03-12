package org.openscience.sherlock.core.utils.detection;

import casekit.nmr.elucidation.Utilities;
import casekit.nmr.elucidation.model.Detections;
import casekit.nmr.elucidation.model.Grouping;
import casekit.nmr.model.nmrium.Correlation;
import casekit.nmr.model.nmrium.Correlations;
import org.openscience.sherlock.core.model.exchange.Transfer;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

public class Detection {

    public static Transfer detect(final WebClient.Builder webClientBuilder, final Transfer requestTransfer) {
        final Transfer responseTransfer = new Transfer();
        final int shiftTolHybridization = 2;
        final int shiftTolDetection = 2;
        final List<Correlation> correlationList = requestTransfer.getCorrelations()
                                                                 .getValues();
        // HYBRIDIZATION
        final Map<Integer, List<Integer>> detectedHybridizations = HybridizationDetection.detectHybridizations(
                webClientBuilder, correlationList, requestTransfer.getMf(), requestTransfer.getDetectionOptions()
                                                                                           .getHybridizationDetectionThreshold(),
                shiftTolHybridization);
        System.out.println("detectedHybridizations: "
                                   + detectedHybridizations);
        // set hybridization of correlations from detection
        for (final Map.Entry<Integer, List<Integer>> entry : detectedHybridizations.entrySet()) {
            correlationList.get(entry.getKey())
                           .setHybridization(entry.getValue());
        }
        // HEAVY ATOM STATISTICS
        final Map<String, Integer> detectedHeavyAtomStatistics = HeavyAtomStatisticsDetection.detect(webClientBuilder,
                                                                                                     requestTransfer.getMf());
        System.out.println("detectedHeavyAtomStatistics: "
                                   + detectedHeavyAtomStatistics);
        responseTransfer.setElucidationOptions(requestTransfer.getElucidationOptions());
        responseTransfer.getElucidationOptions()
                        .setAllowHeteroHeteroBonds(
                                HeavyAtomStatisticsDetection.checkAllowanceOfHeteroAtom(webClientBuilder,
                                                                                        requestTransfer.getMf(), 0.01));

        // DETECTIONS
        final Map<Integer, Map<String, Map<Integer, Set<Integer>>>> detectedOccurrenceForbidden = ConnectivityDetection.detectByOccurrenceCounts(
                webClientBuilder, correlationList, shiftTolDetection, requestTransfer.getDetectionOptions()
                                                                                     .getLowerElementCountThreshold(),
                requestTransfer.getMf(), "lowerLimit");

        System.out.println("detectedOccurrenceForbidden: "
                                   + detectedOccurrenceForbidden);
        final Map<Integer, Map<String, Map<Integer, Set<Integer>>>> detectedOccurrenceAllowed = ConnectivityDetection.detectByOccurrenceCounts(
                webClientBuilder, correlationList, shiftTolDetection, requestTransfer.getDetectionOptions()
                                                                                     .getUpperElementCountThreshold(),
                requestTransfer.getMf(), "upperLimit");

        System.out.println("detectedOccurrenceAllowed: "
                                   + detectedOccurrenceAllowed);

        final Map<Integer, Set<Integer>> fixedNeighbors = requestTransfer.getDetections()
                                                                  != null
                                                                  && requestTransfer.getDetections()
                                                                                    .getFixedNeighbors()
                != null
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
                               detectedOccurrenceAllowed,//detectedConnectivityCounts, forbiddenNeighbors, setNeighbors,
                               fixedNeighbors));
        responseTransfer.setDetectionOptions(requestTransfer.getDetectionOptions());

        //        // in case of no hetero hetero bonds are allowed then reduce the hybridization states and proton counts by carbon neighborhood statistics
        //        if (responseTransfer.getDetectionOptions()
        //                            .isUseNeighborDetections()
        //                && responseTransfer.getDetections()
        //                != null
        //                && !requestTransfer.getElucidationOptions()
        //                                   .isAllowHeteroHeteroBonds()) {
        //            Utilities.reduceDefaultHybridizationsAndProtonCountsOfHeteroAtoms(responseTransfer.getCorrelations()
        //                                                                                              .getValues(),
        //                                                                              responseTransfer.getDetections()
        //                                                                                              .getDetectedConnectivities(),
        //                                                                              responseTransfer.getDetections()
        //                                                                                              .getDetectedHybridizations());
        //        }


        responseTransfer.setGrouping(detectGroups(responseTransfer.getCorrelations()));
        System.out.println("grouping: "
                                   + responseTransfer.getGrouping());
        boolean multipleGroupMembersExist = false;
        for (final Map.Entry<String, Map<Integer, List<Integer>>> entryPerAtomType : responseTransfer.getGrouping()
                                                                                                     .getGroups()
                                                                                                     .entrySet()) {
            for (final Map.Entry<Integer, List<Integer>> entryPerGroup : entryPerAtomType.getValue()
                                                                                         .entrySet()) {
                if (entryPerGroup.getValue()
                                 .size()
                        > 1) {
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

    public static Grouping detectGroups(final Correlations correlations) {
        return Utilities.buildGroups(correlations.getValues(), (Map<String, Double>) correlations.getOptions()
                                                                                                 .get("tolerance"));
    }
}
