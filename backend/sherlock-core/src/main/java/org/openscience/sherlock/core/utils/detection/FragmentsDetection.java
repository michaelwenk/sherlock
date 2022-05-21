package org.openscience.sherlock.core.utils.detection;

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
import java.util.List;
import java.util.stream.Collectors;

public class FragmentsDetection {

    public static List<DataSet> detect(final WebClient.Builder webClientBuilder, final Spectrum querySpectrum,
                                       final List<Correlation> correlationsList, final String mf) {
        final List<List<Integer>> hybridizationList = new ArrayList<>();
        for (final Correlation correlation : correlationsList) {
            hybridizationList.add(correlation.getHybridization());
        }
        final WebClient webClient = webClientBuilder.baseUrl(
                                                            "http://sherlock-gateway:8080/sherlock-db-service-dataset/fragment/getBySpectrumAndMfAndSetBits")
                                                    .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                   MediaType.APPLICATION_STREAM_JSON_VALUE)
                                                    .build();

        final Transfer queryTransfer = new Transfer();
        queryTransfer.setQuerySpectrum(querySpectrum);
        queryTransfer.setMf(mf);
        queryTransfer.setHybridizationList(hybridizationList);
        queryTransfer.setShiftTol(2.0);
        queryTransfer.setMaximumAverageDeviation(1.0);
        queryTransfer.setCheckMultiplicity(true);

        final List<DataSet> fragmentList = webClient.post()
                                                    .bodyValue(queryTransfer)
                                                    .retrieve()
                                                    .bodyToFlux(DataSet.class)
                                                    .collectList()
                                                    .block();
        if (fragmentList
                == null) {
            return new ArrayList<>();
        }

        return fragmentList.stream()
                           .map(dataSet -> {
                               try {
                                   Utilities.addMolFileToDataSet(dataSet);
                               } catch (final CDKException e) {
                                   e.printStackTrace();
                               }
                               dataSet.addAttachment("include", false);

                               return dataSet;
                           })
                           .collect(Collectors.toList());
    }
}
