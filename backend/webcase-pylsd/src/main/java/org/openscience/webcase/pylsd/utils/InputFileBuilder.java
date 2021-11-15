package org.openscience.webcase.pylsd.utils;

import casekit.nmr.lsd.PyLSDInputFileBuilder;
import casekit.nmr.lsd.Utilities;
import casekit.nmr.model.nmrium.Correlation;
import org.openscience.webcase.pylsd.model.Detections;
import org.openscience.webcase.pylsd.model.exchange.Transfer;
import org.openscience.webcase.pylsd.utils.detection.Detection;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InputFileBuilder {

    public static Transfer createPyLSDInputFile(final WebClient.Builder webClientBuilder,
                                                final Transfer requestTransfer) {

        System.out.println("-> detected data was already given: "
                                   + (requestTransfer.getDetections()
                != null)
                                   + " -> "
                                   + requestTransfer.getDetections());
        System.out.println(requestTransfer.getDetections());
        final Detections detections = requestTransfer.getDetections()
                                              != null
                                      ? requestTransfer.getDetections()
                                      : Detection.detect(webClientBuilder, requestTransfer)
                                                 .getDetections();

        // in case of no hetero hetero bonds are allowed then reduce the hybridization states and proton counts by carbon neighborhood statistics
        if (!requestTransfer.getElucidationOptions()
                            .isAllowHeteroHeteroBonds()) {
            final List<Correlation> correlationList = requestTransfer.getData()
                                                                     .getCorrelations()
                                                                     .getValues();
            Utilities.reduceDefaultHybridizationsAndProtonCountsOfHeteroAtoms(correlationList,
                                                                              detections.getDetectedConnectivities());
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


        final Transfer responseTransfer = new Transfer();
        responseTransfer.setDetections(detections);
        responseTransfer.setPyLSDInputFileContent(
                PyLSDInputFileBuilder.buildPyLSDInputFileContent(requestTransfer.getData(), requestTransfer.getMf(),
                                                                 detections.getDetectedHybridizations(),
                                                                 detections.getDetectedConnectivities(),
                                                                 detections.getForbiddenNeighbors(),
                                                                 detections.getSetNeighbors(),
                                                                 requestTransfer.getElucidationOptions()));
        return responseTransfer;
    }
}
