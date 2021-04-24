package org.openscience.webcase.pylsd.utils;

import org.openscience.webcase.pylsd.model.nmrdisplayer.Correlation;
import org.openscience.webcase.pylsd.model.nmrdisplayer.Data;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HybridizationDetection {

    public static Map<Integer, List<Integer>> getDetectedHybridizations(final WebClient.Builder webClientBuilder,
                                                                        final Data data, final float thrs) {
        final Map<Integer, List<Integer>> detectedHybridizations = new HashMap<>();

        final WebClient webClient = webClientBuilder.
                                                            baseUrl("http://webcase-gateway:8081/webcase-db-service-hybridization/nmrshiftdb")
                                                    .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                   MediaType.APPLICATION_JSON_VALUE)
                                                    .build();

        UriComponentsBuilder uriComponentsBuilder;
        List<Integer> hybridizations;
        Correlation correlation;
        String multiplicity;
        for (int i = 0; i
                < data.getCorrelations()
                      .getValues()
                      .size(); i++) {
            correlation = data.getCorrelations()
                              .getValues()
                              .get(i);
            multiplicity = HybridizationDetection.getMultiplicityFromProtonsCount(correlation);
            if (multiplicity
                    != null) {
                uriComponentsBuilder = UriComponentsBuilder.newInstance();
                uriComponentsBuilder.path("/detectHybridizations")
                                    .queryParam("nucleus", Constants.nucleiMap.get(correlation.getAtomType()))
                                    .queryParam("minShift", (int) correlation.getSignal()
                                                                             .getDelta()
                                            - 2)
                                    .queryParam("maxShift", (int) correlation.getSignal()
                                                                             .getDelta()
                                            + 2)
                                    .queryParam("multiplicity", multiplicity)
                                    .queryParam("thrs", thrs);
                hybridizations = webClient //final Flux<DataSet> results = webClient
                                           .get()
                                           .uri(uriComponentsBuilder.toUriString())
                                           .retrieve()
                                           .bodyToMono(new ParameterizedTypeReference<List<Integer>>() {
                                           })
                                           .block();

                detectedHybridizations.put(i, hybridizations);
            }
        }

        return detectedHybridizations;
    }

    private static String getMultiplicityFromProtonsCount(final Correlation correlation) {
        if (correlation.getAtomType()
                       .equals("C")
                && correlation.getProtonsCount()
                              .size()
                == 1) {
            switch (correlation.getProtonsCount()
                               .get(0)) {
                case 0:
                    return "s";
                case 1:
                    return "d";
                case 2:
                    return "t";
                case 3:
                    return "q";
                default:
                    return null;
            }
        }
        return null;
    }
}
