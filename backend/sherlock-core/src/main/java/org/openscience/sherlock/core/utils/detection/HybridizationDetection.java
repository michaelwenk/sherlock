package org.openscience.sherlock.core.utils.detection;

import casekit.nmr.elucidation.Constants;
import casekit.nmr.model.Signal;
import casekit.nmr.model.nmrium.Correlation;
import casekit.nmr.utils.Utils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

public class HybridizationDetection {

    public static Map<Integer, List<Integer>> detectHybridizations(final WebClient.Builder webClientBuilder,
                                                                   final List<Correlation> correlationList,
                                                                   final String mf, final float threshold,
                                                                   final int shiftTol) {
        final Map<Integer, List<Integer>> detectedHybridizations = new HashMap<>();

        final WebClient webClient = webClientBuilder.baseUrl(
                                                            "http://sherlock-gateway:8080/sherlock-db-service-statistics/hybridization/")
                                                    .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                   MediaType.APPLICATION_JSON_VALUE)
                                                    .build();
        UriComponentsBuilder uriComponentsBuilder;
        List<Integer> hybridizations;
        Correlation correlation;
        String multiplicity;
        Signal signal;
        for (int i = 0; i
                < correlationList.size(); i++) {
            correlation = correlationList.get(i);
            multiplicity = Utils.getMultiplicityFromProtonsCount(correlation);
            signal = Utils.extractFirstSignalFromCorrelation(correlation);
            if (!correlation.getAtomType()
                            .equals("H")
                    && Constants.nucleiMap.containsKey(correlation.getAtomType())
                    && multiplicity
                    != null
                    && signal
                    != null) {
                uriComponentsBuilder = UriComponentsBuilder.newInstance();
                uriComponentsBuilder.path("/detectHybridizations")
                                    .queryParam("nucleus", Constants.nucleiMap.get(correlation.getAtomType()))
                                    .queryParam("multiplicity", multiplicity)
                                    .queryParam("minShift", signal.getShift(0)
                                                                  .intValue()
                                            - shiftTol)
                                    .queryParam("maxShift", signal.getShift(0)
                                                                  .intValue()
                                            + shiftTol)
                                    .queryParam("threshold", threshold)
                                    .queryParam("mf", mf);
                hybridizations = webClient.get()
                                          .uri(uriComponentsBuilder.toUriString())
                                          .retrieve()
                                          .bodyToFlux(Integer.class)
                                          .collectList()
                                          .block();

                detectedHybridizations.put(i, hybridizations
                                                      != null
                                              ? new ArrayList<>(new HashSet<>(hybridizations))
                                              : new ArrayList<>());
            }
        }

        return detectedHybridizations;
    }
}
