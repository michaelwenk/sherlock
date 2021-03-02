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
import org.openscience.webcase.core.model.exchange.Transfer;
import org.openscience.webcase.core.model.nmrdisplayer.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@RestController
@RequestMapping(value = "/")
public class CoreController {

    @Autowired
    private WebClient.Builder webClientBuilder;

    @PostMapping(value = "/core", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Transfer> core(@RequestBody final Data data, @RequestParam final boolean dereplicate, @RequestParam final boolean allowHeteroHeteroBonds) {
        final Transfer transfer = new Transfer();
        List<DataSet> solutions = new ArrayList<>();
        try {
            // NEW UUID CREATION
            final String requestID = UUID.randomUUID().toString();
            // set ExchangeSettings
            final ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 100000)).build();
            // DEREPLICATION
            if (dereplicate) {
                    final WebClient webClient = webClientBuilder.
                        baseUrl("http://localhost:8081/webcase-dereplication/dereplication")
                            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .exchangeStrategies(exchangeStrategies)
                        .build();
                    final Transfer queryTransfer = new Transfer();
                    queryTransfer.setData(data);
                    final Transfer queryResultTransfer = webClient //final Flux<DataSet> results = webClient
                            .post()
                            .bodyValue(queryTransfer)
                            .retrieve()
                            .bodyToMono(Transfer.class).block();
                solutions = queryResultTransfer.getDataSetList();

//                    if (solutions.size() > 0) {
                        System.out.println("DEREPLICATION WAS SUCCESSFUL: " + solutions.size());
                        transfer.setDataSetList(solutions);
                        return new ResponseEntity<>(transfer, HttpStatus.OK);
//                    }
            }

            // @TODO check possible structural input (incl. assignment) by nmr-displayer

            // @TODO SUBSTRUCTURE SEARCH

            // PyLSD FILE CONTENT CREATION
//            final String pyLSDFileContent = PyLSDInputFileBuilder.buildPyLSDFileContent(data, mf, getDetectedHybridizations(data, thrsHybridizations), allowHeteroHeteroBonds, PATH_TO_LSD_FILTER_LIST, uuid.toString());
            final WebClient webClient = webClientBuilder.
                    baseUrl("http://localhost:8081/webcase-elucidation")
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .exchangeStrategies(exchangeStrategies)
                    .build();
            final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
            uriComponentsBuilder.path("/elucidation")
                    .queryParam("allowHeteroHeteroBonds", allowHeteroHeteroBonds)
                    .queryParam("requestID", requestID);
            final Transfer queryTransfer = new Transfer();
            queryTransfer.setData(data);
            final Transfer queryResultTransfer = webClient //final Flux<DataSet> results = webClient
                    .post()
                    .uri(uriComponentsBuilder.toUriString())
                    .bodyValue(queryTransfer)
                    .retrieve()
                    .bodyToMono(Transfer.class).block();
            solutions = queryResultTransfer.getDataSetList();
        } catch (final Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
        }

        transfer.setDataSetList(solutions);
        return new ResponseEntity<>(transfer, HttpStatus.OK);
    }
}
