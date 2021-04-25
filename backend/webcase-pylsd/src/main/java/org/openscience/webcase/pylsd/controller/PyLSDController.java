package org.openscience.webcase.pylsd.controller;

import org.openscience.webcase.pylsd.model.DataSet;
import org.openscience.webcase.pylsd.model.exchange.Transfer;
import org.openscience.webcase.pylsd.utils.FileSystem;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/")
public class PyLSDController {
    final String pathToPyLSDExecutableFolder = "/data/lsd/PyLSD/Variant/";
    final String pathToPyLSDInputFileFolder = "/data/lsd/PyLSD/Variant/";
    final String pathToPyLSDResultFileFolder = "/data/lsd/PyLSD/Variant/"; //"/data/lsd/PyLSD/Predict/";

    // set ExchangeSettings
    final int maxInMemorySizeMB = 1000;
    final ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                                                                    .codecs(configurer -> configurer.defaultCodecs()
                                                                                                    .maxInMemorySize(
                                                                                                            this.maxInMemorySizeMB
                                                                                                                    * 1024
                                                                                                                    * 1024))
                                                                    .build();

    @Autowired
    private WebClient.Builder webClientBuilder;

    @PostMapping(value = "createPyLSDInputFile", consumes = "application/json")
    public String createPyLSDInputFile(@RequestBody final Transfer requestTransfer) {
        final Map<Integer, List<Integer>> detectedHybridizations = HybridizationDetection.getDetectedHybridizations(
                this.webClientBuilder, requestTransfer.getData(), requestTransfer.getElucidationOptions()
                                                                                 .getHybridizationDetectionThreshold());
        final WebClient webClient = this.webClientBuilder.
                                                                 baseUrl("http://webcase-gateway:8081/webcase-casekit/pylsd/createInputFile")
                                                         .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                        MediaType.APPLICATION_JSON_VALUE)
                                                         .build();

        final Transfer queryTransfer = new Transfer();
        queryTransfer.setData(requestTransfer.getData());
        queryTransfer.setDetectedHybridizations(detectedHybridizations);
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

        return webClient.post()
                        .bodyValue(queryTransfer)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
    }

    @PostMapping(value = "/runPyLSD")
    public ResponseEntity<Transfer> runPyLSD(@RequestBody final Transfer requestTransfer) {
        final Transfer responseTransfer = new Transfer();

        // build PyLSD input file
        final String pyLSDInputFileContent = this.createPyLSDInputFile(requestTransfer);
        final String pathToPyLSDInputFile = this.pathToPyLSDInputFileFolder
                + requestTransfer.getRequestID()
                + ".pylsd";
        final String pathToRankedSDFile = this.pathToPyLSDResultFileFolder
                + requestTransfer.getRequestID()
                + "_D.sdf";

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
                final int exitCode = process.waitFor();
                final boolean pyLSDRunWasSuccessful = exitCode
                        == 0;

                if (pyLSDRunWasSuccessful) {
                    System.out.println("-> run was successful");
                    final String pathToResultsFilePredictions = this.pathToPyLSDResultFileFolder
                            + requestTransfer.getRequestID()
                            + "_D.sdf";
                    System.out.println(pathToResultsFilePredictions);

                    final List<DataSet> dataSetList = this.retrieveResultFromRankedSDFile(pathToRankedSDFile, "13C");


                    System.out.println("--> number of results: "
                                               + dataSetList.size());
                    responseTransfer.setDataSetList(dataSetList);

                    // store results in DB if not empty
                    if (!responseTransfer.getDataSetList()
                                         .isEmpty()) {
                        final WebClient webClient = this.webClientBuilder.baseUrl(
                                "http://webcase-gateway:8081/webcase-result/store/storeResult")
                                                                         .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                                        MediaType.APPLICATION_JSON_VALUE)
                                                                         .exchangeStrategies(this.exchangeStrategies)
                                                                         .build();
                        final Transfer queryResultTransfer = webClient.post()
                                                                      .bodyValue(responseTransfer)
                                                                      .retrieve()
                                                                      .bodyToMono(Transfer.class)
                                                                      .block();
                        if (queryResultTransfer.getResultID()
                                != null) {
                            System.out.println("resultID: "
                                                       + queryResultTransfer.getResultID());
                            responseTransfer.setResultID(queryResultTransfer.getResultID());
                        }
                    }

                    //                    // cleanup of created files and folder
                    //                    final String[] directoriesToCheck = new String[]{this.pathToPyLSDInputFileFolder,
                    //                                                                     this.pathToPyLSDResultFileFolder};
                    //                    FileSystem.cleanup(directoriesToCheck, requestTransfer.getRequestID());
                } else {
                    System.out.println("run was NOT successful");
                }
            } catch (final Exception e) {
                e.printStackTrace();
                responseTransfer.setPyLSDRunWasSuccessful(false);
            }
        } else {
            System.out.println("--> file creation failed: "
                                       + pathToPyLSDInputFile);
        }

        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
    }

    public List<DataSet> retrieveResultFromRankedSDFile(final String pathToRankedSDFile, final String nucleus) {
        final WebClient webClient = this.webClientBuilder.baseUrl(
                "http://webcase-gateway:8081/webcase-casekit/fileParser/parseRankedSdf")
                                                         .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                        MediaType.APPLICATION_JSON_VALUE)
                                                         .exchangeStrategies(this.exchangeStrategies)
                                                         .build();

        final BufferedReader bufferedReader = FileSystem.readFile(pathToRankedSDFile);
        if (bufferedReader
                == null) {
            System.out.println("retrieveResultFromRankedSDFile: could not read file \""
                                       + pathToRankedSDFile
                                       + "\"");
            return new ArrayList<>();
        }
        final String fileContent = bufferedReader.lines()
                                                 .collect(Collectors.joining("\n"));
        final Transfer requestTransfer = new Transfer();
        requestTransfer.setFileContent(fileContent);
        requestTransfer.setNucleus(nucleus);
        final Transfer resultTransfer = webClient.post()
                                                 .bodyValue(requestTransfer)
                                                 .retrieve()
                                                 .bodyToMono(Transfer.class)
                                                 .block();

        return resultTransfer.getDataSetList();
    }
}
