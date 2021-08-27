package org.openscience.webcase.dbservice.statistics.utils;

import org.openscience.webcase.dbservice.statistics.service.model.DataSetRecord;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.util.Arrays;

public class Utilities {

    // set ExchangeSettings
    final static int maxInMemorySizeMB = 1000;
    final static ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                                                                           .codecs(configurer -> configurer.defaultCodecs()
                                                                                                           .maxInMemorySize(
                                                                                                                   maxInMemorySizeMB
                                                                                                                           * 1024

                                                                                                                           * 1024))
                                                                           .build();

    public static Flux<DataSetRecord> getByDataSetSpectrumNuclei(final WebClient.Builder webClientBuilder,
                                                                 final String[] nuclei) {
        final WebClient webClient = webClientBuilder.baseUrl("http://webcase-gateway:8080/webcase-db-service-dataset")
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
}
