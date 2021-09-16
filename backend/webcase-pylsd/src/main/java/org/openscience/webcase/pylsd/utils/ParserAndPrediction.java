package org.openscience.webcase.pylsd.utils;

import casekit.io.FileSystem;
import casekit.nmr.model.DataSet;
import casekit.nmr.utils.Parser;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.openscience.webcase.pylsd.model.db.HOSECode;
import org.openscience.webcase.pylsd.model.exchange.Transfer;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    private void fillHOSECodeDBEntriesMap() {
        System.out.println("\nloading DB entries map...");
        this.hoseCodeDBEntriesMap = new HashMap<>();
        final String pathToHOSECodesFile = "/data/hosecode/hosecodes.json";
        final List<HOSECode> hoseCodeObjectList = this.gson.fromJson(FileSystem.getFileContent(pathToHOSECodesFile),
                                                                     new TypeToken<List<HOSECode>>() {
                                                                     }.getType());
        for (final HOSECode hoseCodeObject : hoseCodeObjectList) {
            this.hoseCodeDBEntriesMap.put(hoseCodeObject.getHOSECode(), hoseCodeObject.getValues());
        }
        System.out.println(" -> done: "
                                   + this.hoseCodeDBEntriesMap.size());
    }

    public ResponseEntity<Transfer> parseAndPredictFromSmilesFile(final Transfer requestTransfer) {
        final Transfer responseTransfer = new Transfer();
        try {
            requestTransfer.setSmilesList(Parser.smilesFileToList(requestTransfer.getPathToSmilesFile()));
        } catch (final FileNotFoundException e) {
            System.out.println("Could not parse SMILES file: "
                                       + requestTransfer.getPathToSmilesFile());
        }
        try {
            final List<DataSet> dataSetList = Prediction.predict(requestTransfer, this.hoseCodeDBEntriesMap,
                                                                 Objects.requireNonNull(
                                                                         this.getMultiplicitySectionsSettings()
                                                                             .block()));
            responseTransfer.setDataSetList(dataSetList);
        } catch (final Exception e) {
            responseTransfer.setErrorMessage(e.getMessage());
            return new ResponseEntity<>(responseTransfer, HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
    }

    private Mono<Map<String, int[]>> getMultiplicitySectionsSettings() {
        final WebClient webClient = this.webClientBuilder.baseUrl(
                "http://webcase-gateway:8080/webcase-db-service-dataset/getMultiplicitySectionsSettings")
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
