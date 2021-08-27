package org.openscience.webcase.dbservice.hosecode.controller;

import casekit.nmr.analysis.HOSECodeShiftStatistics;
import casekit.nmr.utils.Statistics;
import org.apache.http.HttpHeaders;
import org.openscience.webcase.dbservice.hosecode.service.HOSECodeServiceImplementation;
import org.openscience.webcase.dbservice.hosecode.service.model.DataSetRecord;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

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


    @DeleteMapping(value = "/deleteAll")
    public Mono<Void> deleteAll() {
        return this.hoseCodeServiceImplementation.deleteAll();
    }

    @PostMapping(value = "/replaceAll")
    public void replaceAll(@RequestParam final String[] nuclei, final int maxSphere) {
        this.deleteAll()
            .block();
        this.clearHOSECodeDBEntriesMap();

        final Map<String, Map<String, ConcurrentLinkedQueue<Double>>> hoseCodeShifts = new ConcurrentHashMap<>();
        this.getByDataSetSpectrumNuclei(nuclei)
            .doOnNext(dataSetRecord -> HOSECodeShiftStatistics.insert(dataSetRecord.getDataSet(), maxSphere, false,
                                                                      hoseCodeShifts))
            .doOnTerminate(() -> {
                System.out.println(" -> hoseCodeShifts size: "
                                           + hoseCodeShifts.size());
                final Map<String, Map<String, Double[]>> hoseCodeShiftStatistics = this.buildHOSECodeShiftStatistics(
                        hoseCodeShifts);
                System.out.println(" -> hoseCodeShiftStatistics size: "
                                           + hoseCodeShiftStatistics.size());

                final Flux<HOSECodeRecord> hoseCodeRecordFlux = Flux.fromStream(hoseCodeShiftStatistics.keySet()
                                                                                                       .stream()
                                                                                                       .map(hoseCode -> new HOSECodeRecord(
                                                                                                               hoseCode,
                                                                                                               new HOSECode(
                                                                                                                       hoseCode,
                                                                                                                       hoseCodeShiftStatistics.get(
                                                                                                                               hoseCode)))));

                this.hoseCodeServiceImplementation.insertMany(hoseCodeRecordFlux)
                                                  .doOnError(Throwable::printStackTrace)
                                                  .doAfterTerminate(() -> {
                                                      this.fillHOSECodeDBEntriesMap();
                                                      System.out.println(" -> done");
                                                  })
                                                  .subscribe();
            })
            .doOnError(Throwable::printStackTrace)
            .subscribe();
    }

    private Map<String, Map<String, Double[]>> buildHOSECodeShiftStatistics(
            final Map<String, Map<String, ConcurrentLinkedQueue<Double>>> hoseCodeShifts) {

        final Map<String, Map<String, Double[]>> hoseCodeShiftStatistics = new HashMap<>();
        List<Double> values;
        for (final Map.Entry<String, Map<String, ConcurrentLinkedQueue<Double>>> hoseCodes : hoseCodeShifts.entrySet()) {
            hoseCodeShiftStatistics.put(hoseCodes.getKey(), new HashMap<>());
            for (final Map.Entry<String, ConcurrentLinkedQueue<Double>> solvents : hoseCodes.getValue()
                                                                                            .entrySet()) {
                values = new ArrayList<>(solvents.getValue());
                Statistics.removeOutliers(values, 1.5);
                hoseCodeShiftStatistics.get(hoseCodes.getKey())
                                       .put(solvents.getKey(),
                                            new Double[]{(double) values.size(), Collections.min(values),
                                                         Statistics.getMean(values), Statistics.getMedian(values),
                                                         Collections.max(values)});
            }
        }

        return hoseCodeShiftStatistics;
    }

    public Flux<DataSetRecord> getByDataSetSpectrumNuclei(final String[] nuclei) {
        final WebClient webClient = this.webClientBuilder.baseUrl(
                "http://webcase-gateway:8080/webcase-db-service-dataset/")
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
                        .bodyToFlux(DataSetRecord.class);
    }

    public void clearHOSECodeDBEntriesMap() {
        final WebClient webClient = this.webClientBuilder.baseUrl(
                "http://webcase-gateway:8080/webcase-db-service-dataset/clearHOSECodeDBEntriesMap")
                                                         .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                        MediaType.APPLICATION_JSON_VALUE)
                                                         .exchangeStrategies(this.exchangeStrategies)
                                                         .build();

        webClient.post()
                 .retrieve();
    }

    public void fillHOSECodeDBEntriesMap() {
        final WebClient webClient = this.webClientBuilder.baseUrl(
                "http://webcase-gateway:8080/webcase-db-service-dataset/fillHOSECodeDBEntriesMap")
                                                         .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                        MediaType.APPLICATION_JSON_VALUE)
                                                         .exchangeStrategies(this.exchangeStrategies)
                                                         .build();

        webClient.post()
                 .retrieve();
    }
}
