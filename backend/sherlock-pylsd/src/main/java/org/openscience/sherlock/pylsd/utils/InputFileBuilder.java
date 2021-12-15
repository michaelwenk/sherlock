package org.openscience.sherlock.pylsd.utils;

import casekit.nmr.lsd.PyLSDInputFileBuilder;
import casekit.nmr.lsd.Utilities;
import casekit.nmr.lsd.model.Detections;
import org.openscience.sherlock.pylsd.model.exchange.Transfer;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InputFileBuilder {

    public static Transfer createPyLSDInputFile(final WebClient.Builder webClientBuilder,
                                                final Transfer requestTransfer) {

        System.out.println("-> detected data was already given: "
                                   + (requestTransfer.getDetections()
                != null));
        System.out.println(requestTransfer.getDetections());

        Detections newDetections = requestTransfer.getDetections();
        if (newDetections
                == null) {
            newDetections = new Detections(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(),
                                           requestTransfer.getDetections()
                                                   != null
                                                   && requestTransfer.getDetections()
                                                                     .getFixedNeighbors()
                                                   != null
                                           ? requestTransfer.getDetections()
                                                            .getFixedNeighbors()
                                           : new HashMap<>());
        }
        requestTransfer.setDetections(newDetections);

        // @TODO remove following hybridization replacements as soon as the frontend stores the same information into the NMRium data
        if (requestTransfer.getDetectionOptions()
                           .isUseHybridizationDetections()
                && requestTransfer.getDetections()
                != null) {
            // set hybridization of correlations from previous detection
            for (final Map.Entry<Integer, List<Integer>> entry : requestTransfer.getDetections()
                                                                                .getDetectedHybridizations()
                                                                                .entrySet()) {
                requestTransfer.getCorrelations()
                               .getValues()
                               .get(entry.getKey())
                               .setHybridization(entry.getValue());
            }
        }

        // in case of no hetero hetero bonds are allowed then reduce the hybridization states and proton counts by carbon neighborhood statistics
        if (requestTransfer.getDetectionOptions()
                           .isUseNeighborDetections()
                && requestTransfer.getDetections()
                != null
                && !requestTransfer.getElucidationOptions()
                                   .isAllowHeteroHeteroBonds()) {
            Utilities.reduceDefaultHybridizationsAndProtonCountsOfHeteroAtoms(requestTransfer.getCorrelations()
                                                                                             .getValues(),
                                                                              requestTransfer.getDetections()
                                                                                             .getDetectedConnectivities());
        }

        // add (custom) filters to elucidation options
        final String pathToFilterRing3 = "/data/lsd/PyLSD/LSD/Filters/ring3";
        final String pathToFilterRing4 = "/data/lsd/PyLSD/LSD/Filters/ring4";
        final Path pathToCustomFilters = Paths.get("/data/lsd/filters/");
        List<String> filterList = new ArrayList<>();
        try {
            filterList = Files.walk(pathToCustomFilters)
                              .filter(path -> !Files.isDirectory(path))
                              .map(path -> path.toFile()
                                               .getAbsolutePath())
                              .collect(Collectors.toList());
        } catch (final IOException e) {
            e.printStackTrace();
        }
        if (requestTransfer.getElucidationOptions()
                           .isUseFilterLsdRing3()) {
            filterList.add(pathToFilterRing3);
        }
        if (requestTransfer.getElucidationOptions()
                           .isUseFilterLsdRing4()) {
            filterList.add(pathToFilterRing4);
        }
        requestTransfer.getElucidationOptions()
                       .setFilterPaths(filterList.toArray(String[]::new));


        final Detections detectionsToUse = new Detections(new HashMap<>(), new HashMap<>(), new HashMap<>(),
                                                          new HashMap<>(), requestTransfer.getDetections()
                                                                                   != null
                                                                                   && requestTransfer.getDetections()
                                                                                                     .getFixedNeighbors()
                != null
                                                                           ? requestTransfer.getDetections()
                                                                                            .getFixedNeighbors()
                                                                           : new HashMap<>());
        if (requestTransfer.getDetectionOptions()
                           .isUseHybridizationDetections()) {
            detectionsToUse.setDetectedHybridizations(requestTransfer.getDetections()
                                                                     .getDetectedHybridizations());
        }
        if (requestTransfer.getDetectionOptions()
                           .isUseNeighborDetections()) {
            detectionsToUse.setDetectedConnectivities(requestTransfer.getDetections()
                                                                     .getDetectedConnectivities());
            detectionsToUse.setForbiddenNeighbors(requestTransfer.getDetections()
                                                                 .getForbiddenNeighbors());
            detectionsToUse.setSetNeighbors(requestTransfer.getDetections()
                                                           .getSetNeighbors());
        }

        requestTransfer.setPyLSDInputFileContent(
                PyLSDInputFileBuilder.buildPyLSDInputFileContent(requestTransfer.getCorrelations(),
                                                                 requestTransfer.getMf(), detectionsToUse,
                                                                 requestTransfer.getElucidationOptions()));
        return requestTransfer;
    }
}
