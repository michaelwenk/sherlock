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
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(value = "/")
public class ElucidationController {

    @Autowired
    private WebClient.Builder webClientBuilder;

    private final String pathToLSDFilterList = "/Users/mwenk/work/software/PyLSD-a4/LSD/Filters/list.txt";
    private final String pathToPyLSDInputFileFolder = "/Users/mwenk/work/software/PyLSD-a4/Variant/";

    @PostMapping(value = "elucidation")
    public ResponseEntity<Transfer> elucidate(@RequestBody final Transfer requestTransfer, @RequestParam final boolean allowHeteroHeteroBonds, @RequestParam final String requestID){
        final List<DataSet> dataSetList = new ArrayList<>();
        final Data data = requestTransfer.getData();
        // set ExchangeSettings
        final ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 100000)).build();
        final WebClient webClient = webClientBuilder.
                baseUrl("http://localhost:8081/webcase-pylsd-create-input-file")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(exchangeStrategies)
                .build();
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
        uriComponentsBuilder.path("/createPyLSDInputFile")
                .queryParam("allowHeteroHeteroBonds", allowHeteroHeteroBonds)
                .queryParam("pathToPyLSDInputFile", pathToPyLSDInputFileFolder + "webcase_" + requestID + ".pylsd")
                .queryParam("pathToLSDFilterList", pathToLSDFilterList)
                .queryParam("requestID", requestID);

        // create PyLSD input file
        final Transfer queryTransfer = new Transfer();
        queryTransfer.setData(data);
        final HttpStatus queryResultHttpStatus = webClient
                .post()
                .uri(uriComponentsBuilder.toUriString())
                .bodyValue(queryTransfer)
                .retrieve()
                .bodyToMono(HttpStatus.class).block();
        // run PyLSD
        if(queryResultHttpStatus == HttpStatus.OK){
        }


        final Transfer resultTransfer = new Transfer();
        resultTransfer.setDataSetList(dataSetList);

        return new ResponseEntity<>(resultTransfer, HttpStatus.OK);
    }
}
