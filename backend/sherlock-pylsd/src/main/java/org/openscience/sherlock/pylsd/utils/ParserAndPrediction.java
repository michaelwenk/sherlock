package org.openscience.sherlock.pylsd.utils;

import casekit.io.FileSystem;
import casekit.nmr.model.DataSet;
import casekit.nmr.utils.Parser;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.openscience.sherlock.pylsd.model.db.HOSECode;
import org.openscience.sherlock.pylsd.model.exchange.Transfer;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.FileNotFoundException;
import java.util.*;

public class ParserAndPrediction {

    final Gson gson = new GsonBuilder().setLenient()
                                       .create();

    private final WebClient.Builder webClientBuilder;
    private final ExchangeStrategies exchangeStrategies;
    private Map<String, Map<String, Double[]>> hoseCodeDBEntriesMap;

    public ParserAndPrediction(final WebClient.Builder webClientBuilder, final ExchangeStrategies exchangeStrategies) {
        this.webClientBuilder = webClientBuilder;
        this.exchangeStrategies = exchangeStrategies;

        this.fillHOSECodeDBEntriesMap();
    }

    public String loopMethod(final String pathToHOSECodesFile, final int waitingDuration,
                             final int totalWaitingDuration, int currentWaitingDuration) {
        final String fileContent = FileSystem.getFileContent(pathToHOSECodesFile);
        if (fileContent
                == null) {
            try {
                System.out.println(" -> could not read HOSE codes from file: \""
                                           + pathToHOSECodesFile
                                           + "\" -> trying again in "
                                           + waitingDuration
                                           + " ms");
                Thread.sleep(waitingDuration);
                currentWaitingDuration += waitingDuration;
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
            if (currentWaitingDuration
                    < totalWaitingDuration) {
                return this.loopMethod(pathToHOSECodesFile, waitingDuration, totalWaitingDuration,
                                       currentWaitingDuration);
            }
        }

        return fileContent;
    }

    private void fillHOSECodeDBEntriesMap() {
        System.out.println("\nloading DB entries map...");
        this.hoseCodeDBEntriesMap = new HashMap<>();
        final String pathToHOSECodesFile = "/data/hosecode/hosecodes.json";
        final int waitingDuration = 30; // seconds
        final int totalWaitingDuration = 300; // seconds
        final String fileContent = this.loopMethod(pathToHOSECodesFile, waitingDuration
                * 1000, totalWaitingDuration
                                                           * 1000, 0);
        if (fileContent
                != null) {
            final List<HOSECode> hoseCodeObjectList = this.gson.fromJson(fileContent, new TypeToken<List<HOSECode>>() {
            }.getType());
            for (final HOSECode hoseCodeObject : hoseCodeObjectList) {
                this.hoseCodeDBEntriesMap.put(hoseCodeObject.getHOSECode(), hoseCodeObject.getValues());
            }
            System.out.println(" -> done: "
                                       + this.hoseCodeDBEntriesMap.size());
        } else {
            System.out.println(" -> could not read HOSE codes from file: \""
                                       + pathToHOSECodesFile
                                       + "\" !!!");
        }
    }

    public ResponseEntity<Transfer> parseAndPredictFromSmilesFile(final Transfer requestTransfer) {
        final Transfer responseTransfer = new Transfer();
        try {
            requestTransfer.setSmilesList(Parser.smilesFileToList(requestTransfer.getPathToSmilesFile()));
            try {
                final List<DataSet> dataSetList = Prediction.predict(requestTransfer, this.hoseCodeDBEntriesMap,
                                                                     Objects.requireNonNull(
                                                                             this.getMultiplicitySectionsSettings()
                                                                                 .block()));
                responseTransfer.setDataSetList(dataSetList);
            } catch (final Exception e) {
                responseTransfer.setErrorMessage(e.getMessage());
                return new ResponseEntity<>(responseTransfer, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (final FileNotFoundException e) {
            System.out.println("Could not parse SMILES file: "
                                       + requestTransfer.getPathToSmilesFile());
            responseTransfer.setDataSetList(new ArrayList<>());
        }

        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
    }

    private Mono<Map<String, int[]>> getMultiplicitySectionsSettings() {
        final WebClient webClient = this.webClientBuilder.baseUrl(
                                                "http://sherlock-gateway:8080/sherlock-db-service-dataset/getMultiplicitySectionsSettings")
                                                         .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                        MediaType.APPLICATION_JSON_VALUE)
                                                         .exchangeStrategies(this.exchangeStrategies)
                                                         .build();
        return webClient.get()
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<>() {
                        });
    }
}
