package org.openscience.sherlock.pylsd.utils;

import casekit.nmr.lsd.PyLSDInputFileBuilder;
import casekit.nmr.lsd.Utilities;
import org.openscience.sherlock.pylsd.model.exchange.Transfer;
import org.openscience.sherlock.pylsd.utils.detection.Detection;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
        final Transfer responseTransferByDetection = requestTransfer.getDetections()
                                                             != null
                                                     ? requestTransfer
                                                     : Detection.detect(webClientBuilder, requestTransfer);
        final Transfer responseTransfer = new Transfer();
        responseTransfer.setData(responseTransferByDetection.getData());
        responseTransfer.setDetections(responseTransferByDetection.getDetections());
        responseTransfer.setMf(requestTransfer.getMf());
        responseTransfer.setElucidationOptions(requestTransfer.getElucidationOptions());

        // @TODO remove following hybridization replacements as soon as the frontend stores the same information into the NMRium data
        if (requestTransfer.getDetections()
                != null) {
            // set hybridization of correlations from previous detection
            for (final Map.Entry<Integer, List<Integer>> entry : requestTransfer.getDetections()
                                                                                .getDetectedHybridizations()
                                                                                .entrySet()) {
                responseTransfer.getData()
                                .getCorrelations()
                                .getValues()
                                .get(entry.getKey())
                                .setHybridization(entry.getValue());
            }
        }

        // in case of no hetero hetero bonds are allowed then reduce the hybridization states and proton counts by carbon neighborhood statistics
        if (!responseTransfer.getElucidationOptions()
                             .isAllowHeteroHeteroBonds()) {
            Utilities.reduceDefaultHybridizationsAndProtonCountsOfHeteroAtoms(responseTransfer.getData()
                                                                                              .getCorrelations()
                                                                                              .getValues(),
                                                                              responseTransfer.getDetections()
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
        if (responseTransfer.getElucidationOptions()
                            .isUseFilterLsdRing3()) {
            filterList.add(pathToFilterRing3);
        }
        if (responseTransfer.getElucidationOptions()
                            .isUseFilterLsdRing4()) {
            filterList.add(pathToFilterRing4);
        }
        responseTransfer.getElucidationOptions()
                        .setFilterPaths(filterList.toArray(String[]::new));


        responseTransfer.setPyLSDInputFileContent(
                PyLSDInputFileBuilder.buildPyLSDInputFileContent(responseTransfer.getData(), responseTransfer.getMf(),
                                                                 responseTransfer.getDetections()
                                                                                 .getDetectedHybridizations(),
                                                                 responseTransfer.getDetections()
                                                                                 .getDetectedConnectivities(),
                                                                 responseTransfer.getDetections()
                                                                                 .getForbiddenNeighbors(),
                                                                 responseTransfer.getDetections()
                                                                                 .getSetNeighbors(),
                                                                 responseTransfer.getDetections()
                                                                                 .getFixedNeighbors(),
                                                                 responseTransfer.getElucidationOptions()));
        return responseTransfer;
    }
}
