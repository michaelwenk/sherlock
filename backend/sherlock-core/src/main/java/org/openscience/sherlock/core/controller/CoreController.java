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

import casekit.nmr.filterandrank.FilterAndRank;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import casekit.nmr.model.SpectrumCompact;
import casekit.nmr.model.nmrium.Correlations;
import casekit.nmr.utils.Utils;
import org.bson.types.ObjectId;
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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping(value = "/")
public class CoreController {

    private final WebClient.Builder webClientBuilder;
    private final ExchangeStrategies exchangeStrategies;
    private final DereplicationController dereplicationController;
    private final ElucidationController elucidationController;

    @Autowired
    public CoreController(final WebClient.Builder webClientBuilder, final ExchangeStrategies exchangeStrategies,
                          final DereplicationController dereplicationController,
                          final ElucidationController elucidationController) {
        this.webClientBuilder = webClientBuilder;
        this.exchangeStrategies = exchangeStrategies;
        this.dereplicationController = dereplicationController;
        this.elucidationController = elucidationController;
    }

    @GetMapping(value = "/", produces = "application/json")
    public String root() {

        return "Welcome to the Sherlock backend services!"
                + "\n\n"
                + "status: "
                + "OK"
                + "\n"
                + "version: "
                + "1.1.2"
                + "\n"
                + "API documentation: "
                + "postman_link"
                + "\n"
                + "documentation: "
                + "https://docs.nmrxiv.org/sherlock"
                + "\n"
                + "github: "
                + "https://github.com/michaelwenk/sherlock"
                + "\n";
    }

    @PostMapping(value = "/core", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Transfer> core(@RequestBody final Transfer requestTransfer) {
        final Transfer responseTransfer = new Transfer();
        // set all input data to the output data
        // and overwrite certain parts after that
        responseTransfer.setQueryType(requestTransfer.getQueryType());
        responseTransfer.setDereplicationOptions(requestTransfer.getDereplicationOptions());
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
                responseTransfer.getResultRecord()
                                .setDataSetList(transferResponseEntity.getBody()
                                                                      .getDataSetList());
                responseTransfer.getResultRecord()
                                .setDataSetListSize(transferResponseEntity.getBody()
                                                                          .getDataSetList()
                                                                          .size());
                responseTransfer.getResultRecord()
                                .setQuerySpectrum(new SpectrumCompact(querySpectrum));

                return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
            }

            if (requestTransfer.getQueryType()
                               .equals("elucidation")) {
                // NEW UUID CREATION
                final String requestID = UUID.randomUUID()
                                             .toString();
                // PyLSD RUN
                final Transfer queryTransfer = new Transfer();
                queryTransfer.setQuerySpectrum(querySpectrum);
                queryTransfer.setCorrelations(requestTransfer.getResultRecord()
                                                             .getCorrelations());
                queryTransfer.setDetectionOptions(requestTransfer.getResultRecord()
                                                                 .getDetectionOptions());
                queryTransfer.setElucidationOptions(requestTransfer.getResultRecord()
                                                                   .getElucidationOptions());
                queryTransfer.setDetections(requestTransfer.getResultRecord()
                                                           .getDetections());
                queryTransfer.setDetected(requestTransfer.getResultRecord()
                                                         .getDetected());
                queryTransfer.setGrouping(requestTransfer.getResultRecord()
                                                         .getGrouping());
                queryTransfer.setRequestID(requestID);
                queryTransfer.setMf(mf);
                queryTransfer.setPredictionOptions(requestTransfer.getResultRecord()
                                                                  .getPredictionOptions());

                ResponseEntity<Transfer> transferResponseEntity = this.elucidationController.elucidate(queryTransfer);
                if (transferResponseEntity.getStatusCode()
                                          .isError()) {
                    System.out.println("ELUCIDATION request failed: "
                                               + transferResponseEntity.getBody()
                                                                       .getErrorMessage());

                    return transferResponseEntity;
                }
                final Transfer elucidationResultTransfer = transferResponseEntity.getBody();
                requestTransfer.getResultRecord()
                               .setDataSetList(elucidationResultTransfer.getDataSetList());
                requestTransfer.getResultRecord()
                               .setQuerySpectrum(new SpectrumCompact(querySpectrum));
                requestTransfer.getResultRecord()
                               .setCorrelations(correlations);
                requestTransfer.getResultRecord()
                               .setDetected(elucidationResultTransfer.getDetected());
                requestTransfer.getResultRecord()
                               .setDetections(elucidationResultTransfer.getDetections());
                requestTransfer.getResultRecord()
                               .setDetectionOptions(elucidationResultTransfer.getDetectionOptions());
                requestTransfer.getResultRecord()
                               .setGrouping(elucidationResultTransfer.getGrouping());
                requestTransfer.getResultRecord()
                               .setPredictionOptions(requestTransfer.getResultRecord()
                                                                    .getPredictionOptions());

                transferResponseEntity = this.rankAndStore(requestTransfer);
                if (transferResponseEntity.getStatusCode()
                                          .isError()) {
                    System.out.println("ELUCIDATION -> configuration and storage request failed: "
                                               + transferResponseEntity.getBody()
                                                                       .getErrorMessage());
                    responseTransfer.setErrorMessage("ELUCIDATION -> configuration and storage of result failed: "
                                                             + transferResponseEntity.getStatusCode());
                    responseTransfer.getResultRecord()
                                    .setQuerySpectrum(new SpectrumCompact(querySpectrum));

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
                queryTransfer.setQuerySpectrum(querySpectrum);
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
                                .setDetected(queryResultTransfer.getDetected());
                responseTransfer.getResultRecord()
                                .setDetections(queryResultTransfer.getDetections());
                responseTransfer.getResultRecord()
                                .setGrouping(queryResultTransfer.getGrouping());
                responseTransfer.getResultRecord()
                                .setElucidationOptions(queryResultTransfer.getElucidationOptions());
                responseTransfer.getResultRecord()
                                .setQuerySpectrum(new SpectrumCompact(querySpectrum));
                return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
            }
        } catch (final Exception e) {
            System.err.println("An error occurred: ");
            e.printStackTrace();

            responseTransfer.setErrorMessage(e.getMessage());
            responseTransfer.getResultRecord()
                            .setQuerySpectrum(new SpectrumCompact(querySpectrum));
            return new ResponseEntity<>(responseTransfer, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        responseTransfer.setDataSetList(new ArrayList<>());
        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
    }

    @GetMapping(value = "/cancel")
    public ResponseEntity<Transfer> cancel() {
        return PyLSD.cancel(false);
    }

    private ResponseEntity<Transfer> rankAndStore(final Transfer requestTransfer) {
        final ResultRecord resultRecord = requestTransfer.getResultRecord();
        final Transfer responseTransfer = new Transfer();
        // store results in DB if not empty and replace resultRecord in responseTransfer
        if (!resultRecord.getDataSetList()
                         .isEmpty()) {
            FilterAndRank.rank(resultRecord.getDataSetList());

            for (final DataSet dataSet : resultRecord.getDataSetList()) {
                dataSet.addMetaInfo("withStereo", "false");
            }

            if (resultRecord.getPredictionOptions()
                            .isPredictWithStereo()) {
                int limit = resultRecord.getPredictionOptions()
                                        .getStereoPredictionLimit();
                if (limit
                        > resultRecord.getDataSetList()
                                      .size()) {
                    limit = resultRecord.getDataSetList()
                                        .size();
                }
                System.out.println(" -> prediction with stereo for the first "
                                           + limit
                                           + " candidates");
                final List<DataSet> dataSetSubList = new ArrayList<>();
                for (int i = 0; i
                        < limit; i++) {
                    dataSetSubList.add(resultRecord.getDataSetList()
                                                   .get(i));
                }
                for (final DataSet dataSet : dataSetSubList) {
                    final String smiles = dataSet.getMeta()
                                                 .get("smiles");
                    System.out.println(" -> generated isomers and predict for \""
                                               + smiles
                                               + "\" ...");
                    requestTransfer.setSmiles(smiles);
                    requestTransfer.setQuerySpectrum(resultRecord.getQuerySpectrum()
                                                                 .toSpectrum());
                    requestTransfer.setPredictionOptions(resultRecord.getPredictionOptions());
                    final List<DataSet> stereoPredictionDataSetList = Utilities.getStereoPredictedDataSetFlux(
                                                                                       requestTransfer, this.webClientBuilder, this.exchangeStrategies)
                                                                               .collectList()
                                                                               .block();
                    if (stereoPredictionDataSetList
                            != null) {
                        for (final DataSet stereoPredictionDataSet : stereoPredictionDataSetList) {
                            stereoPredictionDataSet.addMetaInfo("withStereo", "true");
                        }
                        resultRecord.getDataSetList()
                                    .addAll(stereoPredictionDataSetList);
                        System.out.println(" --> added "
                                                   + stereoPredictionDataSetList.size()
                                                   + " candidates with stereo for \""
                                                   + dataSet.getMeta()
                                                            .get("smiles")
                                                   + "\"");
                        //                            // remove original dataset to avoid duplicates
                        //                            resultRecord.getDataSetList()
                        //                                        .remove(dataSet);
                    }
                }
            }
            FilterAndRank.rank(resultRecord.getDataSetList());

            //                Utilities.addMolFileToDataSets(resultRecord.getDataSetList());

            resultRecord.setDate(LocalDateTime.now(ZoneId.of("UTC"))
                                              .toString());
            final List<DataSet> cutDataSetList = new ArrayList<>();
            for (int i = 0; i
                    < 500; i++) {
                if (i
                        >= resultRecord.getDataSetList()
                                       .size()) {
                    break;
                }
                cutDataSetList.add(resultRecord.getDataSetList()
                                               .get(i));
            }
            resultRecord.setDataSetList(cutDataSetList);
            resultRecord.setDataSetListSize(cutDataSetList.size());
            resultRecord.setPreviewDataSet(cutDataSetList.get(0));

            final WebClient webClient = this.webClientBuilder.baseUrl(
                                                    "http://sherlock-gateway:8080/sherlock-db-service-result/insert")
                                                             .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                            MediaType.APPLICATION_JSON_VALUE)
                                                             .exchangeStrategies(this.exchangeStrategies)
                                                             .build();
            try {
                final ResponseEntity<ObjectId> resultStorageResponseEntity = webClient.post()
                                                                                      .bodyValue(resultRecord)
                                                                                      .retrieve()
                                                                                      .toEntity(ObjectId.class)
                                                                                      .block();
                if (resultStorageResponseEntity.getStatusCode()
                                               .isError()
                        || resultStorageResponseEntity.getBody()
                        == null) {
                    responseTransfer.setResultRecord(resultRecord);
                    responseTransfer.setErrorMessage("Result storage request failed: "
                                                             + resultStorageResponseEntity.getStatusCode());
                    return new ResponseEntity<>(responseTransfer, resultStorageResponseEntity.getStatusCode());
                }
                responseTransfer.setResultRecord(resultRecord);
                responseTransfer.getResultRecord()
                                .setId(resultStorageResponseEntity.getBody()
                                                                  .toString());
            } catch (final Exception e) {
                responseTransfer.setResultRecord(resultRecord);
                responseTransfer.setErrorMessage(e.getMessage());
                return new ResponseEntity<>(responseTransfer, HttpStatus.NOT_FOUND);
            }
        } else {
            responseTransfer.setResultRecord(resultRecord);

            responseTransfer.getResultRecord()
                            .setDataSetList(new ArrayList<>());
            responseTransfer.getResultRecord()
                            .setDataSetListSize(0);
            responseTransfer.getResultRecord()
                            .setPreviewDataSet(null);
            responseTransfer.getResultRecord()
                            .setId(null);
        }

        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
    }

    // @TODO preparation for using multipart/form-data

    //    ## multipart settings
    //    #spring.servlet.multipart.max-file-size=-1
    //    #spring.servlet.multipart.max-request-size=-1

    //    @PostMapping(value = "/core", consumes = "multipart/form-data", produces = "application/json")
    //    public ResponseEntity<Transfer> core(@RequestParam("data") final MultipartFile multipartFile) {
    //        final Transfer responseTransfer = new Transfer();
    //        final Transfer requestTransfer;
    //        try {
    //            requestTransfer = this.inputStreamToTransfer(multipartFile.getResource()
    //                                                                      .getInputStream());
    //        } catch (final IOException e) {
    //            e.printStackTrace();
    //            responseTransfer.setErrorMessage("Could not parse request data properly!!!");
    //            return new ResponseEntity<>(responseTransfer, HttpStatus.BAD_REQUEST);
    //        }
    //    }

    //    public <T> List<T> parseObjectList(final JsonReader jsonReader, final Class<T> clazz) throws IOException {
    //        final List<T> objectList = new ArrayList<>();
    //
    //        jsonReader.beginArray();
    //        while (jsonReader.hasNext()) {
    //            objectList.add(this.gson.fromJson(jsonReader, clazz));
    //        }
    //        jsonReader.endArray();
    //        return objectList;
    //    }
    //
    //    private ResultRecord parseResultRecord(final JsonReader jsonReader) throws IOException {
    //        final ResultRecord resultRecord = new ResultRecord();
    //        jsonReader.beginObject();
    //        while (jsonReader.hasNext()) {
    //            switch (jsonReader.nextName()) {
    //                case "id":
    //                    resultRecord.setId(jsonReader.nextString());
    //                    break;
    //                case "name":
    //                    resultRecord.setName(jsonReader.nextString());
    //                    break;
    //                case "date":
    //                    resultRecord.setDate(jsonReader.nextString());
    //                    break;
    //                case "dataSetList":
    //                    resultRecord.setDataSetList(this.parseObjectList(jsonReader, DataSet.class));
    //                    break;
    //                case "previewDataSet":
    //                    resultRecord.setPreviewDataSet(this.gson.fromJson(jsonReader, DataSet.class));
    //                    break;
    //                case "dataSetListSize":
    //                    resultRecord.setDataSetListSize(jsonReader.nextInt());
    //                    break;
    //                case "correlations":
    //                    resultRecord.setCorrelations(this.gson.fromJson(jsonReader, Correlations.class));
    //                    break;
    //                case "detections":
    //                    resultRecord.setDetections(this.gson.fromJson(jsonReader, Detections.class));
    //                    break;
    //                case "detectionOptions":
    //                    resultRecord.setDetectionOptions(this.gson.fromJson(jsonReader, DetectionOptions.class));
    //                    break;
    //                case "elucidationOptions":
    //                    resultRecord.setElucidationOptions(this.gson.fromJson(jsonReader, ElucidationOptions.class));
    //                    break;
    //                case "grouping":
    //                    resultRecord.setGrouping(this.gson.fromJson(jsonReader, Grouping.class));
    //                    break;
    //                case "querySpectrum":
    //                    resultRecord.setQuerySpectrum(this.gson.fromJson(jsonReader, SpectrumCompact.class));
    //                    break;
    //                //                case "nmriumDataJsonParts":
    //                //                    resultRecord.setNmriumDataJsonParts(this.parseObjectList(jsonReader, String.class));
    //                //                    System.out.println(resultRecord.getNmriumDataJsonParts()
    //                //                                                   .isEmpty()
    //                //                                       ? " -> no NMRium data parts"
    //                //                                       : " -> NMRium data part count: "
    //                //                                               + resultRecord.getNmriumDataJsonParts()
    //                //                                                             .size());
    //                //                    break;
    //                default:
    //                    jsonReader.skipValue();
    //                    break;
    //            }
    //        }
    //        jsonReader.endObject();
    //
    //        return resultRecord;
    //    }
    //
    //    private Transfer inputStreamToTransfer(final InputStream inputStream) {
    //
    //        try {
    //            final Transfer transfer = new Transfer();
    //            final JsonReader jsonReader = new JsonReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    //            jsonReader.beginObject();
    //            while (jsonReader.hasNext()) {
    //                switch (jsonReader.nextName()) {
    //                    case "queryType":
    //                        transfer.setQueryType(jsonReader.nextString());
    //                        break;
    //                    case "dereplicationOptions":
    //                        transfer.setDereplicationOptions(this.gson.fromJson(jsonReader, DereplicationOptions.class));
    //                        break;
    //                    case "resultRecord":
    //                        transfer.setResultRecord(this.parseResultRecord(jsonReader));
    //                        break;
    //                    default:
    //                        jsonReader.skipValue();
    //                        break;
    //                }
    //            }
    //            jsonReader.endObject();
    //            jsonReader.close();
    //
    //            return transfer;
    //
    //            //            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    //            //            byteArrayOutputStream.writeBytes(inputStream.readAllBytes());
    //            //            final String resultString = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
    //            //
    //            //            return this.gson.fromJson(resultString, Transfer.class);
    //        } catch (final IOException e) {
    //            e.printStackTrace();
    //        }
    //
    //        return null;
    //    }
}
