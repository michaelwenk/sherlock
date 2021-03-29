package org.openscience.webcase.resultretrieval.controller;

import org.openscience.webcase.resultretrieval.model.DataSet;
import org.openscience.webcase.resultretrieval.model.exchange.Transfer;
import org.openscience.webcase.resultretrieval.utils.FileSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/")
public class ResultRetrievalController {

    @Autowired
    private WebClient.Builder webClientBuilder;

    @GetMapping(value = "retrieveResultFromDatabase")
    public ResponseEntity<Transfer> retrieveResultFromDatabase(@RequestParam final String resultID){
        final Transfer resultTransfer = new Transfer();

        final WebClient webClient = this.webClientBuilder.baseUrl("http://localhost:8081/webcase-db-service-result")
                                         .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                        MediaType.APPLICATION_JSON_VALUE)
                                         .build();
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
        uriComponentsBuilder.path("/getById")
                            .queryParam("id", resultID);

        final List<DataSet> dataSetList = webClient.get()
                                         .uri(uriComponentsBuilder.toUriString())
                                         .retrieve()
                                         .bodyToMono(new ParameterizedTypeReference<List<DataSet>>() {
                                         })
                                         .block();

        resultTransfer.setDataSetList(dataSetList);
        return new ResponseEntity<>(resultTransfer, HttpStatus.OK);
    }

    @GetMapping(value = "retrieveResultFromFile")
    public ResponseEntity<Transfer> retrieveResultFromFile(@RequestParam final String pathToResultsFile){
        final Transfer resultTransfer = new Transfer();
        final List<DataSet> dataSetList = new ArrayList<>();
        final List<String> smilesList = this.getSmilesListFromFile(pathToResultsFile);

        DataSet dataSet;
        Map<String, String> meta;
        for (final String smiles : smilesList){
            meta = new HashMap<>();
            meta.put("smiles", smiles);
            dataSet = new DataSet();
            dataSet.setMeta(meta);

            dataSetList.add(dataSet);
        }

        resultTransfer.setDataSetList(dataSetList);
        return new ResponseEntity<>(resultTransfer, HttpStatus.OK);
    }

    private List<String> getSmilesListFromFile(final String pathToSmilesFile){
        final List<String> smilesList = new ArrayList<>();
        try {
            final BufferedReader bufferedReader = FileSystem.readFile(pathToSmilesFile);
            if (bufferedReader != null) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    smilesList.add(line);
                }
                bufferedReader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return smilesList;
    }
}
