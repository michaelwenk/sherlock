package org.openscience.sherlock.core.controller;

import org.openscience.sherlock.core.model.exchange.Transfer;
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
@RequestMapping(value = "/elucidation")
public class ElucidationController {

    private final WebClient.Builder webClientBuilder;
    private final ExchangeStrategies exchangeStrategies;

    @Autowired
    public ElucidationController(final WebClient.Builder webClientBuilder,
                                 final ExchangeStrategies exchangeStrategies) {
        this.webClientBuilder = webClientBuilder;
        this.exchangeStrategies = exchangeStrategies;
    }

    @PostMapping(value = "/elucidate")
    public ResponseEntity<Transfer> elucidate(@RequestBody final Transfer requestTransfer) {
        final Transfer responseTransfer = new Transfer();

        final WebClient webClient = this.webClientBuilder.baseUrl("http://sherlock-gateway:8080/sherlock-pylsd/runPyLSD")
                                                         .exchangeStrategies(this.exchangeStrategies)
                                                         .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                        MediaType.APPLICATION_JSON_VALUE)
                                                         .build();

        // run PyLSD
        try {
            final Transfer queryResultTransfer = webClient.post()
                                                          .bodyValue(requestTransfer)
                                                          .retrieve()
                                                          .bodyToMono(Transfer.class)
                                                          .block();
            responseTransfer.setPyLSDRunWasSuccessful(queryResultTransfer.getPyLSDRunWasSuccessful());
            responseTransfer.setDataSetList(queryResultTransfer.getDataSetList());
            responseTransfer.setDetections(queryResultTransfer.getDetections());
        } catch (final Exception e) {
            responseTransfer.setErrorMessage(e.getMessage());
            return new ResponseEntity<>(responseTransfer, HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
    }
}
