package org.openscience.sherlock.core.utils;

import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import casekit.nmr.utils.Utils;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.io.MDLV3000Writer;
import org.openscience.sherlock.core.model.db.DataSetRecord;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Utilities {

    public static Mono<Map<String, int[]>> getMultiplicitySectionsSettings(final WebClient.Builder webClientBuilder,
                                                                           final ExchangeStrategies exchangeStrategies) {
        final WebClient webClient = webClientBuilder.baseUrl(
                                                            "http://sherlock-gateway:8080/sherlock-db-service-dataset/dataset/getMultiplicitySectionsSettings")
                                                    .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                   MediaType.APPLICATION_JSON_VALUE)
                                                    .exchangeStrategies(exchangeStrategies)
                                                    .build();

        return webClient.get()
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<>() {
                        });
    }

    public static Flux<DataSetRecord> getDataSetRecordFlux(final WebClient.Builder webClientBuilder,
                                                           final ExchangeStrategies exchangeStrategies,
                                                           final Spectrum querySpectrum, final String mf) {
        final WebClient webClient = webClientBuilder.baseUrl(
                                                            "http://sherlock-gateway:8080/sherlock-db-service-dataset/dataset")
                                                    .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                   MediaType.APPLICATION_JSON_VALUE)
                                                    .exchangeStrategies(exchangeStrategies)
                                                    .build();
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();

        // @TODO take the nuclei order into account when matching -> now it's just an exact array match
        final String nucleiString = Arrays.stream(querySpectrum.getNuclei())
                                          .reduce("", (concat, current) -> concat
                                                  + current);
        if (mf
                != null) {
            uriComponentsBuilder.path("/getByNucleiAndSignalCountAndMf")
                                .queryParam("nuclei", nucleiString)
                                .queryParam("signalCount", querySpectrum.getSignalCount())
                                .queryParam("mf", Utils.getAlphabeticMF(mf));
        } else {
            uriComponentsBuilder.path("/getByNucleiAndSignalCount")
                                .queryParam("nuclei", nucleiString)
                                .queryParam("signalCount", querySpectrum.getSignalCount());
        }

        return webClient.get()
                        .uri(uriComponentsBuilder.toUriString())
                        .retrieve()
                        .bodyToFlux(DataSetRecord.class);
    }

    public static DataSet addMolFileToDataSet(final DataSet dataSet) throws CDKException {
        // store as MOL file
        final MDLV3000Writer mdlv3000Writer = new MDLV3000Writer();
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        mdlv3000Writer.setWriter(byteArrayOutputStream);
        mdlv3000Writer.write(dataSet.getStructure()
                                    .toAtomContainer());
        dataSet.addMetaInfo("molfile", byteArrayOutputStream.toString());

        return dataSet;
    }

    public static List<DataSet> addMolFileToDataSets(final List<DataSet> dataSetList) throws CDKException {
        for (final DataSet dataSet : dataSetList) {
            addMolFileToDataSet(dataSet);
        }

        return dataSetList;
    }
}
