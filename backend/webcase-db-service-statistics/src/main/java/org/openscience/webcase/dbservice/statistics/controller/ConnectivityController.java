package org.openscience.webcase.dbservice.statistics.controller;

import casekit.nmr.analysis.ConnectivityStatistics;
import casekit.nmr.utils.Utils;
import org.openscience.webcase.dbservice.statistics.service.ConnectivityServiceImplementation;
import org.openscience.webcase.dbservice.statistics.service.model.ConnectivityRecord;
import org.openscience.webcase.dbservice.statistics.service.model.DataSetRecord;
import org.openscience.webcase.dbservice.statistics.utils.Utilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping(value = "/connectivity")
public class ConnectivityController {

    private final WebClient.Builder webClientBuilder;
    private final ConnectivityServiceImplementation connectivityServiceImplementation;

    @Autowired
    public ConnectivityController(final WebClient.Builder webClientBuilder,
                                  final ConnectivityServiceImplementation connectivityServiceImplementation) {
        this.webClientBuilder = webClientBuilder;
        this.connectivityServiceImplementation = connectivityServiceImplementation;
    }

    @GetMapping(value = "/count", produces = "application/json")
    public Mono<Long> getCount() {
        return this.connectivityServiceImplementation.count();
    }

    @GetMapping(value = "/getAll", produces = "application/stream+json")
    public Flux<ConnectivityRecord> getAll() {
        return this.connectivityServiceImplementation.findAll();
    }

    @GetMapping(value = "/getConnectivityCounts", produces = "application/stream+json")
    public Flux<ConnectivityRecord> findByNucleusAndHybridizationAndMultiplicityAndShift(
            @RequestParam final String nucleus, @RequestParam final String hybridization,
            @RequestParam final String multiplicity, @RequestParam final int minShift,
            @RequestParam final int maxShift) {
        return this.connectivityServiceImplementation.findByNucleusAndHybridizationAndMultiplicityAndShift(nucleus,
                                                                                                           hybridization,
                                                                                                           multiplicity,
                                                                                                           minShift,
                                                                                                           maxShift);
    }

    @PostMapping(value = "/deleteAll")
    public Mono<Void> deleteAll() {
        return this.connectivityServiceImplementation.deleteAll();
    }

    @PostMapping(value = "/replaceAll")
    public void replaceAll(@RequestParam final String[] nuclei) {
        this.deleteAll()
            .block();

        // nucleus -> multiplicity -> hybridization -> shift (int) -> connected atom symbol -> connected atom hybridization -> connected atom protons count -> occurrence
        final Map<String, Map<String, Map<String, Map<Integer, Map<String, Map<String, Map<Integer, Integer>>>>>>> connectivityStatisticsPerNucleus = new ConcurrentHashMap<>();
        Utilities.getByDataSetSpectrumNuclei(this.webClientBuilder, nuclei)
                 .map(DataSetRecord::getDataSet)
                 .doOnNext(dataSet -> {
                     final String nucleus = dataSet.getSpectrum()
                                                   .getNuclei()[0];
                     final String atomType = Utils.getAtomTypeFromNucleus(nucleus);
                     connectivityStatisticsPerNucleus.putIfAbsent(nucleus, new ConcurrentHashMap<>());
                     ConnectivityStatistics.buildConnectivityStatistics(dataSet, atomType,
                                                                        connectivityStatisticsPerNucleus.get(nucleus));
                 })
                 .doAfterTerminate(() -> {
                     System.out.println(" -> connectivityStatistics done");
                     connectivityStatisticsPerNucleus.keySet()
                                                     .forEach(nucleus -> connectivityStatisticsPerNucleus.get(nucleus)
                                                                                                         .keySet()
                                                                                                         .forEach(
                                                                                                                 multiplicity -> connectivityStatisticsPerNucleus.get(
                                                                                                                         nucleus)
                                                                                                                                                                 .get(multiplicity)
                                                                                                                                                                 .keySet()
                                                                                                                                                                 .forEach(
                                                                                                                                                                         hybridization -> connectivityStatisticsPerNucleus.get(
                                                                                                                                                                                 nucleus)
                                                                                                                                                                                                                          .get(multiplicity)
                                                                                                                                                                                                                          .get(hybridization)
                                                                                                                                                                                                                          .keySet()
                                                                                                                                                                                                                          .forEach(
                                                                                                                                                                                                                                  shift -> {
                                                                                                                                                                                                                                      this.connectivityServiceImplementation.insert(
                                                                                                                                                                                                                                              new ConnectivityRecord(
                                                                                                                                                                                                                                                      null,
                                                                                                                                                                                                                                                      nucleus,
                                                                                                                                                                                                                                                      hybridization,
                                                                                                                                                                                                                                                      multiplicity,
                                                                                                                                                                                                                                                      shift,
                                                                                                                                                                                                                                                      connectivityStatisticsPerNucleus.get(
                                                                                                                                                                                                                                                              nucleus)
                                                                                                                                                                                                                                                                                      .get(multiplicity)
                                                                                                                                                                                                                                                                                      .get(hybridization)
                                                                                                                                                                                                                                                                                      .get(shift)))
                                                                                                                                                                                                                                                                            .subscribe();
                                                                                                                                                                                                                                  }))));
                     System.out.println(" -> done -> "
                                                + this.getCount()
                                                      .subscribe());
                 })
                 .subscribe();
    }

    @GetMapping(value = "/extractNeighborHybridizations", produces = "application/json")
    public void extractNeighborHybridizations(@RequestParam final String nucleus,
                                              @RequestParam final String hybridization,
                                              @RequestParam final String multiplicity, @RequestParam final int minShift,
                                              @RequestParam final int maxShift,
                                              @RequestParam final double thresholdNeighborCount,
                                              @RequestParam final double thresholdHybridizationCount) {
        final List<Map<String, Map<String, Map<Integer, Integer>>>> extractedConnectivityCounts = this.findByNucleusAndHybridizationAndMultiplicityAndShift(
                nucleus, hybridization, multiplicity, minShift, maxShift)
                                                                                                      .map(ConnectivityRecord::getConnectivityCounts)
                                                                                                      .collectList()
                                                                                                      .block();
        System.out.println(extractedConnectivityCounts);
        for (final Map<String, Map<String, Map<Integer, Integer>>> extractedConnectivityCount : extractedConnectivityCounts) {
            final List<String> extractedNeighborAtomTypes = ConnectivityStatistics.extractNeighborAtomTypes(
                    extractedConnectivityCount, thresholdNeighborCount);
            for (final String extractedNeighborAtomType : extractedNeighborAtomTypes) {
                final Map<String, Map<Integer, Integer>> extractedNeighborHybridizationMap = ConnectivityStatistics.extractNeighborHybridizations(
                        extractedConnectivityCount, extractedNeighborAtomType, thresholdHybridizationCount);
                System.out.println(extractedNeighborHybridizationMap);
            }
        }
    }
}
