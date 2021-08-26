package org.openscience.webcase.pylsd.utils;

import casekit.nmr.lsd.Constants;
import casekit.nmr.model.nmrdisplayer.Correlation;
import casekit.nmr.model.nmrdisplayer.Data;
import casekit.nmr.utils.Utils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HybridizationDetection {

    public static Map<Integer, List<Integer>> getDetectedHybridizations(final WebClient.Builder webClientBuilder,
                                                                        final Data data, final float thrs,
                                                                        final int shiftTol) {
        final Map<Integer, List<Integer>> detectedHybridizations = new HashMap<>();

        final WebClient webClient = webClientBuilder.baseUrl(
                "http://webcase-gateway:8080/webcase-db-service-hybridization/")
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
            multiplicity = Utils.getMultiplicityFromProtonsCount(correlation);
            if (multiplicity
                    != null) {
                uriComponentsBuilder = UriComponentsBuilder.newInstance();
                uriComponentsBuilder.path("/detectHybridizations")
                                    .queryParam("nucleus", Constants.nucleiMap.get(correlation.getAtomType()))
                                    .queryParam("minShift", (int) correlation.getSignal()
                                                                             .getDelta()
                                            - shiftTol)
                                    .queryParam("maxShift", (int) correlation.getSignal()
                                                                             .getDelta()
                                            + shiftTol)
                                    .queryParam("multiplicity", multiplicity)
                                    .queryParam("thrs", thrs);
                hybridizations = webClient.get()
                                          .uri(uriComponentsBuilder.toUriString())
                                          .retrieve()
                                          .bodyToFlux(Integer.class)
                                          .collectList()
                                          .block();

                detectedHybridizations.put(i, hybridizations);
            }
        }

        return detectedHybridizations;
    }
}
