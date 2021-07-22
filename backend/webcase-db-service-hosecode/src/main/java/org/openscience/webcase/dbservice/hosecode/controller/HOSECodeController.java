package org.openscience.webcase.dbservice.hosecode.controller;

import casekit.nmr.analysis.HOSECodeShiftStatistics;
import casekit.nmr.model.DataSet;
import org.apache.http.HttpHeaders;
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

import java.util.Arrays;
import java.util.List;
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

    private final HOSECodeServiceImplementation hoseCodeServiceImplementation;
    private final WebClient.Builder webClientBuilder;

    @Autowired
    public HOSECodeController(final WebClient.Builder webClientBuilder,
                              final HOSECodeServiceImplementation hoseCodeServiceImplementation) {
        this.webClientBuilder = webClientBuilder;
        this.hoseCodeServiceImplementation = hoseCodeServiceImplementation;
    }


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
    public void replaceAll(@RequestParam final String[] nuclei, final int maxSphere) {
        this.deleteAll();

        final List<DataSet> dataSetList = this.getByDataSetSpectrumNuclei(nuclei)
                                              .collectList()
                                              .block();
        final Map<String, Map<String, Double[]>> hoseCodeShiftStatistics = HOSECodeShiftStatistics.buildHOSECodeShiftStatistics(
                dataSetList, maxSphere, false);
        hoseCodeShiftStatistics.keySet()
                               .forEach(hoseCode -> this.insert(new HOSECodeRecord(hoseCode, new HOSECode(hoseCode,
                                                                                                          hoseCodeShiftStatistics.get(
                                                                                                                  hoseCode)))));
    }

    public Flux<DataSet> getByDataSetSpectrumNuclei(final String[] nuclei) {
        final WebClient webClient = this.webClientBuilder.baseUrl(
                "http://webcase-gateway:8080/webcase-db-service-dataset/nmrshiftdb")
                                                         .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                        MediaType.APPLICATION_JSON_VALUE)
                                                         .exchangeStrategies(this.exchangeStrategies)
                                                         .build();
        // @TODO take the nuclei order into account when matching -> now it's just an exact array match
        final String nucleiString = Arrays.stream(nuclei)
                                          .reduce("", (concat, current) -> concat
                                                  + current);
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
        uriComponentsBuilder.path("/getByNuclei")
                            .queryParam("nuclei", nucleiString);

        return webClient.get()
                        .uri(uriComponentsBuilder.toUriString())
                        .retrieve()
                        .bodyToFlux(DataSet.class);
    }
}
