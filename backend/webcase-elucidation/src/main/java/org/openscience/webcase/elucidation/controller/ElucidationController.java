package org.openscience.webcase.elucidation.controller;

import org.openscience.webcase.elucidation.model.exchange.Transfer;
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
@RequestMapping(value = "/")
public class ElucidationController {

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

    @PostMapping(value = "elucidation")
    public ResponseEntity<Transfer> elucidate(@RequestBody final Transfer requestTransfer) {
        final Transfer responseTransfer = new Transfer();

        final WebClient webClient = this.webClientBuilder.baseUrl("http://webcase-gateway:8080/webcase-pylsd/runPyLSD")
                                                         .exchangeStrategies(this.exchangeStrategies)
                                                         .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                        MediaType.APPLICATION_JSON_VALUE)
                                                         .build();

        // run PyLSD
        final Transfer queryTransfer = new Transfer();
        queryTransfer.setData(requestTransfer.getData());
        queryTransfer.setElucidationOptions(requestTransfer.getElucidationOptions());
        queryTransfer.setRequestID(requestTransfer.getRequestID());
        queryTransfer.setMf(requestTransfer.getMf());
        final Transfer queryResultTransfer = webClient.post()
                                                      .bodyValue(queryTransfer)
                                                      .retrieve()
                                                      .bodyToMono(Transfer.class)
                                                      .block();
        responseTransfer.setPyLSDRunWasSuccessful(queryResultTransfer.getPyLSDRunWasSuccessful());
        responseTransfer.setDataSetList(queryResultTransfer.getDataSetList());
        responseTransfer.setResultID(queryResultTransfer.getResultID());


        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
    }
}
