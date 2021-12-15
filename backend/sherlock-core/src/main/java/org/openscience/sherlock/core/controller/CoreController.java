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

package org.openscience.sherlock.core.controller;

import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import casekit.nmr.model.nmrium.Correlations;
import casekit.nmr.utils.Utils;
import org.openscience.sherlock.core.model.db.ResultRecord;
import org.openscience.sherlock.core.model.exchange.Transfer;
import org.openscience.sherlock.core.utils.Ranking;
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

    @Autowired
    public CoreController(final WebClient.Builder webClientBuilder, final ExchangeStrategies exchangeStrategies) {
        this.webClientBuilder = webClientBuilder;
        this.exchangeStrategies = exchangeStrategies;
        this.dereplicationController = new DereplicationController(this.webClientBuilder, this.exchangeStrategies);
        this.elucidationController = new ElucidationController(this.webClientBuilder, this.exchangeStrategies);
    }

    @PostMapping(value = "/core", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Transfer> core(@RequestBody final Transfer requestTransfer) {
        final Transfer responseTransfer = new Transfer();
        responseTransfer.setQueryType(requestTransfer.getQueryType());
        responseTransfer.setDereplicationOptions(responseTransfer.getDereplicationOptions());
        responseTransfer.setResultRecord(requestTransfer.getResultRecord());

        final Correlations correlations = requestTransfer.getResultRecord()
                                                         .getCorrelations();
        final Spectrum querySpectrum = Utils.correlationListToSpectrum1D(correlations.getValues(), "13C");

        // INPUT DATA CHECK
        // check whether each signal has a multiplicity; if not stop here
        if (querySpectrum.getSignals()
                         .stream()
                         .anyMatch(signal -> signal.getMultiplicity()
                                 == null)) {
            responseTransfer.setErrorMessage("At least for one carbon the number of attached protons is missing!!!");
            return new ResponseEntity<>(responseTransfer, HttpStatus.BAD_REQUEST);
        }
        final String mf = (String) correlations.getOptions()
                                               .get("mf");
        // check for mf
        if (mf
                == null) {
            responseTransfer.setErrorMessage("Molecular formula is missing!!!");
            return new ResponseEntity<>(responseTransfer, HttpStatus.BAD_REQUEST);
        }
        //        // check for error state
        //        final Map<String, Map<String, Object>> state = requestTransfer.getData()
        //                                                                      .getCorrelations()
        //                                                                      .getState();
        //        final Map<String, Map<String, Boolean>> errors = new HashMap<>();
        //        for (final Map.Entry<String, Map<String, Object>> atomTypeEntry : state.entrySet()) {
        //            if (atomTypeEntry.getValue()
        //                             .containsKey("error")
        //                    && !((Map<String, Object>) atomTypeEntry.getValue()
        //                                                            .get("error")).isEmpty()) {
        //                errors.putIfAbsent(atomTypeEntry.getKey(), new HashMap<>());
        //                for (final Map.Entry<String, Boolean> errorEntry : ((Map<String, Boolean>) atomTypeEntry.getValue()
        //                                                                                                        .get("error")).entrySet()) {
        //                    errors.get(atomTypeEntry.getKey())
        //                          .put(errorEntry.getKey(), errorEntry.getValue());
        //                }
        //            }
        //        }
        //        if (!errors.isEmpty()) {
        //            System.out.println("ERRORS: "
        //                                       + errors);
        //            //            responseTransfer.setErrorMessage("There are errors in correlation data:\n"
        //            //                                                     + errors);
        //            //            return new ResponseEntity<>(responseTransfer, HttpStatus.BAD_REQUEST);
        //        }

        try {
            // DEREPLICATION
            if (requestTransfer.getQueryType()
                               .equals("dereplication")) {
                final Transfer queryTransfer = new Transfer();
                queryTransfer.setCorrelations(requestTransfer.getResultRecord()
                                                             .getCorrelations());
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
                responseTransfer.getResultRecord()
                                .setDataSetList(uniqueDataSetList);
                responseTransfer.getResultRecord()
                                .setDataSetListSize(uniqueDataSetList.size());

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
                queryTransfer.setCorrelations(requestTransfer.getResultRecord()
                                                             .getCorrelations());
                queryTransfer.setDetectionOptions(requestTransfer.getResultRecord()
                                                                 .getDetectionOptions());
                queryTransfer.setElucidationOptions(requestTransfer.getResultRecord()
                                                                   .getElucidationOptions());
                queryTransfer.setDetections(requestTransfer.getResultRecord()
                                                           .getDetections());
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
                final Transfer queryResultTransfer = transferResponseEntity.getBody();
                final List<DataSet> dataSetList = queryResultTransfer.getDataSetList()
                                                          != null
                                                  ? queryResultTransfer.getDataSetList()
                                                  : new ArrayList<>();

                // store results in DB if not empty and replace resultRecord in responseTransfer
                if (!dataSetList.isEmpty()) {
                    Ranking.rankDataSetList(dataSetList);

                    final SimpleDateFormat formatter = new SimpleDateFormat("EE MMM d y H:m:s ZZZ");
                    final String dateString = formatter.format(new Date());
                    final ResultRecord queryResultRecord = new ResultRecord();
                    queryResultRecord.setDataSetList(dataSetList);
                    queryResultRecord.setName(requestTransfer.getResultRecord()
                                                             .getName());
                    queryResultRecord.setDataSetListSize(dataSetList.size());
                    queryResultRecord.setDate(dateString);
                    queryResultRecord.setPreviewDataSet(dataSetList.get(0));
                    queryResultRecord.setCorrelations(correlations);
                    queryResultRecord.setDetections(queryResultTransfer.getDetections());
                    queryResultRecord.setDetectionOptions(requestTransfer.getResultRecord()
                                                                         .getDetectionOptions());
                    queryResultRecord.setElucidationOptions(requestTransfer.getResultRecord()
                                                                           .getElucidationOptions());

                    final WebClient webClient = this.webClientBuilder.baseUrl(
                                                            "http://sherlock-gateway:8080/sherlock-db-service-result/insert")
                                                                     .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                                    MediaType.APPLICATION_JSON_VALUE)
                                                                     .exchangeStrategies(this.exchangeStrategies)
                                                                     .build();
                    try {
                        final ResponseEntity<ResultRecord> resultRecordResponseEntity = webClient.post()
                                                                                                 .bodyValue(
                                                                                                         queryResultRecord)
                                                                                                 .retrieve()
                                                                                                 .toEntity(
                                                                                                         ResultRecord.class)
                                                                                                 .block();
                        if (resultRecordResponseEntity.getStatusCode()
                                                      .isError()) {
                            System.out.println("Result storage request failed: "
                                                       + resultRecordResponseEntity.getStatusCode());
                            responseTransfer.setErrorMessage("Result storage request failed: "
                                                                     + resultRecordResponseEntity.getStatusCode());
                            transferResponseEntity = new ResponseEntity<>(responseTransfer,
                                                                          resultRecordResponseEntity.getStatusCode());
                            return transferResponseEntity;
                        }
                        System.out.println("resultRecord: "
                                                   + resultRecordResponseEntity.getBody());
                        responseTransfer.setResultRecord(resultRecordResponseEntity.getBody());
                    } catch (final Exception e) {
                        responseTransfer.setErrorMessage(e.getMessage());
                        return new ResponseEntity<>(responseTransfer, HttpStatus.NOT_FOUND);
                    }
                } else {
                    responseTransfer.getResultRecord()
                                    .setDataSetList(new ArrayList<>());
                    responseTransfer.getResultRecord()
                                    .setDataSetListSize(0);
                    responseTransfer.getResultRecord()
                                    .setPreviewDataSet(null);
                    responseTransfer.getResultRecord()
                                    .setDate(null);
                    responseTransfer.getResultRecord()
                                    .setId(null);
                }

                return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
            }

            // DETECTION
            if (requestTransfer.getQueryType()
                               .equals("detection")) {
                final WebClient webClient = this.webClientBuilder.baseUrl(
                                                        "http://sherlock-gateway:8080/sherlock-pylsd/detect")
                                                                 .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                                MediaType.APPLICATION_JSON_VALUE)
                                                                 .build();
                final Transfer queryTransfer = new Transfer();
                queryTransfer.setCorrelations(requestTransfer.getResultRecord()
                                                             .getCorrelations());
                queryTransfer.setDetectionOptions(requestTransfer.getResultRecord()
                                                                 .getDetectionOptions());
                queryTransfer.setMf(mf);
                queryTransfer.setDetections(requestTransfer.getResultRecord()
                                                           .getDetections());

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
                responseTransfer.getResultRecord()
                                .setDetections(queryResultTransfer.getDetections());
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
        final WebClient webClient = this.webClientBuilder.baseUrl("http://sherlock-gateway:8080/sherlock-pylsd/cancel")
                                                         .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                        MediaType.APPLICATION_JSON_VALUE)
                                                         .build();
        return webClient.get()
                        .retrieve()
                        .toEntity(Transfer.class)
                        .block();
    }
}
