package org.openscience.sherlock.pylsd.utils;

import casekit.nmr.lsd.inputfile.PyLSDInputFileBuilder;
import casekit.nmr.lsd.model.Detections;
import casekit.nmr.lsd.model.Grouping;
import casekit.nmr.model.nmrium.Correlation;
import org.openscience.sherlock.pylsd.model.exchange.Transfer;
import org.openscience.sherlock.pylsd.utils.detection.Detection;
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
        System.out.println("-> detected data was already given?: "
                                   + (requestTransfer.getDetections()
                != null));
        System.out.println(requestTransfer.getDetections());

        if (requestTransfer.getDetections()
                == null) {
            final Transfer detectionTransfer = Detection.detect(webClientBuilder, requestTransfer);
            requestTransfer.setDetections(detectionTransfer.getDetections());
            System.out.println(" -> new detections: "
                                       + requestTransfer.getDetections());
        }
        System.out.println("-> grouping was already given?: "
                                   + (requestTransfer.getGrouping()
                != null));
        System.out.println(requestTransfer.getGrouping());
        if (requestTransfer.getGrouping()
                == null) {
            requestTransfer.setGrouping(Detection.detectGroups(requestTransfer.getCorrelations()));
            System.out.println(" -> new grouping: "
                                       + requestTransfer.getGrouping());
        }

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
        } else {
            for (final Correlation correlation : requestTransfer.getCorrelations()
                                                                .getValues()) {
                correlation.setHybridization(new ArrayList<>());
            }
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
                                                                 requestTransfer.getElucidationOptions()
                                                                                .isUseCombinatorics()
                                                                 ? requestTransfer.getGrouping()
                                                                 : new Grouping(new HashMap<>(), new HashMap<>(),
                                                                                new HashMap<>()),
                                                                 requestTransfer.getElucidationOptions()));
        return requestTransfer;
    }
}
