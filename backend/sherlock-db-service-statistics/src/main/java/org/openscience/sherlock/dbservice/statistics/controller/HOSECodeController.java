package org.openscience.sherlock.dbservice.statistics.controller;

import casekit.io.FileSystem;
import casekit.nmr.analysis.HOSECodeShiftStatistics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.HttpHeaders;
import org.openscience.sherlock.dbservice.statistics.service.HOSECodeServiceImplementation;
import org.openscience.sherlock.dbservice.statistics.service.model.DataSetRecord;
import org.openscience.sherlock.dbservice.statistics.service.model.HOSECode;
import org.openscience.sherlock.dbservice.statistics.service.model.HOSECodeRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@RestController
@RequestMapping(value = "/hosecode")
public class HOSECodeController {

    private final WebClient.Builder webClientBuilder;
    private final ExchangeStrategies exchangeStrategies;
    private final HOSECodeServiceImplementation hoseCodeServiceImplementation;

    @Autowired
    public HOSECodeController(final WebClient.Builder webClientBuilder, final ExchangeStrategies exchangeStrategies,
                              final HOSECodeServiceImplementation hoseCodeServiceImplementation) {
        this.webClientBuilder = webClientBuilder;
        this.exchangeStrategies = exchangeStrategies;
        this.hoseCodeServiceImplementation = hoseCodeServiceImplementation;
    }

    private String decode(final String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return "";
    }

    @GetMapping(value = "/getByID")
    public Mono<HOSECode> getByID(@RequestParam final String id) {
        return this.hoseCodeServiceImplementation.findById(this.decode(id));
    }

    @GetMapping(value = "/getByHOSECode", produces = "application/json")
    public Mono<HOSECode> getByHOSECode(@RequestParam final String HOSECode) {
        return this.hoseCodeServiceImplementation.findByHoseCodeObjectHOSECode(this.decode(HOSECode));
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
    public void replaceAll(@RequestParam final String[] nuclei, final int maxSphere, final String source) {
        System.out.println(" --> delete all DB entries...");
        this.deleteAll()
            .block();
        System.out.println(" --> deleted all DB entries!");

        System.out.println(" --> fetching all datasets, build HOSE code statistics and store...");
        final Map<String, Map<String, ConcurrentLinkedQueue<Double>>> hoseCodeShifts = new ConcurrentHashMap<>();
        this.getByDataSetSpectrumNucleiAndSource(nuclei, source)
            .doOnNext(
                    dataSetRecord -> HOSECodeShiftStatistics.insert(dataSetRecord.getDataSet(), maxSphere, true, false,
                                                                    hoseCodeShifts))
            .doAfterTerminate(() -> {
                System.out.println(" -> hoseCodeShifts size: "
                                           + hoseCodeShifts.size());
                final Map<String, Map<String, Double[]>> hoseCodeShiftStatistics = HOSECodeShiftStatistics.buildHOSECodeShiftStatistics(
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
                                                  .doAfterTerminate(this::saveAllAsMap)
                                                  .subscribe();
            })
            .doOnError(Throwable::printStackTrace)
            .subscribe();
    }

    public Flux<DataSetRecord> getByDataSetSpectrumNucleiAndSource(final String[] nuclei, final String source) {
        final WebClient webClient = this.webClientBuilder.baseUrl(
                                                "http://sherlock-gateway:8080/sherlock-db-service-dataset/")
                                                         .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                        MediaType.APPLICATION_JSON_VALUE)
                                                         .exchangeStrategies(this.exchangeStrategies)
                                                         .build();
        // @TODO take the nuclei order into account when matching -> now it's just an exact array match
        final String nucleiString = Arrays.stream(nuclei)
                                          .reduce("", (concat, current) -> concat
                                                  + current);
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
        if (source
                == null) {
            uriComponentsBuilder.path("/getByNuclei")
                                .queryParam("nuclei", nucleiString);
        } else {
            uriComponentsBuilder.path("/getByNucleiAndSource")
                                .queryParam("nuclei", nucleiString)
                                .queryParam("source", source);
        }

        return webClient.get()
                        .uri(uriComponentsBuilder.toUriString())
                        .retrieve()
                        .bodyToFlux(DataSetRecord.class);
    }

    @GetMapping(value = "/saveAllAsMap")
    public void saveAllAsMap() {

        final Gson gson = new GsonBuilder().create();
        final String pathToHOSECodesFile = "/data/hosecode/hosecodes.json";
        System.out.println("-> store json file in shared volume under \""
                                   + pathToHOSECodesFile
                                   + "\"");

        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");

        this.getAll()
            .doOnNext(hoseCodeObject -> stringBuilder.append(gson.toJson(hoseCodeObject))
                                                     .append(","))
            .doAfterTerminate(() -> {
                stringBuilder.deleteCharAt(stringBuilder.length()
                                                   - 1);
                stringBuilder.append("]");
                FileSystem.writeFile(pathToHOSECodesFile, stringBuilder.toString());
                System.out.println("-> done");
            })
            .subscribe();
    }
}
