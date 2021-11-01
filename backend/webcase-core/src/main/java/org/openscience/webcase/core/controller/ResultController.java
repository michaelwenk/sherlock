package org.openscience.webcase.core.controller;

import org.openscience.webcase.core.model.db.ResultRecord;
import org.openscience.webcase.core.model.exchange.Transfer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

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

    public ResponseEntity<Transfer> store(final ResultRecord requestResultRecord) {
        final Transfer responseTransfer = new Transfer();
        final WebClient webClient = this.webClientBuilder.baseUrl(
                                                "http://webcase-gateway:8080/webcase-db-service-result/insert")
                                                         .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                        MediaType.APPLICATION_JSON_VALUE)
                                                         .exchangeStrategies(this.exchangeStrategies)
                                                         .build();
        try {
            final ResultRecord responseResultRecord = webClient.post()
                                                               .bodyValue(requestResultRecord)
                                                               .retrieve()
                                                               .bodyToMono(ResultRecord.class)
                                                               .block();
            responseTransfer.setResultRecord(responseResultRecord);
        } catch (final Exception e) {
            responseTransfer.setErrorMessage(e.getMessage());
            return new ResponseEntity<>(responseTransfer, HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
    }
}
