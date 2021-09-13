package org.openscience.webcase.pylsd.controller;

import casekit.io.FileSystem;
import casekit.nmr.lsd.PyLSDInputFileBuilder;
import casekit.nmr.lsd.Utilities;
import casekit.nmr.lsd.model.ElucidationOptions;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.nmrium.Correlation;
import org.openscience.webcase.pylsd.model.exchange.Transfer;
import org.openscience.webcase.pylsd.utils.ConnectivityDetection;
import org.openscience.webcase.pylsd.utils.HybridizationDetection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/")
public class PyLSDController {
    final String pathToPyLSDExecutableFolder = "/data/lsd/PyLSD/Variant/";
    final String pathToPyLSDInputFileFolder = "/data/lsd/PyLSD/Variant/";
    final String pathToPyLSDResultFileFolder = "/data/lsd/PyLSD/Variant/";

    // set ExchangeSettings
    final int maxInMemorySizeMB = 1000;
    final ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                                                                    .codecs(configurer -> configurer.defaultCodecs()
                                                                                                    .maxInMemorySize(
                                                                                                            this.maxInMemorySizeMB
                                                                                                                    * 1024
                                                                                                                    * 1024))
                                                                    .build();

    private final WebClient.Builder webClientBuilder;

    @Autowired
    public PyLSDController(final WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }


    @PostMapping(value = "/runPyLSD")
    public ResponseEntity<Transfer> runPyLSD(@RequestBody final Transfer requestTransfer) {
        final Transfer responseTransfer = new Transfer();

        // build PyLSD input file
        final String pyLSDInputFileContent = this.createPyLSDInputFile(requestTransfer);
        System.out.println("file content:\n"
                                   + pyLSDInputFileContent);
        final String pathToPyLSDInputFile = this.pathToPyLSDInputFileFolder
                + requestTransfer.getRequestID()
                + ".pylsd";

        // run PyLSD if file was written successfully
        if (FileSystem.writeFile(pathToPyLSDInputFile, pyLSDInputFileContent)) {
            System.out.println("--> has been written successfully: "
                                       + pathToPyLSDInputFile);
            try {
                // try to execute PyLSD
                final ProcessBuilder builder = new ProcessBuilder();
                builder.directory(new File(this.pathToPyLSDExecutableFolder))
                       .redirectError(new File(this.pathToPyLSDInputFileFolder
                                                       + requestTransfer.getRequestID()
                                                       + "_error.txt"))
                       .redirectOutput(new File(this.pathToPyLSDInputFileFolder
                                                        + requestTransfer.getRequestID()
                                                        + "_log.txt"))
                       .command("python2.7", this.pathToPyLSDExecutableFolder
                               + "lsd.py", pathToPyLSDInputFile);
                final Process process = builder.start();
                final boolean pyLSDRunWasSuccessful = process.waitFor(requestTransfer.getElucidationOptions()
                                                                                     .getTimeLimitTotal(),
                                                                      TimeUnit.MINUTES);
                if (pyLSDRunWasSuccessful) {
                    System.out.println("-> run was successful");
                    final String pathToResultsFile = this.pathToPyLSDResultFileFolder
                            + requestTransfer.getRequestID()
                            + "_0.smiles";
                    System.out.println(pathToResultsFile);

                    final List<DataSet> dataSetList = this.retrieveAndRankResultsFromPyLSDOutputFile(pathToResultsFile,
                                                                                                     requestTransfer);
                    System.out.println("--> number of parsed and ranked structures: "
                                               + dataSetList.size());


                    System.out.println("--> number of results: "
                                               + dataSetList.size());
                    responseTransfer.setDataSetList(dataSetList);
                } else {
                    System.out.println("run was NOT successful");
                }
            } catch (final Exception e) {
                e.printStackTrace();
                responseTransfer.setPyLSDRunWasSuccessful(false);
            }
            // cleanup of created files and folder
            final String[] directoriesToCheck = new String[]{this.pathToPyLSDInputFileFolder,
                                                             this.pathToPyLSDResultFileFolder};
            System.out.println("cleaned ? -> "
                                       + FileSystem.cleanup(directoriesToCheck, requestTransfer.getRequestID()));
        } else {
            System.out.println("--> file creation failed: "
                                       + pathToPyLSDInputFile);
        }

        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
    }

    private String createPyLSDInputFile(final Transfer requestTransfer) {
        final int shiftTol = 0;
        final List<Correlation> correlationList = requestTransfer.getData()
                                                                 .getCorrelations()
                                                                 .getValues();
        final Map<Integer, List<Integer>> detectedHybridizations = HybridizationDetection.detectHybridizations(
                this.webClientBuilder, correlationList, requestTransfer.getElucidationOptions()
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
                this.webClientBuilder, correlationList, shiftTol, requestTransfer.getElucidationOptions()
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

        return this.createInputFile(queryTransfer);
    }

    private String createInputFile(final Transfer requestTransfer) {
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

        return PyLSDInputFileBuilder.buildPyLSDInputFileContent(requestTransfer.getData(), requestTransfer.getMf(),
                                                                requestTransfer.getDetectedHybridizations(),
                                                                requestTransfer.getDetectedConnectivities(),
                                                                elucidationOptions);
    }


    private List<DataSet> retrieveAndRankResultsFromPyLSDOutputFile(final String pathToResultsFile,
                                                                    final Transfer requestTransfer) {
        final BufferedReader bufferedReader = FileSystem.readFile(pathToResultsFile);
        if (bufferedReader
                == null) {
            System.out.println("retrieveAndRankResultsFromPyLSDOutputFile: could not read file \""
                                       + pathToResultsFile
                                       + "\"");
            return new ArrayList<>();
        }
        final WebClient webClient = this.webClientBuilder.baseUrl(
                "http://webcase-gateway:8080/webcase-db-service-hosecode/fileParser/parseResultFile")
                                                         .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                        MediaType.APPLICATION_JSON_VALUE)
                                                         .exchangeStrategies(this.exchangeStrategies)
                                                         .build();
        final String fileContent = bufferedReader.lines()
                                                 .collect(Collectors.joining("\n"));
        final Transfer queryTransfer = new Transfer();
        queryTransfer.setFileContent(fileContent);
        queryTransfer.setData(requestTransfer.getData());
        queryTransfer.setElucidationOptions(requestTransfer.getElucidationOptions());
        return webClient.post()
                        .bodyValue(queryTransfer)
                        .retrieve()
                        .bodyToMono(Transfer.class)
                        .block()
                        .getDataSetList();
    }
}
