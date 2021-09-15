package org.openscience.webcase.pylsd.utils;

import casekit.nmr.lsd.Utilities;
import casekit.nmr.lsd.model.ElucidationOptions;
import casekit.nmr.model.nmrium.Correlation;
import org.openscience.webcase.pylsd.model.exchange.Transfer;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PyLSDInputFileBuilder {

    public static String createPyLSDInputFile(final WebClient.Builder webClientBuilder,
                                              final Transfer requestTransfer) {
        final int shiftTol = 0;
        final List<Correlation> correlationList = requestTransfer.getData()
                                                                 .getCorrelations()
                                                                 .getValues();
        final Map<Integer, List<Integer>> detectedHybridizations = HybridizationDetection.detectHybridizations(
                webClientBuilder, correlationList, requestTransfer.getElucidationOptions()
                                                                  .getHybridizationDetectionThreshold(), shiftTol);
        // set hybridization of correlations from detection if there was nothing set before
        for (final Map.Entry<Integer, List<Integer>> entry : detectedHybridizations.entrySet()) {
            if (correlationList.get(entry.getKey())
                               .getHybridization()
                    == null
                    || correlationList.get(entry.getKey())
                                      .getHybridization()
                                      .isEmpty()) {
                correlationList.get(entry.getKey())
                               .setHybridization(new ArrayList<>(entry.getValue()
                                                                      .stream()
                                                                      .map(numericHybridization -> "SP"
                                                                              + numericHybridization)
                                                                      .collect(Collectors.toList())));
            }
        }

        final Map<Integer, Map<String, Map<String, Set<Integer>>>> detectedConnectivities = ConnectivityDetection.detectConnectivities(
                webClientBuilder, correlationList, shiftTol, requestTransfer.getElucidationOptions()
                                                                            .getHybridizationCountThreshold(),
                requestTransfer.getElucidationOptions()
                               .getProtonsCountThreshold(), requestTransfer.getMf());

        System.out.println("detectedConnectivities: "
                                   + detectedConnectivities);
        System.out.println("allowedNeighborAtomHybridizations: "
                                   + Utilities.buildAllowedNeighborAtomHybridizations(correlationList,
                                                                                      detectedConnectivities));
        System.out.println("allowedNeighborAtomProtonCounts: "
                                   + Utilities.buildAllowedNeighborAtomProtonCounts(correlationList,
                                                                                    detectedConnectivities));

        // in case of no hetero hetero bonds are allowed then reduce the hybridization states and proton counts by carbon neighborhood statistics
        if (!requestTransfer.getElucidationOptions()
                            .isAllowHeteroHeteroBonds()) {
            Utilities.reduceDefaultHybridizationsAndProtonCountsOfHeteroAtoms(correlationList, detectedConnectivities);
        }

        final Transfer queryTransfer = new Transfer();
        queryTransfer.setData(requestTransfer.getData());
        queryTransfer.setDetectedHybridizations(detectedHybridizations);
        queryTransfer.setDetectedConnectivities(detectedConnectivities);
        queryTransfer.setMf(requestTransfer.getMf());
        queryTransfer.setElucidationOptions(requestTransfer.getElucidationOptions());

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
        queryTransfer.getElucidationOptions()
                     .setFilterPaths(filterList.toArray(String[]::new));

        return PyLSDInputFileBuilder.createInputFile(queryTransfer);
    }

    public static String createInputFile(final Transfer requestTransfer) {
        final ElucidationOptions elucidationOptions = new ElucidationOptions();

        elucidationOptions.setFilterPaths(requestTransfer.getElucidationOptions()
                                                         .getFilterPaths());
        elucidationOptions.setAllowHeteroHeteroBonds(requestTransfer.getElucidationOptions()
                                                                    .isAllowHeteroHeteroBonds());
        elucidationOptions.setUseElim(requestTransfer.getElucidationOptions()
                                                     .isUseElim());
        elucidationOptions.setElimP1(requestTransfer.getElucidationOptions()
                                                    .getElimP1());
        elucidationOptions.setElimP2(requestTransfer.getElucidationOptions()
                                                    .getElimP2());
        elucidationOptions.setHmbcP3(requestTransfer.getElucidationOptions()
                                                    .getHmbcP3());
        elucidationOptions.setHmbcP4(requestTransfer.getElucidationOptions()
                                                    .getHmbcP4());
        elucidationOptions.setCosyP3(requestTransfer.getElucidationOptions()
                                                    .getCosyP3());
        elucidationOptions.setCosyP4(requestTransfer.getElucidationOptions()
                                                    .getCosyP4());

        return casekit.nmr.lsd.PyLSDInputFileBuilder.buildPyLSDInputFileContent(requestTransfer.getData(),
                                                                                requestTransfer.getMf(),
                                                                                requestTransfer.getDetectedHybridizations(),
                                                                                requestTransfer.getDetectedConnectivities(),
                                                                                elucidationOptions);
    }
}
