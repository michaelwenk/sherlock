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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/")
public class ResultRetrievalController {

    final ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                                                                    .codecs(configurer -> configurer.defaultCodecs()
                                                                                                    .maxInMemorySize(
                                                                                                            1024
                                                                                                                    * 1000000))
                                                                    .build();

    @Autowired
    private WebClient.Builder webClientBuilder;

    @GetMapping(value = "retrieveResultFromDatabase")
    public ResponseEntity<Transfer> retrieveResultFromDatabase(@RequestParam final String resultID) {
        final Transfer responseTransfer = new Transfer();

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

        responseTransfer.setDataSetList(dataSetList);
        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
    }

    @GetMapping(value = "retrieveResultFromSmilesFile")
    public ResponseEntity<Transfer> retrieveResultFromSmilesFile(@RequestParam final String pathToResultsFile) {
        final Transfer responseTransfer = new Transfer();
        final List<DataSet> dataSetList = new ArrayList<>();
        final List<String> smilesList = FileSystem.getSmilesListFromFile(pathToResultsFile);

        DataSet dataSet;
        Map<String, String> meta;
        for (final String smiles : smilesList) {
            meta = new HashMap<>();
            meta.put("smiles", smiles);
            dataSet = new DataSet();
            dataSet.setMeta(meta);

            dataSetList.add(dataSet);
        }

        responseTransfer.setDataSetList(dataSetList);
        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
    }

    @GetMapping(value = "retrieveResultFromRankedSDFile")
    public ResponseEntity<Transfer> retrieveResultFromRankedSDFile(@RequestParam final String pathToRankedSDFile) {
        final Transfer responseTransfer = new Transfer();
        final WebClient webClient = this.webClientBuilder.baseUrl("http://localhost:8081/webcase-casekit/fileParser")
                                                         .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                        MediaType.APPLICATION_JSON_VALUE)
                                                         .exchangeStrategies(this.exchangeStrategies)
                                                         .build();
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
        uriComponentsBuilder.path("/parseRankedSdf")
                            .queryParam("pathToRankedSDFile", pathToRankedSDFile);
        final Transfer resultTransfer = webClient.get()
                                                 .uri(uriComponentsBuilder.toUriString())
                                                 .retrieve()
                                                 .bodyToMono(Transfer.class)
                                                 .block();
        System.out.println(resultTransfer.getDataSetList()
                                         .size());

        responseTransfer.setDataSetList(resultTransfer.getDataSetList());
        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
    }
}
