package org.openscience.webcase.elucidation.controller;

import org.openscience.webcase.elucidation.model.DataSet;
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
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/")
public class ElucidationController {

    @Autowired
    private WebClient.Builder webClientBuilder;

    @PostMapping(value = "elucidation")
    public ResponseEntity<Transfer> elucidate(@RequestBody final Transfer requestTransfer) {
        final Transfer resultTransfer = new Transfer();
        final List<DataSet> dataSetList = new ArrayList<>();

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
                                                           baseUrl("http://localhost:8081/webcase-pylsd-create-input-file/createPyLSDInputFile")
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
        Transfer queryResultTransfer = webClient.post()
                                                //                                                .uri(uriComponentsBuilder.toUriString())
                                                .bodyValue(queryTransfer)
                                                .retrieve()
                                                .bodyToMono(Transfer.class)
                                                .block();

        // run PyLSD
        if (queryResultTransfer.getPyLSDInputFileCreationWasSuccessful()) {
            System.out.println("--> has been written successfully: "
                                       + pathToPyLSDInputFile);
            webClient = this.webClientBuilder.
                                                     baseUrl("http://localhost:8081/webcase-pylsd-run/runPyLSD")
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
                                                 .build();
                final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
                uriComponentsBuilder.path("/retrieveResultFromFile")
                                    .queryParam("pathToResultsFile", queryResultTransfer.getElucidationOptions()
                                                                                        .getPathToResultsFile());

                // retrieve results from PyLSD results file
                queryResultTransfer = webClient.get()
                                               .uri(uriComponentsBuilder.toUriString())
                                               .retrieve()
                                               .bodyToMono(Transfer.class)
                                               .block();
                System.out.println("--> list of results from SMILES file: "
                                           + queryResultTransfer.getDataSetList()
                                                                .size()
                                           + " -> "
                                           + queryResultTransfer.getDataSetList());
                final List<DataSet> resultDataSets = queryResultTransfer.getDataSetList()
                                                                        .stream()
                                                                        .map(dataSet -> {
                                                                            dataSet.addMetaInfo("determination",
                                                                                                "elucidation");
                                                                            dataSetList.add(dataSet);

                                                                            return dataSet;
                                                                        })
                                                                        .collect(Collectors.toList());

                if (!resultDataSets.isEmpty()) {
                    webClient = this.webClientBuilder.baseUrl("http://localhost:8081/webcase-db-service-result/insert")
                                                     .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                    MediaType.APPLICATION_JSON_VALUE)
                                                     .build();
                    // store results in results DB
                    final String resultID = webClient.post()
                                                     .bodyValue(resultDataSets)
                                                     .retrieve()
                                                     .bodyToMono(String.class)
                                                     .block();
                    if (resultID
                            != null) {
                        System.out.println(resultID);
                        resultTransfer.setResultID(resultID);
                        resultTransfer.setDataSetList(resultDataSets);
                    }
                }
            }

        } else {
            System.out.println("--> file creation or execution failed: "
                                       + pathToPyLSDInputFile);
        }
        return new ResponseEntity<>(resultTransfer, HttpStatus.OK);
    }
}
