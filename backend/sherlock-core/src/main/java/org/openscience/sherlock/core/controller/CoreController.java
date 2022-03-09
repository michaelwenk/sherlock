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

import casekit.nmr.analysis.MultiplicitySectionsBuilder;
import casekit.nmr.elucidation.model.Detections;
import casekit.nmr.elucidation.model.Grouping;
import casekit.nmr.filterandrank.FilterAndRank;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import casekit.nmr.model.nmrium.Correlations;
import casekit.nmr.utils.Utils;
import org.openscience.cdk.exception.CDKException;
import org.openscience.sherlock.core.model.db.ResultRecord;
import org.openscience.sherlock.core.model.exchange.Transfer;
import org.openscience.sherlock.core.utils.Utilities;
import org.openscience.sherlock.core.utils.detection.Detection;
import org.openscience.sherlock.core.utils.elucidation.PyLSD;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping(value = "/")
public class CoreController {

    private final WebClient.Builder webClientBuilder;
    private final ExchangeStrategies exchangeStrategies;
    private final DereplicationController dereplicationController;
    private final ElucidationController elucidationController;
    private final MultiplicitySectionsBuilder multiplicitySectionsBuilder = new MultiplicitySectionsBuilder();

    @Autowired
    public CoreController(final WebClient.Builder webClientBuilder, final ExchangeStrategies exchangeStrategies,
                          final DereplicationController dereplicationController,
                          final ElucidationController elucidationController) {
        this.webClientBuilder = webClientBuilder;
        this.exchangeStrategies = exchangeStrategies;
        this.dereplicationController = dereplicationController;
        this.elucidationController = elucidationController;
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


        final Map<String, Number> tolerances = (Map<String, Number>) requestTransfer.getResultRecord()
                                                                                    .getCorrelations()
                                                                                    .getOptions()
                                                                                    .get("tolerance");
        for (final String atomType : tolerances.keySet()) {
            if (tolerances.get(atomType) instanceof Integer) {
                tolerances.put(atomType, tolerances.get(atomType)
                                                   .doubleValue());
            }
        }
        requestTransfer.getResultRecord()
                       .getCorrelations()
                       .getOptions()
                       .put("tolerance", tolerances);

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
                // unique the dereplication result
                List<DataSet> uniqueDataSetList = new ArrayList<>();
                final Set<String> uniqueDataSetIDs = new HashSet<>();
                String id;
                for (final DataSet dataSet : Objects.requireNonNull(transferResponseEntity.getBody())
                                                    .getDataSetList()) {
                    id = dataSet.getMeta()
                                .get("id");
                    if (!uniqueDataSetIDs.contains(id)) {
                        uniqueDataSetIDs.add(id);
                        uniqueDataSetList.add(dataSet);
                    }
                }

                final Map<String, int[]> multiplicitySectionsSettings;
                try {
                    final Mono<Map<String, int[]>> multiplicitySectionsSettingsMono = Utilities.getMultiplicitySectionsSettings(
                            this.webClientBuilder, this.exchangeStrategies);
                    multiplicitySectionsSettings = multiplicitySectionsSettingsMono.block();
                    this.multiplicitySectionsBuilder.setMinLimit(
                            multiplicitySectionsSettings.get(querySpectrum.getNuclei()[0])[0]);
                    this.multiplicitySectionsBuilder.setMaxLimit(
                            multiplicitySectionsSettings.get(querySpectrum.getNuclei()[0])[1]);
                    this.multiplicitySectionsBuilder.setStepSize(
                            multiplicitySectionsSettings.get(querySpectrum.getNuclei()[0])[2]);

                    uniqueDataSetList = FilterAndRank.filterAndRank(uniqueDataSetList, querySpectrum,
                                                                    requestTransfer.getDereplicationOptions()
                                                                                   .getShiftTolerance(),
                                                                    requestTransfer.getDereplicationOptions()
                                                                                   .getMaximumAverageDeviation(),
                                                                    requestTransfer.getDereplicationOptions()
                                                                                   .isCheckMultiplicity(),
                                                                    requestTransfer.getDereplicationOptions()
                                                                                   .isCheckEquivalencesCount(),
                                                                    this.multiplicitySectionsBuilder, false);
                    Utilities.addMolFileToDataSets(uniqueDataSetList);
                } catch (final Exception e) {
                    responseTransfer.setErrorMessage(e.getMessage());
                    return new ResponseEntity<>(responseTransfer, HttpStatus.NOT_FOUND);
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
                queryTransfer.setGrouping(requestTransfer.getResultRecord()
                                                         .getGrouping());
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
                transferResponseEntity = this.rankAndStore(requestTransfer, correlations,
                                                           queryResultTransfer.getDetections(),
                                                           queryResultTransfer.getGrouping(), dataSetList);
                if (transferResponseEntity.getStatusCode()
                                          .isError()) {
                    System.out.println("ELUCIDATION -> configuration and storage request failed: "
                                               + transferResponseEntity.getBody()
                                                                       .getErrorMessage());
                    responseTransfer.setErrorMessage("ELUCIDATION -> configuration and storage of result failed: "
                                                             + transferResponseEntity.getStatusCode());

                    return new ResponseEntity<>(responseTransfer, transferResponseEntity.getStatusCode());
                }

                responseTransfer.setResultRecord(transferResponseEntity.getBody()
                                                                       .getResultRecord());

                return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
            }

            // DETECTION
            if (requestTransfer.getQueryType()
                               .equals("detection")) {
                final Transfer queryTransfer = new Transfer();
                queryTransfer.setCorrelations(requestTransfer.getResultRecord()
                                                             .getCorrelations());
                queryTransfer.setDetectionOptions(requestTransfer.getResultRecord()
                                                                 .getDetectionOptions());
                queryTransfer.setMf(mf);
                queryTransfer.setDetections(requestTransfer.getResultRecord()
                                                           .getDetections());
                queryTransfer.setElucidationOptions(requestTransfer.getResultRecord()
                                                                   .getElucidationOptions());

                final Transfer queryResultTransfer = Detection.detect(this.webClientBuilder, queryTransfer);
                responseTransfer.getResultRecord()
                                .setCorrelations(queryResultTransfer.getCorrelations());
                responseTransfer.getResultRecord()
                                .setDetections(queryResultTransfer.getDetections());
                responseTransfer.getResultRecord()
                                .setGrouping(queryResultTransfer.getGrouping());
                return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
            }

            // (RE-)PREDICTION
            if (requestTransfer.getQueryType()
                               .equals("prediction")) {
                final Transfer queryTransfer = new Transfer();
                queryTransfer.setCorrelations(requestTransfer.getResultRecord()
                                                             .getCorrelations());
                queryTransfer.setDetectionOptions(requestTransfer.getResultRecord()
                                                                 .getDetectionOptions());
                queryTransfer.setElucidationOptions(requestTransfer.getResultRecord()
                                                                   .getElucidationOptions());
                queryTransfer.setDetections(requestTransfer.getResultRecord()
                                                           .getDetections());
                queryTransfer.setGrouping(requestTransfer.getResultRecord()
                                                         .getGrouping());
                queryTransfer.setMf(mf);
                queryTransfer.setDataSetList(requestTransfer.getResultRecord()
                                                            .getDataSetList());

                ResponseEntity<Transfer> transferResponseEntity = this.elucidationController.predict(queryTransfer);
                if (transferResponseEntity.getStatusCode()
                                          .isError()) {
                    System.out.println("(RE-)PREDICTION -> prediction failed: "
                                               + transferResponseEntity.getBody()
                                                                       .getErrorMessage());
                    responseTransfer.setErrorMessage("(RE-)PREDICTION -> prediction failed: "
                                                             + transferResponseEntity.getStatusCode());

                    return new ResponseEntity<>(responseTransfer, transferResponseEntity.getStatusCode());
                }

                transferResponseEntity = this.rankAndStore(requestTransfer, correlations, queryTransfer.getDetections(),
                                                           queryTransfer.getGrouping(), transferResponseEntity.getBody()
                                                                                                              .getDataSetList());
                if (transferResponseEntity.getStatusCode()
                                          .isError()) {
                    System.out.println("(RE-)PREDICTION -> configuration and storage request failed: "
                                               + transferResponseEntity.getBody()
                                                                       .getErrorMessage());
                    responseTransfer.setErrorMessage("(RE-)PREDICTION -> configuration and storage of result failed: "
                                                             + transferResponseEntity.getStatusCode());

                    return new ResponseEntity<>(responseTransfer, transferResponseEntity.getStatusCode());
                }

                responseTransfer.setResultRecord(transferResponseEntity.getBody()
                                                                       .getResultRecord());

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
        return PyLSD.cancel();
    }

    private ResponseEntity<Transfer> rankAndStore(final Transfer requestTransfer, final Correlations correlations,
                                                  final Detections detections, final Grouping grouping,
                                                  final List<DataSet> dataSetList) {
        final Transfer responseTransfer = new Transfer();
        try {
            Utilities.addMolFileToDataSets(dataSetList);

            final SimpleDateFormat formatter = new SimpleDateFormat("EE MMM d y H:m:s ZZZ");
            final String dateString = formatter.format(new Date());
            final ResultRecord queryResultRecord = new ResultRecord();
            queryResultRecord.setName(requestTransfer.getResultRecord()
                                                     .getName());
            queryResultRecord.setCorrelations(correlations);
            queryResultRecord.setDetections(detections);
            queryResultRecord.setGrouping(grouping);
            queryResultRecord.setDetectionOptions(requestTransfer.getResultRecord()
                                                                 .getDetectionOptions());
            queryResultRecord.setElucidationOptions(requestTransfer.getResultRecord()
                                                                   .getElucidationOptions());
            // store results in DB if not empty and replace resultRecord in responseTransfer
            if (!dataSetList.isEmpty()) {
                FilterAndRank.rank(dataSetList);

                queryResultRecord.setDate(dateString);
                queryResultRecord.setDataSetList(dataSetList);
                queryResultRecord.setDataSetListSize(dataSetList.size());
                queryResultRecord.setPreviewDataSet(dataSetList.get(0));

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
                        responseTransfer.setErrorMessage("Result storage request failed: "
                                                                 + resultRecordResponseEntity.getStatusCode());
                        return new ResponseEntity<>(responseTransfer, resultRecordResponseEntity.getStatusCode());
                    }
                    responseTransfer.setResultRecord(resultRecordResponseEntity.getBody());
                } catch (final Exception e) {
                    responseTransfer.setErrorMessage(e.getMessage());
                    return new ResponseEntity<>(responseTransfer, HttpStatus.NOT_FOUND);
                }
            } else {
                responseTransfer.setResultRecord(queryResultRecord);

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
        } catch (final CDKException e) {
            e.printStackTrace();
        }

        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
    }
}
