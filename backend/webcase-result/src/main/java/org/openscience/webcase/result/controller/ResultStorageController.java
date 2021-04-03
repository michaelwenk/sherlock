package org.openscience.webcase.result.controller;

import org.openscience.webcase.result.model.db.ResultRecord;
import org.openscience.webcase.result.model.exchange.Transfer;
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

@RestController
@RequestMapping(value = "/store")
public class ResultStorageController {

    final ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                                                                    .codecs(configurer -> configurer.defaultCodecs()
                                                                                                    .maxInMemorySize(
                                                                                                            1024
                                                                                                                    * 1000000))
                                                                    .build();

    @Autowired
    private WebClient.Builder webClientBuilder;

    @PostMapping(value = "/storeResult")
    public ResponseEntity<Transfer> storeResult(@RequestBody final Transfer requestTransfer) {
        final Transfer responseTransfer = new Transfer();
        final WebClient webClient = this.webClientBuilder.baseUrl(
                "http://localhost:8081/webcase-db-service-result/insert")
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
}
