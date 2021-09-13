package org.openscience.webcase.pylsd.utils;

import casekit.nmr.lsd.Constants;
import casekit.nmr.model.nmrium.Correlation;
import casekit.nmr.utils.Utils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

public class ConnectivityDetection {

    public static Map<Integer, Map<String, Map<String, Set<Integer>>>> detectConnectivities(
            final WebClient.Builder webClientBuilder, final List<Correlation> correlationList, final int shiftTol,
            final double thresholdHybridizationCount, final double thresholdProtonsCount, final String mf) {
        final Map<Integer, Map<String, Map<String, Set<Integer>>>> detectedConnectivities = new HashMap<>();

        final WebClient webClient = webClientBuilder.baseUrl(
                "http://webcase-gateway:8080/webcase-db-service-statistics/connectivity/")
                                                    .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                   MediaType.APPLICATION_JSON_VALUE)
                                                    .build();
        UriComponentsBuilder uriComponentsBuilder;
        Map<String, Map<String, Map<Integer, Integer>>> detectedConnectivitiesTemp;
        Correlation correlation;
        String multiplicity;
        for (int i = 0; i
                < correlationList.size(); i++) {
            correlation = correlationList.get(i);
            multiplicity = Utils.getMultiplicityFromProtonsCount(correlation);
            if (!correlation.getAtomType()
                            .equals("H")
                    && multiplicity
                    != null) {
                for (final String hybridizationString : correlation.getHybridization()) {
                    uriComponentsBuilder = UriComponentsBuilder.newInstance();
                    uriComponentsBuilder.path("/detectConnectivities")
                                        .queryParam("nucleus", Constants.nucleiMap.get(correlation.getAtomType()))
                                        .queryParam("hybridization", hybridizationString)
                                        .queryParam("multiplicity", multiplicity)
                                        .queryParam("minShift", (int) correlation.getSignal()
                                                                                 .getDelta()
                                                - shiftTol)
                                        .queryParam("maxShift", (int) correlation.getSignal()
                                                                                 .getDelta()
                                                + shiftTol)
                                        .queryParam("thresholdHybridizationCount", thresholdHybridizationCount)
                                        .queryParam("thresholdProtonsCount", thresholdProtonsCount)
                                        .queryParam("mf", mf);
                    detectedConnectivitiesTemp = webClient.get()
                                                          .uri(uriComponentsBuilder.toUriString())
                                                          .retrieve()
                                                          .bodyToMono(
                                                                  new ParameterizedTypeReference<Map<String, Map<String, Map<Integer, Integer>>>>() {
                                                                  })
                                                          .block();
                    if (detectedConnectivitiesTemp
                            != null) {
                        detectedConnectivities.putIfAbsent(i, new HashMap<>());
                        for (final Map.Entry<String, Map<String, Map<Integer, Integer>>> entryPerNeighborAtomType : detectedConnectivitiesTemp.entrySet()) {
                            detectedConnectivities.get(i)
                                                  .putIfAbsent(entryPerNeighborAtomType.getKey(), new HashMap<>());
                            for (final Map.Entry<String, Map<Integer, Integer>> entryPerNeighborAtomTypeAndHybridization : entryPerNeighborAtomType.getValue()
                                                                                                                                                   .entrySet()) {
                                detectedConnectivities.get(i)
                                                      .get(entryPerNeighborAtomType.getKey())
                                                      .putIfAbsent(entryPerNeighborAtomTypeAndHybridization.getKey(),
                                                                   new HashSet<>());
                                detectedConnectivities.get(i)
                                                      .get(entryPerNeighborAtomType.getKey())
                                                      .get(entryPerNeighborAtomTypeAndHybridization.getKey())
                                                      .addAll(entryPerNeighborAtomTypeAndHybridization.getValue()
                                                                                                      .keySet());
                            }
                        }
                    }
                }
            }
        }

        return detectedConnectivities;
    }
}
