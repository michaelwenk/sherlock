package org.openscience.webcase.result.controller;

import casekit.nmr.model.DataSet;
import org.openscience.webcase.result.model.db.ResultRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping(value = "/retrieve")
public class ResultRetrievalController {

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

    @GetMapping(value = "/retrieveResultFromDatabase")
    public List<DataSet> retrieveResultFromDatabase(@RequestParam final String resultID) {
        final WebClient webClient = this.webClientBuilder.baseUrl(
                "http://webcase-gateway:8080/webcase-db-service-result")
                                                         .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                        MediaType.APPLICATION_JSON_VALUE)
                                                         .build();
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
        uriComponentsBuilder.path("/getById")
                            .queryParam("id", resultID);

        final ResultRecord resultRecord = webClient.get()
                                                   .uri(uriComponentsBuilder.toUriString())
                                                   .retrieve()
                                                   .bodyToMono(ResultRecord.class)
                                                   .block();

        return resultRecord.getDataSetList();
    }
}
