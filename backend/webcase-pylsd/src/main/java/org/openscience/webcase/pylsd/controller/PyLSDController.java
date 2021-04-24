package org.openscience.webcase.pylsd.controller;

import org.openscience.webcase.pylsd.model.exchange.Transfer;
import org.openscience.webcase.pylsd.utils.HybridizationDetection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/")
public class PyLSDController {

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
        queryTransfer.setElucidationOptions(requestTransfer.getElucidationOptions());
        queryTransfer.setMf(requestTransfer.getMf());
        return webClient.post()
                        .bodyValue(queryTransfer)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
    }

    @PostMapping(value = "/runPyLSD")
    public ResponseEntity<Transfer> runPyLSD(@RequestBody final Transfer requestTransfer) {
        final Transfer resultTransfer = new Transfer();

        //        System.out.println(requestTransfer.getElucidationOptions()
        //                                          .getPathToPyLSDInputFileFolder());
        //        final Path path = Paths.get(requestTransfer.getElucidationOptions()
        //                                                   .getPathToPyLSDInputFileFolder());
        //        Files.createDirectory(path);
        //        System.out.println("Directory is created!");

        System.out.println(requestTransfer);
        final String pyLSDInputFileContent = this.createPyLSDInputFile(requestTransfer);
        System.out.println(pyLSDInputFileContent);

        //        resultTransfer.setPyLSDInputFileCreationWasSuccessful(FileSystem.writeFile(
        //                requestTransfer.getElucidationOptions()
        //                               .getPathToPyLSDInputFile(), pyLSDInputFileContent));
        //
        //        // run PyLSD
        //        if (queryResultTransfer.getPyLSDInputFileCreationWasSuccessful()
        //                != null
        //                && queryResultTransfer.getPyLSDInputFileCreationWasSuccessful()) {
        //            System.out.println("--> has been written successfully: ");
        //            //                                                   + pathToPyLSDInputFile);
        //            //            webClient = this.webClientBuilder.
        //            //                                                     baseUrl("http://webcase-gateway:8081/webcase-pylsd/runPyLSD")
        //            //                                             .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        //            //                                             .build();
        //            //
        //            //            queryResultTransfer = webClient.post()
        //            //                                           .bodyValue(queryTransfer)
        //            //                                           .retrieve()
        //            //                                           .bodyToMono(Transfer.class)
        //            //                                           .block();
        //            //            System.out.println("--> has been executed successfully: "
        //            //                                       + queryResultTransfer.getPyLSDRunWasSuccessful());
        //            //            //                                                   + " -> "
        //            //            //                                                   + queryResultTransfer.getElucidationOptions()
        //            //            //                                                                        .getPathToResultsFile());
        //            //
        //            //            if (queryResultTransfer.getPyLSDRunWasSuccessful()
        //            //                    != null
        //            //                    && queryResultTransfer.getPyLSDRunWasSuccessful()) {
        //            //                //                webClient = this.webClientBuilder.baseUrl("http://webcase-gateway:8081/webcase-result/retrieve")
        //            //                //                                                 .defaultHeader(HttpHeaders.CONTENT_TYPE,
        //            //                //                                                                MediaType.APPLICATION_JSON_VALUE)
        //            //                //                                                 .exchangeStrategies(this.exchangeStrategies)
        //            //                //                                                 .build();
        //            //                //                final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
        //            //                //                System.out.println("pathToResultsFile: "
        //            //                //                                           + queryResultTransfer.getElucidationOptions()
        //            //                //                                                                .getPathToResultsFile());
        //            //                //                uriComponentsBuilder.path("/retrieveResultFromRankedSDFile")
        //            //                //                                    .queryParam("pathToRankedSDFile", queryResultTransfer.getElucidationOptions()
        //            //                //                                                                                         .getPathToResultsFile());
        //            //                //                // retrieve results from PyLSD results file
        //            //                //                queryResultTransfer = webClient.get()
        //            //                //                                               .uri(uriComponentsBuilder.toUriString())
        //            //                //                                               .retrieve()
        //            //                //                                               .bodyToMono(Transfer.class)
        //            //                //                                               .block();
        //            //                System.out.println("--> number of results: "
        //            //                                           + queryResultTransfer.getDataSetList()
        //            //                                                                .size());
        //            //                responseTransfer.setDataSetList(queryResultTransfer.getDataSetList());
        //            //
        //            //                // store results in DB if not empty
        //            //                if (!responseTransfer.getDataSetList()
        //            //                                     .isEmpty()) {
        //            //                    webClient = this.webClientBuilder.baseUrl(
        //            //                            "http://webcase-gateway:8081/webcase-result/store/storeResult")
        //            //                                                     .defaultHeader(HttpHeaders.CONTENT_TYPE,
        //            //                                                                    MediaType.APPLICATION_JSON_VALUE)
        //            //                                                     .exchangeStrategies(this.exchangeStrategies)
        //            //                                                     .build();
        //            //                    queryResultTransfer = webClient.post()
        //            //                                                   .bodyValue(responseTransfer)
        //            //                                                   .retrieve()
        //            //                                                   .bodyToMono(Transfer.class)
        //            //                                                   .block();
        //            //                    if (queryResultTransfer.getResultID()
        //            //                            != null) {
        //            //                        System.out.println(queryResultTransfer.getResultID());
        //            //                        responseTransfer.setResultID(queryResultTransfer.getResultID());
        //            //                    }
        //            //                }
        //            //            }
        //            //
        //            //            // cleanup of created files and folder
        //            //            webClient = this.webClientBuilder.baseUrl("http://webcase-gateway:8081/webcase-pylsd")
        //            //                                             .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        //            //                                             .build();
        //            //            final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
        //            //            uriComponentsBuilder.path("/cleanup")
        //            //                                .queryParam("pathToPyLSDInputFileFolder", pathToPyLSDInputFileFolder);
        //            //            webClient.get()
        //            //                     .uri(uriComponentsBuilder.toUriString())
        //            //                     .retrieve()
        //            //                     .bodyToMono(Boolean.class)
        //            //                     .block();
        //        } else {
        //            System.out.println("--> file creation failed: ");
        //            //                                       + pathToPyLSDInputFile);
        //        }
        //
        //        try {
        //            // try to execute PyLSD
        //            final ProcessBuilder builder = new ProcessBuilder();
        //            builder.directory(new File(requestTransfer.getElucidationOptions()
        //                                                      .getPathToPyLSDExecutableFolder()))
        //                   .redirectError(new File(requestTransfer.getElucidationOptions()
        //                                                          .getPathToPyLSDInputFileFolder()
        //                                                   + requestTransfer.getRequestID()
        //                                                   + "_error.txt"))
        //                   .redirectOutput(new File(requestTransfer.getElucidationOptions()
        //                                                           .getPathToPyLSDInputFileFolder()
        //                                                    + requestTransfer.getRequestID()
        //                                                    + "_log.txt"))
        //                   .command("python2.7", requestTransfer.getElucidationOptions()
        //                                                        .getPathToPyLSDExecutableFolder()
        //                           + "lsd_modified.py", requestTransfer.getElucidationOptions()
        //                                                               .getPathToPyLSDInputFile());
        //            final Process process = builder.start();
        //            final int exitCode = process.waitFor();
        //            final boolean pyLSDRunWasSuccessful = exitCode
        //                    == 0;
        //
        //            if (pyLSDRunWasSuccessful) {
        //                System.out.println("run was successful");
        //                System.out.println(requestTransfer.getElucidationOptions()
        //                                                  .getPathToPyLSDInputFileFolder());
        //                resultTransfer.setElucidationOptions(requestTransfer.getElucidationOptions());
        //                final String pathToResultsFilePredictions = requestTransfer.getElucidationOptions()
        //                                                                           .getPathToPyLSDInputFileFolder()
        //                        + "/"
        //                        + requestTransfer.getRequestID()
        //                        + "_D.sdf";
        //                resultTransfer.getElucidationOptions()
        //                              .setPathToResultsFile(pathToResultsFilePredictions);
        //            } else {
        //                System.out.println("run was NOT successful");
        //            }
        //            resultTransfer.setPyLSDRunWasSuccessful(pyLSDRunWasSuccessful);
        //
        //        } catch (final Exception e) {
        //            e.printStackTrace();
        //            resultTransfer.setPyLSDRunWasSuccessful(false);
        //        }

        return new ResponseEntity<>(resultTransfer, HttpStatus.OK);
    }

    @GetMapping(value = "/cleanup")
    public ResponseEntity<Boolean> cleanup(@RequestParam final String pathToPyLSDInputFileFolder) {
        boolean cleaned = false;
        final Path path = Paths.get(pathToPyLSDInputFileFolder);
        try {
            Files.walk(path)
                 .map(Path::toFile)
                 .forEach(File::delete);
            Files.delete(path);
            if (!Files.exists(path)) {
                System.out.println("Directory is deleted!");
                cleaned = true;
            }
        } catch (final IOException e) {
            System.out.println("Directory NOT deleted!");
            e.printStackTrace();
        }
        return new ResponseEntity<>(cleaned, HttpStatus.OK);
    }
}
