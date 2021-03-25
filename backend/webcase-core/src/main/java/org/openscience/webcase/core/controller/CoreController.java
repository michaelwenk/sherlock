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

import org.openscience.webcase.core.model.exchange.Transfer;
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

import java.util.ArrayList;
import java.util.UUID;

@RestController
@RequestMapping(value = "/")
public class CoreController {

    private final String PATH_TO_PYLSD_EXECUTABLE_FOLDER = "/Users/mwenk/work/software/PyLSD-a4/Variant/";
    private final String PATH_TO_LSD_FILTER_LIST = "/Users/mwenk/work/software/PyLSD-a4/LSD/Filters/list.txt";
    private final String PATH_TO_PYLSD_INPUT_FILE_FOLDER = "/Users/mwenk/Downloads/temp_webCASE/";
    //    private final String pathToPyLSDOutputFileFolder = "/Users/mwenk/Downloads/temp_webCASE/";
    //    private final String pathToPyLSDLogAndErrorFolder = "/Users/mwenk/Downloads/temp_webCASE/";

    @Autowired
    private WebClient.Builder webClientBuilder;

    @PostMapping(value = "/core", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Transfer> core(@RequestBody final Transfer requestTransfer) {
        final Transfer transfer = new Transfer();
        try {
            // set ExchangeSettings
            final ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                                                                            .codecs(configurer -> configurer.defaultCodecs()
                                                                                                            .maxInMemorySize(
                                                                                                                    1024
                                                                                                                            * 100000))
                                                                            .build();
            // DEREPLICATION
            if (requestTransfer.getQueryType()
                               .equals("Dereplication")) {
                final WebClient webClient = this.webClientBuilder.
                                                                         baseUrl("http://localhost:8081/webcase-dereplication/dereplication")
                                                                 .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                                MediaType.APPLICATION_JSON_VALUE)
                                                                 .exchangeStrategies(exchangeStrategies)
                                                                 .build();
                final Transfer queryTransfer = new Transfer();
                queryTransfer.setData(requestTransfer.getData());
                queryTransfer.setDereplicationOptions(requestTransfer.getDereplicationOptions());
                final Transfer queryResultTransfer = webClient //final Flux<DataSet> results = webClient
                                                               .post()
                                                               .bodyValue(queryTransfer)
                                                               .retrieve()
                                                               .bodyToMono(Transfer.class)
                                                               .block();
                transfer.setDataSetList(queryResultTransfer.getDataSetList());
                return new ResponseEntity<>(transfer, HttpStatus.OK);

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
                                                                 .exchangeStrategies(exchangeStrategies)
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

                final Transfer queryResultTransfer = webClient //final Flux<DataSet> results = webClient
                                                               .post()
                                                               .bodyValue(queryTransfer)
                                                               .retrieve()
                                                               .bodyToMono(Transfer.class)
                                                               .block();
                transfer.setDataSetList(queryResultTransfer.getDataSetList());
                transfer.setRequestID(requestID);
                return new ResponseEntity<>(transfer, HttpStatus.OK);
            }

            if (requestTransfer.getQueryType()
                               .equals("Retrieval")) {
                System.out.println("RETRIEVAL: "
                                           + requestTransfer.getRetrievalID());
                final String pathToResultsFile = this.PATH_TO_PYLSD_INPUT_FILE_FOLDER
                        + "/"
                        + requestTransfer.getRetrievalID()
                        + "/"
                        + requestTransfer.getRetrievalID()
                        + ".smiles";
                final WebClient webClient = this.webClientBuilder.baseUrl(
                        "http://localhost:8081/webcase-result-retrieval")
                                                                 .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                                MediaType.APPLICATION_JSON_VALUE)
                                                                 .build();
                final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
                uriComponentsBuilder.path("/retrieveResultFromFile")
                                    .queryParam("pathToResultsFile", pathToResultsFile);

                // retrieve results
                final Transfer queryResultTransfer = webClient.get()
                                                              .uri(uriComponentsBuilder.toUriString())
                                                              .retrieve()
                                                              .bodyToMono(Transfer.class)
                                                              .block();
                System.out.println("--> list of results: "
                                           + queryResultTransfer.getDataSetList()
                                                                .size()
                                           + " -> "
                                           + queryResultTransfer.getDataSetList());
                transfer.setDataSetList(queryResultTransfer.getDataSetList());
                transfer.setRequestID(requestTransfer.getRetrievalID());
                return new ResponseEntity<>(transfer, HttpStatus.OK);

            }
        } catch (final Exception e) {
            System.err.println("An error occurred: "
                                       + e.getMessage());
        }

        transfer.setDataSetList(new ArrayList<>());
        return new ResponseEntity<>(transfer, HttpStatus.OK);
    }
}
