package org.openscience.sherlock.core.utils;

import casekit.nmr.model.DataSet;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.io.MDLV3000Writer;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

public class Utilities {

    public static Mono<Map<String, int[]>> getMultiplicitySectionsSettings(final WebClient.Builder webClientBuilder,
                                                                           final ExchangeStrategies exchangeStrategies) {
        final WebClient webClient = webClientBuilder.baseUrl(
                                                            "http://sherlock-gateway:8080/sherlock-db-service-dataset/getMultiplicitySectionsSettings")
                                                    .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                   MediaType.APPLICATION_JSON_VALUE)
                                                    .exchangeStrategies(exchangeStrategies)
                                                    .build();

        return webClient.get()
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<>() {
                        });
    }

    public static List<DataSet> addMolFileToDataSets(final List<DataSet> dataSetList) throws CDKException {
        for (final DataSet dataSet : dataSetList) {
            // store as MOL file
            final MDLV3000Writer mdlv3000Writer = new MDLV3000Writer();
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            mdlv3000Writer.setWriter(byteArrayOutputStream);
            mdlv3000Writer.write(dataSet.getStructure()
                                        .toAtomContainer());
            dataSet.addMetaInfo("molfile", byteArrayOutputStream.toString());
        }

        return dataSetList;
    }
}
