package org.openscience.sherlock.pylsd.utils.detection;

import casekit.nmr.lsd.Constants;
import casekit.nmr.model.Signal;
import casekit.nmr.model.nmrium.Correlation;
import casekit.nmr.utils.Utils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

public class ConnectivityDetection {

    public static Map<Integer, Map<String, Map<Integer, Set<Integer>>>> detectConnectivities(
            final WebClient.Builder webClientBuilder, final List<Correlation> correlationList, final int shiftTol,
            final double elementCountThreshold, final String mf, final boolean onAtomTypeLevel) {
        final Map<Integer, Map<String, Map<Integer, Set<Integer>>>> detectedConnectivities = new HashMap<>();
        final Set<Integer> knownCarbonHybridizations = new HashSet<>();
        for (final Correlation correlation : correlationList) {
            // @TODO constraints for carbon only for now
            if (correlation.getAtomType()
                           .equals("C")) {
                knownCarbonHybridizations.addAll(correlation.getHybridization());
            }
        }

        final WebClient webClient = webClientBuilder.baseUrl(
                                                            "http://sherlock-gateway:8080/sherlock-db-service-statistics/connectivity/")
                                                    .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                   MediaType.APPLICATION_JSON_VALUE)
                                                    .build();
        UriComponentsBuilder uriComponentsBuilder;
        Map<String, Map<Integer, Set<Integer>>> detectedConnectivitiesTemp;
        Correlation correlation;
        String multiplicity;
        Signal signal;
        for (int i = 0; i
                < correlationList.size(); i++) {
            correlation = correlationList.get(i);
            multiplicity = Utils.getMultiplicityFromProtonsCount(correlation);
            signal = Utils.extractSignalFromCorrelation(correlation);
            if (!correlation.getAtomType()
                            .equals("H")
                    && multiplicity
                    != null
                    && signal
                    != null
                    && !correlation.getHybridization()
                                   .isEmpty()) {
                uriComponentsBuilder = UriComponentsBuilder.newInstance();
                uriComponentsBuilder.path("/detectConnectivities")
                                    .queryParam("nucleus", Constants.nucleiMap.get(correlation.getAtomType()))
                                    .queryParam("hybridizations", correlation.getHybridization()
                                                                             .stream()
                                                                             .map(String::valueOf)
                                                                             .collect(Collectors.joining(",")))
                                    .queryParam("multiplicity", multiplicity)
                                    .queryParam("minShift", signal.getShift(0)
                                                                  .intValue()
                                            - shiftTol)
                                    .queryParam("maxShift", signal.getShift(0)
                                                                  .intValue()
                                            + shiftTol)
                                    .queryParam("elementCountThreshold", elementCountThreshold)
                                    .queryParam("mf", mf)
                                    .queryParam("onAtomTypeLevel", onAtomTypeLevel)
                                    .queryParam("knownCarbonHybridizations", knownCarbonHybridizations);
                detectedConnectivitiesTemp = webClient.get()
                                                      .uri(uriComponentsBuilder.toUriString())
                                                      .retrieve()
                                                      .bodyToMono(
                                                              new ParameterizedTypeReference<Map<String, Map<Integer, Set<Integer>>>>() {
                                                              })
                                                      .block();
                detectedConnectivities.put(i, detectedConnectivitiesTemp);
            }
        }

        return detectedConnectivities;
    }
}
