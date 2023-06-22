package org.openscience.sherlock.dbservice.statistics.utils;

import casekit.nmr.analysis.HOSECodeShiftStatistics;
import casekit.nmr.model.DataSet;
import casekit.nmr.utils.Statistics;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.sherlock.dbservice.statistics.service.HOSECodeServiceImplementation;
import org.openscience.sherlock.dbservice.statistics.service.model.DataSetRecord;
import org.openscience.sherlock.dbservice.statistics.service.model.HOSECodeRecord;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utilities {

    public static Flux<DataSetRecord> getAllDataSets(final WebClient.Builder webClientBuilder,
                                                     final ExchangeStrategies exchangeStrategies) {
        final WebClient webClient = webClientBuilder.baseUrl(
                                                            "http://sherlock-gateway:8080/sherlock-db-service-dataset/dataset/getAll")
                                                    .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                   MediaType.APPLICATION_JSON_VALUE)
                                                    .exchangeStrategies(exchangeStrategies)
                                                    .build();

        return webClient.get()
                        .retrieve()
                        .bodyToFlux(DataSetRecord.class);
    }

    public static Flux<DataSetRecord> getByDataSetSpectrumNuclei(final WebClient.Builder webClientBuilder,
                                                                 final ExchangeStrategies exchangeStrategies,
                                                                 final String[] nuclei) {
        final WebClient webClient = webClientBuilder.baseUrl(
                                                            "http://sherlock-gateway:8080/sherlock-db-service-dataset/dataset")
                                                    .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                   MediaType.APPLICATION_JSON_VALUE)
                                                    .exchangeStrategies(exchangeStrategies)
                                                    .build();
        // @TODO take the nuclei order into account when matching -> now it's just an exact array match
        final String nucleiString = Arrays.stream(nuclei)
                                          .reduce("", (concat, current) -> concat
                                                  + current);
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
        uriComponentsBuilder.path("/getByNuclei")
                            .queryParam("nuclei", nucleiString);

        return webClient.get()
                        .uri(uriComponentsBuilder.toUriString())
                        .retrieve()
                        .bodyToFlux(DataSetRecord.class);
    }

    public static boolean containsStereoConfiguration(final DataSet dataSet) {
        for (final int[][] bondProperties : dataSet.getStructure()
                                                   .getBondProperties()) {
            for (final int[] bondProperty : bondProperties) {
                if (bondProperty[4]
                        == IBond.Stereo.UP.ordinal()
                        || bondProperty[4]
                        == IBond.Stereo.UP_INVERTED.ordinal()
                        || bondProperty[4]
                        == IBond.Stereo.DOWN.ordinal()
                        || bondProperty[4]
                        == IBond.Stereo.DOWN_INVERTED.ordinal()) {
                    return true;
                }
            }
        }

        return false;
    }

    public static void removeStereoConfiguration(final DataSet dataSet) {
        for (final int[][] bondProperties : dataSet.getStructure()
                                                   .getBondProperties()) {
            for (final int[] bondProperty : bondProperties) {
                bondProperty[4] = 0;
            }
        }
    }

    public static void insertIntoHoseCodeRecord(final Map.Entry<String, Map<String, Double[]>> entryPerHOSECode,
                                                final HOSECodeRecord hoseCodeRecord) {
        String solvent, shiftString;
        for (final Map.Entry<String, Double[]> entryPerSolvent : entryPerHOSECode.getValue()
                                                                                 .entrySet()) {
            solvent = entryPerSolvent.getKey();
            hoseCodeRecord.getValues()
                          .putIfAbsent(solvent, new HashMap<>());
            for (final Double shift : entryPerSolvent.getValue()) {
                shiftString = String.valueOf(Statistics.roundDouble(shift, 1))
                                    .replaceAll("\\.", "_");
                hoseCodeRecord.getValues()
                              .get(solvent)
                              .putIfAbsent(shiftString, 0L);
                hoseCodeRecord.getValues()
                              .get(solvent)
                              .put(shiftString, hoseCodeRecord.getValues()
                                                              .get(solvent)
                                                              .get(shiftString)
                                      + 1);
            }
        }
    }

    public static void buildAndInsertHOSECodes(final List<DataSet> dataSetList, final int maxSphere,
                                               final HOSECodeServiceImplementation hoseCodeServiceImplementation) {
        //        final List<Boolean> containsStereo = new ArrayList<>();
        //        for (final DataSet dataSet : dataSetList) {
        //            containsStereo.add(Utilities.containsStereoConfiguration(dataSet));
        //        }
        //
        //        System.out.println(" --> building 3D HOSE codes with/without stereo configuration...");
        //        Map<String, Map<String, Double[]>> hoseCodeShiftStatistics = HOSECodeShiftStatistics.buildHOSECodeShiftStatistics(
        //                dataSetList, maxSphere, true, false);
        //        System.out.println(" --> building 3D HOSE codes with stereo configuration done -> "
        //                                   + hoseCodeShiftStatistics.size());
        //        System.out.println(" --> updating 3D HOSE codes in database...");
        //        insertOrUpdateHOSECodeRecord(hoseCodeShiftStatistics, hoseCodeServiceImplementation);
        //        System.out.println(" --> updating 3D HOSE codes in database done");

        //        // walk through every dataset with stereo information again but without considering stereo
        //        final List<DataSet> dataSetList2 = new ArrayList<>();
        //        DataSet dataSet;
        //        for (int i = 0; i
        //                < dataSetList.size(); i++) {
        //            if (containsStereo.get(i)) {
        //                dataSet = dataSetList.get(i)
        //                                     .buildClone();
        //                Utilities.removeStereoConfiguration(dataSet);
        //
        //                dataSetList2.add(dataSet);
        //            }
        //        }
        //
        //        System.out.println(" --> building 3D HOSE codes without stereo configuration in "
        //                                   + dataSetList2.size()
        //                                   + " cases...");
        //        hoseCodeShiftStatistics = HOSECodeShiftStatistics.buildHOSECodeShiftStatistics(dataSetList2, maxSphere, true,
        //                                                                                       false);
        //        System.out.println(" --> building 3D HOSE codes without stereo configuration done -> "
        //                                   + hoseCodeShiftStatistics.size());
        //        System.out.println(" --> updating 3D HOSE codes in database...");
        //        insertOrUpdateHOSECodeRecord(hoseCodeShiftStatistics, hoseCodeServiceImplementation);
        //        System.out.println(" --> updating 3D HOSE codes in database done");


        // for now:
        // walk through every dataset without considering stereo bond information
        // but benefit from the distinctions at double bonds
        for (final DataSet dataSet : dataSetList) {
            Utilities.removeStereoConfiguration(dataSet);
        }
        System.out.println(" --> building 3D HOSE codes without stereo configuration in "
                                   + dataSetList.size()
                                   + " cases...");
        final Map<String, Map<String, Double[]>> hoseCodeShiftStatistics = HOSECodeShiftStatistics.buildHOSECodeShiftStatistics(
                dataSetList, maxSphere, true, false);
        System.out.println(" --> building 3D HOSE codes without stereo configuration done -> "
                                   + hoseCodeShiftStatistics.size());
        System.out.println(" --> updating 3D HOSE codes in database...");

        insertOrUpdateHOSECodeRecord(hoseCodeShiftStatistics, hoseCodeServiceImplementation);
        System.out.println(" --> updating 3D HOSE codes in database done");
    }

    public static void insertOrUpdateHOSECodeRecord(
            final Map<String, Map<String, Double[]>> hoseCodeShiftStatisticsTemp,
            final HOSECodeServiceImplementation hoseCodeServiceImplementation) {
        for (final Map.Entry<String, Map<String, Double[]>> entryPerHOSECode : hoseCodeShiftStatisticsTemp.entrySet()) {
            final String hoseCode = entryPerHOSECode.getKey();
            if (!hoseCodeServiceImplementation.existsById(hoseCode)) {
                final HOSECodeRecord hoseCodeRecord = new HOSECodeRecord(hoseCode, new HashMap<>(), new HashMap<>());
                insertIntoHoseCodeRecord(entryPerHOSECode, hoseCodeRecord);
                hoseCodeServiceImplementation.insert(hoseCodeRecord);
            } else {
                final HOSECodeRecord hoseCodeRecord = hoseCodeServiceImplementation.findById(hoseCode)
                                                                                   .get();
                insertIntoHoseCodeRecord(entryPerHOSECode, hoseCodeRecord);
                hoseCodeServiceImplementation.save(hoseCodeRecord);
            }
        }
    }
}
