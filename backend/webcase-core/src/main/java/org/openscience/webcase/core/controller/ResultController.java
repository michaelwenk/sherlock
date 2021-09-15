package org.openscience.webcase.core.controller;

import org.openscience.webcase.core.model.db.ResultRecord;
import org.openscience.webcase.core.model.exchange.Transfer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping(value = "/result")
public class ResultController {

    private final WebClient.Builder webClientBuilder;
    private final ExchangeStrategies exchangeStrategies;

    @Autowired
    public ResultController(final WebClient.Builder webClientBuilder, final ExchangeStrategies exchangeStrategies) {
        this.webClientBuilder = webClientBuilder;
        this.exchangeStrategies = exchangeStrategies;
    }

    @PostMapping(value = "/store")
    public ResponseEntity<Transfer> store(@RequestBody final Transfer requestTransfer) {
        final Transfer responseTransfer = new Transfer();
        final WebClient webClient = this.webClientBuilder.baseUrl(
                "http://webcase-gateway:8080/webcase-db-service-result/insert")
                                                         .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                        MediaType.APPLICATION_JSON_VALUE)
                                                         .exchangeStrategies(this.exchangeStrategies)
                                                         .build();
        final ResultRecord requestResultRecord = new ResultRecord();
        requestResultRecord.setDataSetList(requestTransfer.getDataSetList());
        final ResultRecord responseResultRecord = webClient.post()
                                                           .bodyValue(requestResultRecord)
                                                           .retrieve()
                                                           .bodyToMono(ResultRecord.class)
                                                           .block();
        System.out.println(responseResultRecord.getId());

        responseTransfer.setResultID(responseResultRecord.getId());
        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
    }

    @GetMapping(value = "/retrieve")
    public ResultRecord retrieve(@RequestParam final String resultID) {
        final WebClient webClient = this.webClientBuilder.baseUrl(
                "http://webcase-gateway:8080/webcase-db-service-result")
                                                         .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                        MediaType.APPLICATION_JSON_VALUE)
                                                         .build();
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
        uriComponentsBuilder.path("/getById")
                            .queryParam("id", resultID);

        return webClient.get()
                        .uri(uriComponentsBuilder.toUriString())
                        .retrieve()
                        .bodyToMono(ResultRecord.class)
                        .block();
    }
}
