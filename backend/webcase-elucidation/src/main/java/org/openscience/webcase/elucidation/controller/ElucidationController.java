package org.openscience.webcase.elucidation.controller;

import org.openscience.webcase.elucidation.model.exchange.Transfer;
import org.openscience.webcase.elucidation.model.nmrdisplayer.Data;
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
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping(value = "/")
public class ElucidationController {

    final ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                                                                    .codecs(configurer -> configurer.defaultCodecs()
                                                                                                    .maxInMemorySize(
                                                                                                            1024
                                                                                                                    * 1000000))
                                                                    .build();

    @Autowired
    private WebClient.Builder webClientBuilder;

    @PostMapping(value = "elucidation")
    public ResponseEntity<Transfer> elucidate(@RequestBody final Transfer requestTransfer) {
        final Transfer responseTransfer = new Transfer();

        final Data data = requestTransfer.getData();
        final String pathToPyLSDInputFileFolder = requestTransfer.getElucidationOptions()
                                                                 .getPathToPyLSDInputFileFolder()
                + "/"
                + requestTransfer.getRequestID()
                + "/";
        final String pathToPyLSDInputFile = pathToPyLSDInputFileFolder
                + requestTransfer.getRequestID()
                + ".pylsd";

        WebClient webClient = this.webClientBuilder.
                                                           baseUrl("http://localhost:8081/webcase-pylsd/createPyLSDInputFile")
                                                   .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                  MediaType.APPLICATION_JSON_VALUE)
                                                   .build();

        // create PyLSD input file
        final Transfer queryTransfer = new Transfer();
        queryTransfer.setData(data);
        requestTransfer.getElucidationOptions()
                       .setPathToPyLSDInputFile(pathToPyLSDInputFile);
        requestTransfer.getElucidationOptions()
                       .setPathToPyLSDInputFileFolder(pathToPyLSDInputFileFolder);
        queryTransfer.setElucidationOptions(requestTransfer.getElucidationOptions());
        queryTransfer.setRequestID(requestTransfer.getRequestID());
        queryTransfer.setMf(requestTransfer.getMf());
        Transfer queryResultTransfer = webClient.post()
                                                .bodyValue(queryTransfer)
                                                .retrieve()
                                                .bodyToMono(Transfer.class)
                                                .block();

        // run PyLSD
        if (queryResultTransfer.getPyLSDInputFileCreationWasSuccessful()) {
            System.out.println("--> has been written successfully: "
                                       + pathToPyLSDInputFile);
            webClient = this.webClientBuilder.
                                                     baseUrl("http://localhost:8081/webcase-pylsd/runPyLSD")
                                             .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                             .build();

            queryResultTransfer = webClient.post()
                                           .bodyValue(queryTransfer)
                                           .retrieve()
                                           .bodyToMono(Transfer.class)
                                           .block();
            System.out.println("--> has been executed successfully: "
                                       + queryResultTransfer.getPyLSDRunWasSuccessful()
                                       + " -> "
                                       + queryResultTransfer.getElucidationOptions()
                                                            .getPathToResultsFile());

            if (queryResultTransfer.getPyLSDRunWasSuccessful()) {
                webClient = this.webClientBuilder.baseUrl("http://localhost:8081/webcase-result-retrieval")
                                                 .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                MediaType.APPLICATION_JSON_VALUE)
                                                 .exchangeStrategies(this.exchangeStrategies)
                                                 .build();
                final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
                //                uriComponentsBuilder.path("/retrieveResultFromSmilesFile")
                //                                    .queryParam("pathToResultsFile", queryResultTransfer.getElucidationOptions()
                //                                                                                        .getPathToResultsFile());
                System.out.println("pathToResultsFile: "
                                           + queryResultTransfer.getElucidationOptions()
                                                                .getPathToResultsFile());
                uriComponentsBuilder.path("/retrieveResultFromRankedSDFile")
                                    .queryParam("pathToRankedSDFile", queryResultTransfer.getElucidationOptions()
                                                                                         .getPathToResultsFile());
                // retrieve results from PyLSD results file
                queryResultTransfer = webClient.get()
                                               .uri(uriComponentsBuilder.toUriString())
                                               .retrieve()
                                               .bodyToMono(Transfer.class)
                                               .block();
                System.out.println("--> number of results: "
                                           + queryResultTransfer.getDataSetList()
                                                                .size());
                responseTransfer.setDataSetList(queryResultTransfer.getDataSetList());

                // store results in DB if not empty
                if (!responseTransfer.getDataSetList()
                                     .isEmpty()) {
                    webClient = this.webClientBuilder.baseUrl("http://localhost:8081/webcase-db-service-result/insert")
                                                     .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                    MediaType.APPLICATION_JSON_VALUE)
                                                     .exchangeStrategies(this.exchangeStrategies)
                                                     .build();
                    final String resultID = webClient.post()
                                                     .bodyValue(responseTransfer.getDataSetList())
                                                     .retrieve()
                                                     .bodyToMono(String.class)
                                                     .block();
                    if (resultID
                            != null) {
                        System.out.println(resultID);
                        responseTransfer.setResultID(resultID);
                    }
                }
            }

            // cleanup of created files and folder
            webClient = this.webClientBuilder.baseUrl("http://localhost:8081/webcase-pylsd")
                                             .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                             .build();
            final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
            uriComponentsBuilder.path("/cleanup")
                                .queryParam("pathToPyLSDInputFileFolder", pathToPyLSDInputFileFolder);
            webClient.get()
                     .uri(uriComponentsBuilder.toUriString())
                     .retrieve()
                     .bodyToMono(Boolean.class)
                     .block();
        } else {
            System.out.println("--> file creation or execution failed: "
                                       + pathToPyLSDInputFile);
        }
        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
    }
}
