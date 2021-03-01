/*
 * MIT License
 *
 * Copyright (c) 2021 Michael Wenk (https://github.com/michaelwenk)
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

package org.openscience.webcase.dereplication.controller;

import org.openscience.webcase.dereplication.model.DataSet;
import org.openscience.webcase.dereplication.model.Signal;
import org.openscience.webcase.dereplication.model.Spectrum;
import org.openscience.webcase.dereplication.model.exchange.Transfer;
import org.openscience.webcase.dereplication.model.nmrdisplayer.Correlation;
import org.openscience.webcase.dereplication.model.nmrdisplayer.Data;
import org.openscience.webcase.dereplication.nmrshiftdb.model.DataSetRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/")
public class DereplicationController {

    @Autowired
    private WebClient.Builder webClientBuilder;

    @PostMapping(value = "dereplication", consumes = "application/json", produces = "application/json") //, produces = "application/stream+json")
    public ResponseEntity<Transfer> dereplication(@RequestBody final Transfer requestTransfer, @RequestParam final String requestID) {
        final Data data = requestTransfer.getData();
        final Transfer resultTransfer = new Transfer();
        final Spectrum querySpectrum = new Spectrum();
        querySpectrum.setNuclei(new String[]{"13C"});
        querySpectrum.setSignals(data.getCorrelations().getValues().stream().filter(correlation -> correlation.getAtomType().equals("C")).map(correlation -> new Signal(querySpectrum.getNuclei(), new Double[]{correlation.getSignal().getDelta()}, this.getMultiplicityFromProtonsCount(correlation), null, correlation.getSignal().getKind(), correlation.getEquivalence(), correlation.getSignal().getSign())).collect(Collectors.toList()));
        querySpectrum.setSignalCount(querySpectrum.getSignals().size());
        // check whether each signal has a multiplicity
        if (querySpectrum.getSignals().stream().noneMatch(signal -> signal.getMultiplicity() == null)) {
            final String mf = (String) data.getCorrelations().getOptions().get("mf");
            final Map<String, Double> shiftTolerances = (HashMap<String, Double>) data.getCorrelations().getOptions().get("tolerance");

            if (querySpectrum.getNuclei().length == 1) {
                List<DataSet> results = dereplicate1D(querySpectrum, mf, shiftTolerances.get("C"))
                        .collectList()
                        .block()
                        .stream()
                        .map(dataSetRecord -> {
                            final DataSet dataSet = dataSetRecord.getDataSet();
                            dataSet.addMetaInfo("determination", "dereplication");
                            return dataSet;
                        })
                        .collect(Collectors.toList());

                final ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 100000)).build();
                final WebClient webClient = this.webClientBuilder.baseUrl("http://localhost:8081/webcase-ranking-spectral-similarity").
                        defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .exchangeStrategies(exchangeStrategies)
                        .build();
                final Transfer queryTransfer = new Transfer();
                queryTransfer.setDataSetList(results);
                queryTransfer.setQuerySpectrum(querySpectrum);
                queryTransfer.setShiftTolerances(shiftTolerances);
                results = webClient
                        .post()
                        .uri("/rankBySpectralSimilarity")
                        .bodyValue(queryTransfer)
                        .retrieve()
                        .bodyToMono(Transfer.class).block().getDataSetList();

                resultTransfer.setDataSetList(results);
                return new ResponseEntity<>(resultTransfer, HttpStatus.OK);
            }
        }

        resultTransfer.setDataSetList(new ArrayList<>());
        return new ResponseEntity<>(resultTransfer, HttpStatus.OK);
    }

    public Flux<DataSetRecord> dereplicate1D(final Spectrum querySpectrum, final String mf, final double shiftTol) {
        final ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 100000)).build();
        final WebClient webClient = this.webClientBuilder.baseUrl("http://localhost:8081/webcase-db-service-dataset/nmrshiftdb").
                    defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(exchangeStrategies)
                .build();
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();

        if (mf != null) {
            uriComponentsBuilder //.scheme("http").host("webcase-db-service-dataset")
                    .path("/getByMf").queryParam("mf", mf);
            return webClient
                            .get()
                            .uri(uriComponentsBuilder.toUriString())
                            .retrieve()
                            .bodyToFlux(DataSetRecord.class);
        }
        // @TODO take the nuclei order into account when matching -> now it's just an exact array match
        final String nucleiString = Arrays.stream(querySpectrum.getNuclei()).reduce("", (concat, current) -> concat + current );
        uriComponentsBuilder //.scheme("http").host("webcase-db-service-dataset")
                .path("/getByNucleiAndSignalCount").queryParam("nuclei", nucleiString).queryParam("signalCount", querySpectrum.getSignalCount());

        return webClient
                .get()
                .uri(uriComponentsBuilder.toUriString())
                .retrieve()
                .bodyToFlux(DataSetRecord.class);
    }

    /**
     * Specified for carbons only -> not generic!!!
     */
    public String getMultiplicityFromProtonsCount(final Correlation correlation) {
        if (correlation.getAtomType().equals("C") && correlation.getProtonsCount().size() == 1) {
            switch (correlation.getProtonsCount().get(0)) {
                case 0:
                    return "s";
                case 1:
                    return "d";
                case 2:
                    return "t";
                case 3:
                    return "q";
                default:
                    return null;
            }
        }
        return null;
    }
}
