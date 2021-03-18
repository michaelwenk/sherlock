package org.openscience.webcase.elucidation.controller;

import org.openscience.webcase.elucidation.model.DataSet;
import org.openscience.webcase.elucidation.model.exchange.Transfer;
import org.openscience.webcase.elucidation.model.nmrdisplayer.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(value = "/")
public class ElucidationController {

    @Autowired
    private WebClient.Builder webClientBuilder;

    @PostMapping(value = "elucidation")
    public ResponseEntity<Transfer> elucidate(@RequestBody final Transfer requestTransfer){
        final List<DataSet> dataSetList = new ArrayList<>();
        final Data data = requestTransfer.getData();
        final String pathToPyLSDInputFileFolder = requestTransfer.getPathToPyLSDInputFileFolder() + "/" + requestTransfer.getRequestID() + "/";
        final String pathToPyLSDInputFile = pathToPyLSDInputFileFolder + requestTransfer.getRequestID() + ".pylsd";

        WebClient webClient = webClientBuilder.
                baseUrl("http://localhost:8081/webcase-pylsd-create-input-file")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
        uriComponentsBuilder.path("/createPyLSDInputFile")
                .queryParam("allowHeteroHeteroBonds", requestTransfer.isAllowHeteroHeteroBonds())
                .queryParam("pathToPyLSDInputFileFolder", pathToPyLSDInputFileFolder)
                .queryParam("pathToPyLSDInputFile", pathToPyLSDInputFile)
                .queryParam("pathToLSDFilterList", requestTransfer.getPathToLSDFilterList())
                .queryParam("requestID", requestTransfer.getRequestID());

        // create PyLSD input file
        final Transfer queryTransfer = new Transfer();
        queryTransfer.setData(data);
        Transfer queryResultTransfer = webClient
                .post()
                .uri(uriComponentsBuilder.toUriString())
                .bodyValue(queryTransfer)
                .retrieve()
                .bodyToMono(Transfer.class).block();

        // run PyLSD
        if(queryResultTransfer.getPyLSDInputFileCreationWasSuccessful()){
            System.out.println("--> has been written successfully: " + pathToPyLSDInputFile);
            webClient = webClientBuilder.
                    baseUrl("http://localhost:8081/webcase-pylsd-run")
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();
            uriComponentsBuilder = UriComponentsBuilder.newInstance();
            uriComponentsBuilder.path("/runPyLSD")
                    .queryParam("pathToPyLSDExecutableFolder", requestTransfer.getPathToPyLSDExecutableFolder())
                    .queryParam("pathToPyLSDInputFileFolder", pathToPyLSDInputFileFolder)
                    .queryParam("pathToPyLSDInputFile", pathToPyLSDInputFile)
                    .queryParam("requestID", requestTransfer.getRequestID());

            // create PyLSD input file
            queryResultTransfer = webClient
                    .get()
                    .uri(uriComponentsBuilder.toUriString())
                    .retrieve()
                    .bodyToMono(Transfer.class).block();
            System.out.println("--> has been executed successfully: " + queryResultTransfer.getPyLSDRunWasSuccessful() + " -> " + queryResultTransfer.getPathToResultsFile());

            if(queryResultTransfer.getPyLSDRunWasSuccessful()){
                webClient = webClientBuilder.baseUrl("http://localhost:8081/webcase-result-retrieval")
                                            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                            .build();
                uriComponentsBuilder = UriComponentsBuilder.newInstance();
                uriComponentsBuilder.path("/retrieveResultFromFile")
                                    .queryParam("pathToResultsFile", queryResultTransfer.getPathToResultsFile());

                // retrieve results
                queryResultTransfer = webClient
                        .get()
                        .uri(uriComponentsBuilder.toUriString())
                        .retrieve()
                        .bodyToMono(Transfer.class).block();
                System.out.println("--> list of results: " + queryResultTransfer.getDataSetList().size() + " -> " + queryResultTransfer.getDataSetList());
                queryResultTransfer.getDataSetList().forEach(dataSet -> {
                    dataSet.addMetaInfo("determination", "elucidation");
                    dataSetList.add(dataSet) ;
                });
            }

        } else {
            System.out.println("--> file creation failed: " + pathToPyLSDInputFile);
        }


        final Transfer resultTransfer = new Transfer();
        resultTransfer.setDataSetList(dataSetList);

        return new ResponseEntity<>(resultTransfer, HttpStatus.OK);
    }
}
