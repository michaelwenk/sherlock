package org.openscience.sherlock.core.utils.detection;

import casekit.nmr.utils.Utils;
import org.openscience.sherlock.core.model.db.HeavyAtomStatisticsRecord;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeavyAtomStatisticsDetection {

    public static Map<String, Integer> detect(final WebClient.Builder webClientBuilder, final String mf) {
        final Map<String, Integer> detectedHeavyAtomStatistics = new HashMap<>();

        final WebClient webClient = webClientBuilder.baseUrl(
                                                            "http://sherlock-gateway:8080/sherlock-db-service-statistics/heavyAtomStatistics/findByMf/")
                                                    .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                   MediaType.APPLICATION_JSON_VALUE)
                                                    .build();
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
        uriComponentsBuilder.queryParam("mf", mf);


        final List<HeavyAtomStatisticsRecord> heavyAtomStatisticsRecordList = webClient.get()
                                                                                       .uri(uriComponentsBuilder.toUriString())
                                                                                       .retrieve()
                                                                                       .bodyToFlux(
                                                                                               HeavyAtomStatisticsRecord.class)
                                                                                       .collectList()
                                                                                       .block();

        if (heavyAtomStatisticsRecordList
                != null) {
            for (final HeavyAtomStatisticsRecord heavyAtomStatisticsRecord : heavyAtomStatisticsRecordList) {
                detectedHeavyAtomStatistics.put(heavyAtomStatisticsRecord.getAtomPair(),
                                                heavyAtomStatisticsRecord.getCount());
            }
        }

        return detectedHeavyAtomStatistics;
    }

    public static boolean checkAllowanceOfHeteroAtom(final WebClient.Builder webClientBuilder, final String mf,
                                                     final double threshold) {
        final Map<String, Integer> elementCounts = Utils.getMolecularFormulaElementCounts(mf);
        final int sumHeteroAtomsByMf = elementCounts.entrySet()
                                                    .stream()
                                                    .filter(entry -> !entry.getKey()
                                                                           .equals("C")
                                                            && !entry.getKey()
                                                                     .equals("H"))
                                                    .map(Map.Entry::getValue)
                                                    .reduce(0, Integer::sum);
        if (sumHeteroAtomsByMf
                <= 1) {
            return false;
        }
        final Map<String, Integer> detectedHeavyAtomStatistics = detect(webClientBuilder, mf);
        int sumHeteroAtoms = 0;
        String[] split;
        for (final Map.Entry<String, Integer> entry : detectedHeavyAtomStatistics.entrySet()) {
            split = entry.getKey()
                         .split("_");
            if (!split[0].equals("C")
                    && !split[1].equals("C")) {
                sumHeteroAtoms += entry.getValue();
            }
        }
        final int sumAll = detectedHeavyAtomStatistics.values()
                                                      .stream()
                                                      .reduce(0, Integer::sum);
        return sumHeteroAtoms
                / (double) sumAll
                > threshold;
    }
}
