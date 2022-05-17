package org.openscience.sherlock.core.utils.detection;

import casekit.nmr.fragments.FragmentUtilities;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import casekit.nmr.model.nmrium.Correlation;
import org.openscience.cdk.exception.CDKException;
import org.openscience.sherlock.core.model.exchange.Transfer;
import org.openscience.sherlock.core.utils.Utilities;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FunctionalGroupDetection {

    public static List<DataSet> detect(final WebClient.Builder webClientBuilder, final Spectrum querySpectrum,
                                       final List<Correlation> correlationsList, final String mf) {
        final List<List<Integer>> hybridizationList = new ArrayList<>();
        for (final Correlation correlation : correlationsList) {
            hybridizationList.add(correlation.getHybridization());
        }
        final WebClient webClient = webClientBuilder.baseUrl(
                                                            "http://sherlock-gateway:8080/sherlock-db-service-dataset/functionalGroup/getBySpectrumAndMfAndSetBits")
                                                    .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                   MediaType.APPLICATION_STREAM_JSON_VALUE)
                                                    .build();

        final Transfer queryTransfer = new Transfer();
        queryTransfer.setQuerySpectrum(querySpectrum);
        queryTransfer.setMf(mf);
        queryTransfer.setHybridizationList(hybridizationList);
        queryTransfer.setShiftTol(1.0);
        queryTransfer.setMaximumAverageDeviation(0.5);
        queryTransfer.setCheckMultiplicity(true);

        final List<DataSet> functionalGroupList = webClient.post()
                                                           .bodyValue(queryTransfer)
                                                           .retrieve()
                                                           .bodyToFlux(DataSet.class)
                                                           .collectList()
                                                           .block();
        if (functionalGroupList
                == null) {
            return new ArrayList<>();
        }
        final Map<String, List<DataSet>> smilesCollection = FragmentUtilities.collectBySmiles(functionalGroupList);

        final LinkedHashMap<String, List<DataSet>> sortByFrequencies = FragmentUtilities.sortByFrequencies(
                smilesCollection);

        long total = 0;
        for (final Map.Entry<String, List<DataSet>> entry : sortByFrequencies.entrySet()) {
            total += entry.getValue()
                          .size();
        }

        final long finalTotal = total;
        return sortByFrequencies.entrySet()
                                .stream()
                                .map(entry -> {
                                    final DataSet dataSet = new DataSet();
                                    dataSet.setStructure(entry.getValue()
                                                              .get(0)
                                                              .getStructure());
                                    try {
                                        Utilities.addMolFileToDataSet(dataSet);
                                    } catch (final CDKException e) {
                                        e.printStackTrace();
                                    }
                                    final long count = entry.getValue()
                                                            .size();
                                    dataSet.addAttachment("count", count);
                                    dataSet.addAttachment("total", finalTotal);
                                    dataSet.addAttachment("include", false);

                                    return dataSet;
                                })
                                .collect(Collectors.toList());
    }
}
