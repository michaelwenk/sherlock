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

import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import casekit.nmr.utils.Utils;
import org.openscience.webcase.core.model.db.ResultRecord;
import org.openscience.webcase.core.model.exchange.Transfer;
import org.openscience.webcase.core.utils.Ranking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping(value = "/")
public class CoreController {

    private final WebClient.Builder webClientBuilder;
    private final ExchangeStrategies exchangeStrategies;
    private final DereplicationController dereplicationController;
    private final ElucidationController elucidationController;
    private final ResultController resultController;

    @Autowired
    public CoreController(final WebClient.Builder webClientBuilder, final ExchangeStrategies exchangeStrategies) {
        this.webClientBuilder = webClientBuilder;
        this.exchangeStrategies = exchangeStrategies;
        this.dereplicationController = new DereplicationController(this.webClientBuilder, this.exchangeStrategies);
        this.elucidationController = new ElucidationController(this.webClientBuilder, this.exchangeStrategies);
        this.resultController = new ResultController(this.webClientBuilder, this.exchangeStrategies);
    }

    @PostMapping(value = "/core", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Transfer> core(@RequestBody final Transfer requestTransfer) {
        final Transfer responseTransfer = new Transfer();
        responseTransfer.setQueryType(requestTransfer.getQueryType());
        
        final Spectrum querySpectrum = Utils.correlationListToSpectrum1D(requestTransfer.getData()
                                                                                        .getCorrelations()
                                                                                        .getValues(), "13C");
        // check whether each signal has a multiplicity; if not stop here
        if (querySpectrum.getSignals()
                         .stream()
                         .anyMatch(signal -> signal.getMultiplicity()
                                 == null)) {
            responseTransfer.setDataSetList(new ArrayList<>());
            responseTransfer.setErrorMessage("At least for one carbon the number of attached protons is missing!!!");
            return new ResponseEntity<>(responseTransfer, HttpStatus.BAD_REQUEST);
        }
        final String mf = (String) requestTransfer.getData()
                                                  .getCorrelations()
                                                  .getOptions()
                                                  .get("mf");

        try {
            // DEREPLICATION
            if (requestTransfer.getQueryType()
                               .equals("dereplication")) {
                final Transfer queryTransfer = new Transfer();
                queryTransfer.setData(requestTransfer.getData());
                queryTransfer.setDereplicationOptions(requestTransfer.getDereplicationOptions());
                queryTransfer.setQueryType(requestTransfer.getQueryType());
                queryTransfer.setQuerySpectrum(querySpectrum);
                queryTransfer.setMf(mf);
                final ResponseEntity<Transfer> transferResponseEntity = this.dereplicationController.dereplicate(
                        queryTransfer);
                if (transferResponseEntity.getStatusCode()
                                          .isError()) {
                    System.out.println("DEREPLICATION request failed: "
                                               + Objects.requireNonNull(transferResponseEntity.getBody())
                                                        .getErrorMessage());

                    return transferResponseEntity;
                }
                final List<DataSet> dataSetList = Objects.requireNonNull(transferResponseEntity.getBody())
                                                         .getDataSetList();
                Ranking.rankDataSetList(dataSetList);

                // unique the dereplication result
                final List<DataSet> uniqueDataSetList = new ArrayList<>();
                final Set<String> uniqueDataSetIDs = new HashSet<>();
                String id;
                for (final DataSet dataSet : dataSetList) {
                    id = dataSet.getMeta()
                                .get("id");
                    if (!uniqueDataSetIDs.contains(id)) {
                        uniqueDataSetIDs.add(id);
                        uniqueDataSetList.add(dataSet);
                    }
                }
                final ResultRecord responseResultRecord = new ResultRecord();
                responseResultRecord.setDataSetList(uniqueDataSetList);
                responseTransfer.setResultRecord(responseResultRecord);

                return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
            }

            // @TODO check possible structural input (incl. assignment) by NMRium

            // @TODO SUBSTRUCTURE SEARCH

            if (requestTransfer.getQueryType()
                               .equals("elucidation")) {
                // NEW UUID CREATION
                final String requestID = UUID.randomUUID()
                                             .toString();
                // PyLSD RUN
                final Transfer queryTransfer = new Transfer();
                queryTransfer.setData(requestTransfer.getData());
                queryTransfer.setDetectionOptions(requestTransfer.getDetectionOptions());
                queryTransfer.setElucidationOptions(requestTransfer.getElucidationOptions());
                queryTransfer.setDetections(requestTransfer.getDetections());
                queryTransfer.setRequestID(requestID);
                queryTransfer.setMf(mf);

                ResponseEntity<Transfer> transferResponseEntity = this.elucidationController.elucidate(queryTransfer);
                if (transferResponseEntity.getStatusCode()
                                          .isError()) {
                    System.out.println("ELUCIDATION request failed: "
                                               + transferResponseEntity.getBody()
                                                                       .getErrorMessage());

                    return transferResponseEntity;
                }
                Transfer queryResultTransfer = transferResponseEntity.getBody();
                final List<DataSet> dataSetList = queryResultTransfer.getDataSetList()
                                                          != null
                                                  ? queryResultTransfer.getDataSetList()
                                                  : new ArrayList<>();
                Ranking.rankDataSetList(dataSetList);
                responseTransfer.setDataSetList(dataSetList);
                responseTransfer.setDetections(queryResultTransfer.getDetections());

                // store results in DB if not empty
                if (!dataSetList.isEmpty()) {
                    final ResultRecord queryResultRecord = new ResultRecord();
                    queryResultRecord.setDataSetList(dataSetList);
                    queryResultRecord.setName(requestTransfer.getResultRecord()
                                                             .getName());
                    final SimpleDateFormat formatter = new SimpleDateFormat("EE MMM d y H:m:s ZZZ");
                    final String dateString = formatter.format(new Date());
                    queryResultRecord.setDate(dateString);

                    transferResponseEntity = this.resultController.store(queryResultRecord);
                    if (transferResponseEntity.getStatusCode()
                                              .isError()) {
                        System.out.println("RESULT storage request failed: "
                                                   + Objects.requireNonNull(transferResponseEntity.getBody())
                                                            .getErrorMessage());

                        return transferResponseEntity;
                    }
                    queryResultTransfer = transferResponseEntity.getBody();
                    if (queryResultTransfer.getResultRecord()
                            != null) {
                        System.out.println("resultRecord: "
                                                   + queryResultTransfer.getResultRecord());
                        responseTransfer.setResultRecord(queryResultTransfer.getResultRecord());
                    }
                }

                return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
            }

            // DETECTION
            if (requestTransfer.getQueryType()
                               .equals("detection")) {
                final WebClient webClient = this.webClientBuilder.baseUrl(
                                                        "http://webcase-gateway:8080/webcase-pylsd/detect")
                                                                 .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                                MediaType.APPLICATION_JSON_VALUE)
                                                                 .build();
                final Transfer queryTransfer = new Transfer();
                queryTransfer.setData(requestTransfer.getData());
                queryTransfer.setDetectionOptions(requestTransfer.getDetectionOptions());
                queryTransfer.setMf(mf);

                final Transfer queryResultTransfer = webClient.post()
                                                              .bodyValue(queryTransfer)
                                                              .retrieve()
                                                              .bodyToMono(Transfer.class)
                                                              .block();
                if (queryResultTransfer
                        == null) {
                    responseTransfer.setErrorMessage("Could not detect connectivities!");
                    return new ResponseEntity<>(responseTransfer, HttpStatus.INTERNAL_SERVER_ERROR);
                }

                responseTransfer.setDetections(queryResultTransfer.getDetections());
                return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
            }
        } catch (final Exception e) {
            System.err.println("An error occurred: ");
            e.printStackTrace();

            responseTransfer.setErrorMessage(e.getMessage());
            return new ResponseEntity<>(responseTransfer, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        responseTransfer.setDataSetList(new ArrayList<>());
        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
    }

    @GetMapping(value = "/cancel")
    public ResponseEntity<Transfer> cancel() {
        final WebClient webClient = this.webClientBuilder.baseUrl("http://webcase-gateway:8080/webcase-pylsd/cancel")
                                                         .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                        MediaType.APPLICATION_JSON_VALUE)
                                                         .build();
        return webClient.get()
                        .retrieve()
                        .toEntity(Transfer.class)
                        .block();
    }
}
