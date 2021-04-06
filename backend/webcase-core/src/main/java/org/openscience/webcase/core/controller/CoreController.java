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

package org.openscience.webcase.core.controller;

import org.openscience.webcase.core.model.DataSet;
import org.openscience.webcase.core.model.Signal;
import org.openscience.webcase.core.model.Spectrum;
import org.openscience.webcase.core.model.exchange.Transfer;
import org.openscience.webcase.core.utils.Utils;
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
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/")
public class CoreController {

    // set ExchangeSettings
    final int maxInMemorySizeMB = 1000;
    final ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                                                                    .codecs(configurer -> configurer.defaultCodecs()
                                                                                                    .maxInMemorySize(
                                                                                                            this.maxInMemorySizeMB
                                                                                                                    * 1024
                                                                                                                    * 1024))
                                                                    .build();
    private final String PATH_TO_PYLSD_EXECUTABLE_FOLDER = "/Users/mwenk/work/software/PyLSD-a4/Variant/";
    //    private final String pathToPyLSDOutputFileFolder = "/Users/mwenk/Downloads/temp_webCASE/";
    //    private final String pathToPyLSDLogAndErrorFolder = "/Users/mwenk/Downloads/temp_webCASE/";
    private final String PATH_TO_LSD_FILTER_LIST = "/Users/mwenk/work/software/PyLSD-a4/LSD/Filters/list.txt";
    private final String PATH_TO_PYLSD_INPUT_FILE_FOLDER = "/Users/mwenk/Downloads/temp_webCASE/";
    @Autowired
    private WebClient.Builder webClientBuilder;

    @PostMapping(value = "/core", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Transfer> core(@RequestBody final Transfer requestTransfer) {
        final Transfer responseTransfer = new Transfer();
        responseTransfer.setQueryType(requestTransfer.getQueryType());

        final Spectrum querySpectrum = new Spectrum();
        querySpectrum.setNuclei(new String[]{"13C"});
        querySpectrum.setSignals(requestTransfer.getData()
                                                .getCorrelations()
                                                .getValues()
                                                .stream()
                                                .filter(correlation -> correlation.getAtomType()
                                                                                  .equals("C"))
                                                .map(correlation -> new Signal(querySpectrum.getNuclei(), new Double[]{
                                                        correlation.getSignal().getDelta()},
                                                                               Utils.getMultiplicityFromProtonsCount(
                                                                                       correlation), null,
                                                                               correlation.getSignal()
                                                                                          .getKind(),
                                                                               correlation.getEquivalence(),
                                                                               correlation.getSignal()
                                                                                          .getSign()))
                                                .collect(Collectors.toList()));
        querySpectrum.setSignalCount(querySpectrum.getSignals()
                                                  .size());

        // check whether each signal has a multiplicity; if not stop here
        if (querySpectrum.getSignals()
                         .stream()
                         .anyMatch(signal -> signal.getMultiplicity()
                                 == null)) {
            responseTransfer.setDataSetList(new ArrayList<>());
            return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
        }
        final String mf = (String) requestTransfer.getData()
                                                  .getCorrelations()
                                                  .getOptions()
                                                  .get("mf");

        try {
            // DEREPLICATION
            if (requestTransfer.getQueryType()
                               .equals("Dereplication")) {
                final WebClient webClient = this.webClientBuilder.
                                                                         baseUrl("http://localhost:8081/webcase-dereplication/dereplication")
                                                                 .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                                MediaType.APPLICATION_JSON_VALUE)
                                                                 .exchangeStrategies(this.exchangeStrategies)
                                                                 .build();
                final Transfer queryTransfer = new Transfer();
                queryTransfer.setData(requestTransfer.getData());
                queryTransfer.setDereplicationOptions(requestTransfer.getDereplicationOptions());
                queryTransfer.setQueryType(requestTransfer.getQueryType());
                queryTransfer.setQuerySpectrum(querySpectrum);
                queryTransfer.setMf(mf);
                final Transfer queryResultTransfer = webClient //final Flux<DataSet> results = webClient
                                                               .post()
                                                               .bodyValue(queryTransfer)
                                                               .retrieve()
                                                               .bodyToMono(Transfer.class)
                                                               .block();
                responseTransfer.setDataSetList(queryResultTransfer.getDataSetList());
                return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
            }

            // @TODO check possible structural input (incl. assignment) by nmr-displayer

            // @TODO SUBSTRUCTURE SEARCH

            if (requestTransfer.getQueryType()
                               .equals("Elucidation")) {
                // NEW UUID CREATION
                final String requestID = UUID.randomUUID()
                                             .toString();
                // PyLSD FILE CONTENT CREATION
                final WebClient webClient = this.webClientBuilder.
                                                                         baseUrl("http://localhost:8081/webcase-elucidation/elucidation")
                                                                 .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                                MediaType.APPLICATION_JSON_VALUE)
                                                                 .exchangeStrategies(this.exchangeStrategies)
                                                                 .build();
                final Transfer queryTransfer = new Transfer();
                queryTransfer.setData(requestTransfer.getData());
                queryTransfer.setElucidationOptions(requestTransfer.getElucidationOptions());
                queryTransfer.setRequestID(requestID);
                queryTransfer.getElucidationOptions()
                             .setPathToPyLSDExecutableFolder(this.PATH_TO_PYLSD_EXECUTABLE_FOLDER);
                queryTransfer.getElucidationOptions()
                             .setPathToLSDFilterList(this.PATH_TO_LSD_FILTER_LIST);
                queryTransfer.getElucidationOptions()
                             .setPathToPyLSDInputFileFolder(this.PATH_TO_PYLSD_INPUT_FILE_FOLDER);
                queryTransfer.setQueryType(requestTransfer.getQueryType());
                queryTransfer.setQuerySpectrum(querySpectrum);
                queryTransfer.setMf(mf);

                final Transfer queryResultTransfer = webClient //final Flux<DataSet> results = webClient
                                                               .post()
                                                               .bodyValue(queryTransfer)
                                                               .retrieve()
                                                               .bodyToMono(Transfer.class)
                                                               .block();
                responseTransfer.setDataSetList(queryResultTransfer.getDataSetList());
                responseTransfer.setResultID(queryResultTransfer.getResultID());
                return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
            }

            if (requestTransfer.getQueryType()
                               .equals("Retrieval")) {
                System.out.println("RETRIEVAL: "
                                           + requestTransfer.getResultID());
                final WebClient webClient = this.webClientBuilder.baseUrl(
                        "http://localhost:8081/webcase-result/retrieve")
                                                                 .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                                MediaType.APPLICATION_JSON_VALUE)
                                                                 .build();
                final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
                uriComponentsBuilder.path("/retrieveResultFromDatabase")
                                    .queryParam("resultID", requestTransfer.getResultID());

                // retrieve results
                final Flux<DataSet> dataSetFlux = webClient.get()
                                                           .uri(uriComponentsBuilder.toUriString())
                                                           .retrieve()
                                                           .bodyToFlux(DataSet.class);
                responseTransfer.setDataSetList(dataSetFlux.collectList()
                                                           .block());
                responseTransfer.setResultID(requestTransfer.getResultID());
                return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
            }
        } catch (final Exception e) {
            System.err.println("An error occurred: ");
            e.printStackTrace();
        }

        responseTransfer.setDataSetList(new ArrayList<>());
        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
    }
}
