package org.openscience.sherlock.dbservice.statistics.utils;

import casekit.nmr.lsd.Constants;
import org.openscience.sherlock.dbservice.statistics.service.model.DataSetRecord;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Utilities {

    public static Flux<DataSetRecord> getByDataSetSpectrumNuclei(final WebClient.Builder webClientBuilder,
                                                                 final ExchangeStrategies exchangeStrategies,
                                                                 final String[] nuclei) {
        final WebClient webClient = webClientBuilder.baseUrl("http://sherlock-gateway:8080/sherlock-db-service-dataset")
                                                    .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                   MediaType.APPLICATION_JSON_VALUE)
                                                    .exchangeStrategies(exchangeStrategies)
                                                    .build();
        // @TODO take the nuclei order into account when matching -> now it's just an exact array match
        final String nucleiString = Arrays.stream(nuclei)
                                          .reduce("", (concat, current) -> concat
                                                  + current);
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
        uriComponentsBuilder.path("/getByNuclei")
                            .queryParam("nuclei", nucleiString);

        return webClient.get()
                        .uri(uriComponentsBuilder.toUriString())
                        .retrieve()
                        .bodyToFlux(DataSetRecord.class);
    }

    public static Map<String, Map<Integer, Map<Integer, Integer>>> convertToNumericHybridizationMapKeys(
            final Map<String, Map<String, Map<Integer, Integer>>> map) {
        final Map<String, Map<Integer, Map<Integer, Integer>>> converted = new HashMap<>();
        int numericHybridization;
        for (final Map.Entry<String, Map<String, Map<Integer, Integer>>> entryPerAtomType : map.entrySet()) {
            converted.put(entryPerAtomType.getKey(), new HashMap<>());
            for (final Map.Entry<String, Map<Integer, Integer>> entryPerHybridizationString : entryPerAtomType.getValue()
                                                                                                              .entrySet()) {
                if (Constants.hybridizationConversionMap.containsKey(entryPerHybridizationString.getKey())) {
                    numericHybridization = Constants.hybridizationConversionMap.get(
                            entryPerHybridizationString.getKey());
                    converted.get(entryPerAtomType.getKey())
                             .putIfAbsent(numericHybridization, entryPerHybridizationString.getValue());
                }
            }
        }

        return converted;
    }
}
