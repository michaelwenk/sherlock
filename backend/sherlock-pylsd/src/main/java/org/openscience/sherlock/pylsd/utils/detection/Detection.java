package org.openscience.sherlock.pylsd.utils.detection;

import casekit.nmr.elucidation.Utilities;
import casekit.nmr.elucidation.model.Detections;
import casekit.nmr.elucidation.model.Grouping;
import casekit.nmr.model.nmrium.Correlation;
import casekit.nmr.model.nmrium.Correlations;
import org.openscience.sherlock.pylsd.model.exchange.Transfer;
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
        //        final Map<Integer, Map<String, Map<Integer, Set<Integer>>>> detectedConnectivityCounts = ConnectivityDetection.detectByConnectivityCounts(
        //                webClientBuilder, correlationList, shiftTolDetection, requestTransfer.getDetectionOptions()
        //                                                                                     .getLowerElementCountThreshold(),
        //                requestTransfer.getMf(), false);
        //
        //        System.out.println("detectedConnectivityCounts: "
        //                                   + detectedConnectivityCounts);
        //
        //        final Map<Integer, Map<String, Map<Integer, Set<Integer>>>> forbiddenNeighbors = ForbiddenNeighborDetection.detectForbiddenNeighbors(
        //                detectedConnectivityCounts, requestTransfer.getMf());
        //        reduce(forbiddenNeighbors);
        //
        //        final Map<Integer, Map<String, Map<Integer, Set<Integer>>>> setNeighbors = ConnectivityDetection.detectByConnectivityCounts(
        //                webClientBuilder, correlationList, shiftTolDetection, requestTransfer.getDetectionOptions()
        //                                                                                     .getUpperElementCountThreshold(),
        //                requestTransfer.getMf(), false);
        //        reduce(setNeighbors);
        //        // remove carbons from forbidden/set neighbors list
        //        //        for (final Map.Entry<Integer, Map<String, Map<Integer, Set<Integer>>>> entryPerCorrelation : forbiddenNeighbors.entrySet()) {
        //        //            entryPerCorrelation.getValue()
        //        //                               .remove("C");
        //        //        }
        //        //        for (final Map.Entry<Integer, Map<String, Map<Integer, Set<Integer>>>> entryPerCorrelation : setNeighbors.entrySet()) {
        //        //            entryPerCorrelation.getValue()
        //        //                               .remove("C");
        //        //        }
        //
        //
        //        // @TODO for now: avoid different neighbor hybridizations
        //        simplifyHybridizations(forbiddenNeighbors);
        //        simplifyHybridizations(setNeighbors);
        //        System.out.println("-> forbiddenNeighbors: "
        //                                   + forbiddenNeighbors);
        //        System.out.println("-> setNeighbors: "
        //                                   + setNeighbors);

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

        return responseTransfer;
    }


    //    private static void reduce(final Map<Integer, Map<String, Map<Integer, Set<Integer>>>> neighbors) {
    //        for (final Map.Entry<Integer, Map<String, Map<Integer, Set<Integer>>>> entryPerCorrelation : neighbors.entrySet()) {
    //            for (final Map.Entry<String, Map<Integer, Set<Integer>>> entryPerAtomType : entryPerCorrelation.getValue()
    //                                                                                                           .entrySet()) {
    //                final Set<Integer> defaultHybridizations = Arrays.stream(
    //                                                                         Constants.defaultHybridizationMap.get(entryPerAtomType.getKey()))
    //                                                                 .boxed()
    //                                                                 .collect(Collectors.toSet());
    //                if (entryPerAtomType.getValue()
    //                                    .keySet()
    //                                    .containsAll(defaultHybridizations)) {
    //                    final Set<Integer> defaultProtonsCounts = Arrays.stream(
    //                                                                            Constants.defaultProtonsCountPerValencyMap.get(entryPerAtomType.getKey()))
    //                                                                    .boxed()
    //                                                                    .collect(Collectors.toSet());
    //                    for (final int protonsCount : defaultProtonsCounts) {
    //                        boolean foundInAllHybridizations = true;
    //                        for (final Map.Entry<Integer, Set<Integer>> entryPerHybridization : entryPerAtomType.getValue()
    //                                                                                                            .entrySet()) {
    //                            if (entryPerHybridization.getKey()
    //                                    != -1
    //                                    && !entryPerHybridization.getValue()
    //                                                             .contains(protonsCount)) {
    //                                foundInAllHybridizations = false;
    //                                break;
    //                            }
    //                        }
    //                        if (foundInAllHybridizations) {
    //                            // remove protonsCount from hybridization
    //                            for (final Map.Entry<Integer, Set<Integer>> entryPerHybridization : entryPerAtomType.getValue()
    //                                                                                                                .entrySet()) {
    //                                if (entryPerHybridization.getKey()
    //                                        != -1) {
    //                                    entryPerHybridization.getValue()
    //                                                         .remove(protonsCount);
    //                                }
    //                            }
    //                            // add protonsCount to -1 which means all hybridization states
    //                            entryPerAtomType.getValue()
    //                                            .putIfAbsent(-1, new HashSet<>());
    //                            entryPerAtomType.getValue()
    //                                            .get(-1)
    //                                            .add(protonsCount);
    //                        }
    //                    }
    //                }
    //                final Set<Integer> hybridizationsToRemove = new HashSet<>();
    //                for (final Map.Entry<Integer, Set<Integer>> entryPerHybridization : entryPerAtomType.getValue()
    //                                                                                                    .entrySet()) {
    //                    if (entryPerHybridization.getValue()
    //                                             .isEmpty()) {
    //                        hybridizationsToRemove.add(entryPerHybridization.getKey());
    //                    }
    //                }
    //                for (final int hybrid : hybridizationsToRemove) {
    //                    entryPerAtomType.getValue()
    //                                    .remove(hybrid);
    //                }
    //            }
    //        }
    //    }
    //
    //    private static void simplifyHybridizations(final Map<Integer, Map<String, Map<Integer, Set<Integer>>>> neighbors) {
    //        for (final Map.Entry<Integer, Map<String, Map<Integer, Set<Integer>>>> entryPerCorrelation : neighbors.entrySet()) {
    //            for (final Map.Entry<String, Map<Integer, Set<Integer>>> entryPerNeighborAtom : entryPerCorrelation.getValue()
    //                                                                                                               .entrySet()) {
    //                for (final int neighborHybridization : new ArrayList<>(entryPerNeighborAtom.getValue()
    //                                                                                           .keySet())) {
    //                    if (neighborHybridization
    //                            != -1) {
    //                        entryPerNeighborAtom.getValue()
    //                                            .putIfAbsent(-1, new HashSet<>());
    //                        entryPerNeighborAtom.getValue()
    //                                            .get(-1)
    //                                            .addAll(entryPerNeighborAtom.getValue()
    //                                                                        .get(neighborHybridization));
    //                        entryPerNeighborAtom.getValue()
    //                                            .remove(neighborHybridization);
    //                    }
    //                }
    //            }
    //        }
    //    }

    public static Grouping detectGroups(final Correlations correlations) {
        return Utilities.buildGroups(correlations.getValues(), (Map<String, Double>) correlations.getOptions()
                                                                                                 .get("tolerance"));
    }
}
