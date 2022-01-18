package org.openscience.sherlock.pylsd.utils.detection;

import casekit.nmr.analysis.ConnectivityStatistics;
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

    public static Map<Integer, Map<String, Map<Integer, Set<Integer>>>> detectByConnectivityCounts(
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
        Map<String, Map<Integer, Map<Integer, Integer>>> detectedConnectivitiesTemp;
        Map<String, Map<Integer, Set<Integer>>> detectedConnectivitiesTempFiltered;
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
                uriComponentsBuilder.path("/detectConnectivityCounts")
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
                                    .queryParam("mf", mf);
                detectedConnectivitiesTemp = webClient.get()
                                                      .uri(uriComponentsBuilder.toUriString())
                                                      .retrieve()
                                                      .bodyToMono(
                                                              new ParameterizedTypeReference<Map<String, Map<Integer, Map<Integer, Integer>>>>() {
                                                              })
                                                      .block();


                if (detectedConnectivitiesTemp
                        != null) {
                    ConnectivityStatistics.filterExtractedConnectivitiesByHybridizations(detectedConnectivitiesTemp,
                                                                                         knownCarbonHybridizations);
                    detectedConnectivitiesTempFiltered = ConnectivityStatistics.filterExtractedConnectivitiesByCount(
                            detectedConnectivitiesTemp, elementCountThreshold, onAtomTypeLevel);
                    detectedConnectivities.put(i, detectedConnectivitiesTempFiltered);
                }
            }
        }

        return detectedConnectivities;
    }

    public static Map<Integer, Map<String, Map<Integer, Set<Integer>>>> detectByOccurrenceCounts(
            final WebClient.Builder webClientBuilder, final List<Correlation> correlationList, final int shiftTol,
            final double elementCountThreshold, final String mf, final String mode) {
        final Map<Integer, Map<String, Map<Integer, Set<Integer>>>> detectedOccurrences = new HashMap<>();
        final WebClient webClient = webClientBuilder.baseUrl(
                                                            "http://sherlock-gateway:8080/sherlock-db-service-statistics/connectivity/")
                                                    .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                   MediaType.APPLICATION_JSON_VALUE)
                                                    .build();
        UriComponentsBuilder uriComponentsBuilder;
        Map<String, Integer[]> detectedOccurrencesTemp;
        Map<String, Map<Integer, Set<Integer>>> transformed;
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
                uriComponentsBuilder.path("/detectOccurrenceCounts")
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
                                    .queryParam("mf", mf);
                detectedOccurrencesTemp = webClient.get()
                                                   .uri(uriComponentsBuilder.toUriString())
                                                   .retrieve()
                                                   .bodyToMono(
                                                           new ParameterizedTypeReference<Map<String, Integer[]>>() {
                                                           })
                                                   .block();


                if (detectedOccurrencesTemp
                        != null) {
                    transformed = new HashMap<>();
                    for (final String neighborAtomType : detectedOccurrencesTemp.keySet()) {
                        final int totalCount = Arrays.stream(detectedOccurrencesTemp.get(neighborAtomType))
                                                     .reduce(0, Integer::sum);
                        if (mode.equals("upperLimit")
                                && detectedOccurrencesTemp.get(neighborAtomType)[0]
                                / (double) totalCount
                                >= elementCountThreshold) {
                            transformed.put(neighborAtomType, new HashMap<>());
                        } else if (mode.equals("lowerLimit")
                                && detectedOccurrencesTemp.get(neighborAtomType)[0]
                                / (double) totalCount
                                < elementCountThreshold) {
                            transformed.put(neighborAtomType, new HashMap<>());
                        }
                    }
                    detectedOccurrences.put(i, transformed);
                }
            }
        }

        return detectedOccurrences;
    }
}
