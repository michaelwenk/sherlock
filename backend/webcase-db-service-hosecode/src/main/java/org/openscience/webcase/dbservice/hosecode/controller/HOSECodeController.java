package org.openscience.webcase.dbservice.hosecode.controller;

import org.apache.http.HttpHeaders;
import org.openscience.webcase.dbservice.hosecode.model.exchange.Transfer;
import org.openscience.webcase.dbservice.hosecode.service.HOSECodeServiceImplementation;
import org.openscience.webcase.dbservice.hosecode.service.model.HOSECode;
import org.openscience.webcase.dbservice.hosecode.service.model.HOSECodeRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping(value = "/")
public class HOSECodeController {

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

    @Autowired
    private HOSECodeServiceImplementation hoseCodeServiceImplementation;

    @GetMapping(value = "/getByID")
    public Mono<HOSECode> getByID(@RequestParam final String id) {
        return this.hoseCodeServiceImplementation.findById(id);
    }

    @GetMapping(value = "/getByHOSECode", produces = "application/json")
    public Mono<HOSECode> getByHOSECode(@RequestParam final String HOSECode) {
        return this.hoseCodeServiceImplementation.findByHoseCodeObjectHOSECode(HOSECode);
    }

    @GetMapping(value = "/count")
    public Mono<Long> getCount() {
        return this.hoseCodeServiceImplementation.count();
    }

    @GetMapping(value = "/getAll", produces = "application/stream+json")
    public Flux<HOSECode> getAll() {
        return this.hoseCodeServiceImplementation.findAll();
    }

    @PostMapping(value = "/insert", consumes = "application/json")
    public void insert(@RequestBody final HOSECodeRecord hoseCodeRecord) {
        this.hoseCodeServiceImplementation.insert(hoseCodeRecord)
                                          .block();
    }

    @DeleteMapping(value = "/deleteAll")
    public void deleteAll() {
        this.hoseCodeServiceImplementation.deleteAll()
                                          .block();
    }

    @PostMapping(value = "/replaceAll")
    public void replaceAll(@RequestParam final String[] nuclei) {
        this.deleteAll();

        final WebClient webClient = this.webClientBuilder.baseUrl(
                "http://webcase-gateway:8080/webcase-casekit/dbservice")
                                                         .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                        MediaType.APPLICATION_JSON_VALUE)
                                                         .exchangeStrategies(this.exchangeStrategies)
                                                         .build();
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
        uriComponentsBuilder.path("/getHOSECodesFromNMRShiftDB")
                            .queryParam("nuclei", String.join(",", nuclei));
        final Map<String, Map<String, Double[]>> hoseCodeShiftStatistics = webClient.get()
                                                                                    .uri(uriComponentsBuilder.toUriString())
                                                                                    .retrieve()
                                                                                    .bodyToMono(Transfer.class)
                                                                                    .block()
                                                                                    .getHoseCodeShiftStatistics();
        hoseCodeShiftStatistics.keySet()
                               .forEach(hoseCode -> this.insert(new HOSECodeRecord(hoseCode, new HOSECode(hoseCode,
                                                                                                          hoseCodeShiftStatistics.get(
                                                                                                                  hoseCode)))));
    }
}
