/*
 * MIT License
 *
 * Copyright (c) 2020 Michael Wenk (https://github.com/michaelwenk)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.openscience.webcase.dbservice.dataset.controller;

import org.openscience.webcase.dbservice.dataset.model.DataSet;
import org.openscience.webcase.dbservice.dataset.model.exchange.Transfer;
import org.openscience.webcase.dbservice.dataset.nmrshiftdb.model.DataSetRecord;
import org.openscience.webcase.dbservice.dataset.nmrshiftdb.service.DataSetServiceImplementation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;


@RestController
@RequestMapping(value = "/nmrshiftdb")
public class NMRShiftDBController {
    
    @Autowired
    private WebClient.Builder webClientBuilder;

    private final DataSetServiceImplementation dataSetServiceImplementation;

    public NMRShiftDBController(final DataSetServiceImplementation dataSetServiceImplementation) {
        this.dataSetServiceImplementation = dataSetServiceImplementation;
    }

    @GetMapping(value = "/count")
    public Mono<Long> getCount() {
        return this.dataSetServiceImplementation.count();
    }

    @GetMapping(value = "/getAll", produces = "application/stream+json")
    public Flux<DataSetRecord> getAll() {
        return this.dataSetServiceImplementation.findAll();
    }

    @GetMapping(value = "/getByMf", produces = "application/stream+json")
    public Flux<DataSetRecord> getByMf(@RequestParam final String mf) {
        return this.dataSetServiceImplementation.findByMf(mf);
    }

    @GetMapping(value = "/getByNuclei", produces = "application/stream+json")
    public Flux<DataSetRecord> getByDataSetSpectrumNuclei(@RequestParam final String[] nuclei) {
        return this.dataSetServiceImplementation.findByDataSetSpectrumNuclei(nuclei);
    }

    @GetMapping(value = "/getByNucleiAndSignalCount", produces = "application/stream+json")
    public Flux<DataSetRecord> getByDataSetSpectrumNucleiAndDataSetSpectrumSignalCount(@RequestParam final String[] nuclei, @RequestParam final int signalCount) {
        return this.dataSetServiceImplementation.findByDataSetSpectrumNucleiAndDataSetSpectrumSignalCount(nuclei, signalCount);
    }

    @GetMapping(value = "/getByNucleiAndSignalCountAndMf", produces = "application/stream+json")
    public Flux<DataSetRecord> getByDataSetSpectrumNucleiAndDataSetSpectrumSignalCountAndMf(@RequestParam final String[] nuclei, @RequestParam final int signalCount, @RequestParam final String mf) {
        return this.dataSetServiceImplementation.findByDataSetSpectrumNucleiAndDataSetSpectrumSignalCountAndMf(nuclei, signalCount, mf);
    }

    @PostMapping(value = "/insert", consumes = "application/json")
    public void insert(@RequestBody final DataSetRecord dataSetRecord) {
        this.dataSetServiceImplementation.insert(dataSetRecord).block();
    }

    @DeleteMapping(value = "/delete/all")
    public void deleteAll() {
        this.dataSetServiceImplementation.deleteAll().block();
    }

    @PostMapping(value = "/replace/all", consumes = "text/plain")
    public void replaceAll(@RequestParam final String filePath, @RequestParam final String[] nuclei) {
        this.deleteAll();
        
        final ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                                                                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1000000)).build();
        final WebClient webClient = webClientBuilder.
                                            baseUrl("http://localhost:8081/webcase-casekit/dbservice")
                                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                                    .exchangeStrategies(exchangeStrategies)
                                    .build();
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
        uriComponentsBuilder.path("/getDataSetsFromNMRShiftDB")
                            .queryParam("pathToNMRShiftDB", filePath)
                            .queryParam("nuclei", String.join(",", nuclei));
        System.out.println(uriComponentsBuilder.toUriString());
        final Transfer queryResultTransfer = webClient
                .get()
                .uri(uriComponentsBuilder.toUriString())
                .retrieve()
                .bodyToMono(Transfer.class)
                .block();

        queryResultTransfer.getDataSetList().forEach(dataSet -> this.insert(new DataSetRecord(null, dataSet.getMeta().get("mf"), dataSet)));
    }
}
