package org.openscience.sherlock.dbservice.statistics.controller;

import casekit.nmr.analysis.ConnectivityStatistics;
import casekit.nmr.utils.Utils;
import org.openscience.sherlock.dbservice.statistics.service.ConnectivityServiceImplementation;
import org.openscience.sherlock.dbservice.statistics.service.model.ConnectivityRecord;
import org.openscience.sherlock.dbservice.statistics.service.model.DataSetRecord;
import org.openscience.sherlock.dbservice.statistics.utils.Utilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/connectivity")
public class ConnectivityController {

    private final WebClient.Builder webClientBuilder;
    private final ExchangeStrategies exchangeStrategies;
    private final ConnectivityServiceImplementation connectivityServiceImplementation;

    @Autowired
    public ConnectivityController(final WebClient.Builder webClientBuilder, final ExchangeStrategies exchangeStrategies,
                                  final ConnectivityServiceImplementation connectivityServiceImplementation) {
        this.webClientBuilder = webClientBuilder;
        this.exchangeStrategies = exchangeStrategies;
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
        Utilities.getByDataSetSpectrumNuclei(this.webClientBuilder, this.exchangeStrategies, nuclei)
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
                                                      .block());
                 })
                 .subscribe();
    }

    @GetMapping(value = "/detectConnectivities", produces = "application/json")
    public Map<String, Map<Integer, Set<Integer>>> detectConnectivities(@RequestParam final String nucleus,
                                                                        @RequestParam final int[] hybridizations,
                                                                        @RequestParam final String multiplicity,
                                                                        @RequestParam final int minShift,
                                                                        @RequestParam final int maxShift,
                                                                        @RequestParam final double elementCountThreshold,
                                                                        @RequestParam final String mf,
                                                                        @RequestParam final boolean onAtomTypeLevel,
                                                                        @RequestParam final Set<Integer> knownCarbonHybridizations) {
        final Map<String, Map<Integer, Map<Integer, Integer>>> extractedConnectivitiesAll = new HashMap<>();
        final Set<String> atomTypesByMf = Utils.getMolecularFormulaElementCounts(mf)
                                               .keySet();

        List<Map<String, Map<String, Map<Integer, Integer>>>> detectedConnectivities;
        Map<String, Map<Integer, Map<Integer, Integer>>> convertedMap;

        // loop through all given hybridization states
        for (final int hybridization : hybridizations) {
            detectedConnectivities = this.findByNucleusAndHybridizationAndMultiplicityAndShift(nucleus, "SP"
                                                 + hybridization, multiplicity, minShift, maxShift)
                                         .map(ConnectivityRecord::getConnectivityCounts)
                                         .collectList()
                                         .block();
            detectedConnectivities.forEach(foundExtractedConnectivityCountsMap -> {
                final Set<String> foundAtomTypesToIgnore = foundExtractedConnectivityCountsMap.keySet()
                                                                                              .stream()
                                                                                              .filter(foundAtomType -> !atomTypesByMf.contains(
                                                                                                      foundAtomType))
                                                                                              .collect(
                                                                                                      Collectors.toSet());
                for (final String foundAtomTypeToIgnore : foundAtomTypesToIgnore) {
                    foundExtractedConnectivityCountsMap.remove(foundAtomTypeToIgnore);
                }
            });
            // loop over all results from DB in case a chemical shift range is given (minShift != maxShift)
            for (final Map<String, Map<String, Map<Integer, Integer>>> extractedConnectivityTemp : detectedConnectivities) {
                convertedMap = ConnectivityStatistics.convertToNumericHybridizationMapKeys(extractedConnectivityTemp);
                for (final String extractedNeighborAtomType : convertedMap.keySet()) {
                    extractedConnectivitiesAll.putIfAbsent(extractedNeighborAtomType, new HashMap<>());
                    for (final int extractedNeighborHybridization : convertedMap.get(extractedNeighborAtomType)
                                                                                .keySet()) {
                        extractedConnectivitiesAll.get(extractedNeighborAtomType)
                                                  .putIfAbsent(extractedNeighborHybridization, new HashMap<>());
                        for (final Map.Entry<Integer, Integer> entryPerProtonsCount : convertedMap.get(
                                                                                                          extractedNeighborAtomType)
                                                                                                  .get(extractedNeighborHybridization)
                                                                                                  .entrySet()) {
                            extractedConnectivitiesAll.get(extractedNeighborAtomType)
                                                      .get(extractedNeighborHybridization)
                                                      .putIfAbsent(entryPerProtonsCount.getKey(), 0);
                            extractedConnectivitiesAll.get(extractedNeighborAtomType)
                                                      .get(extractedNeighborHybridization)
                                                      .put(entryPerProtonsCount.getKey(),
                                                           extractedConnectivitiesAll.get(extractedNeighborAtomType)
                                                                                     .get(extractedNeighborHybridization)
                                                                                     .get(entryPerProtonsCount.getKey())
                                                                   + entryPerProtonsCount.getValue());
                        }
                    }
                }
            }
        }

        return ConnectivityStatistics.filterExtractedConnectivities(extractedConnectivitiesAll, elementCountThreshold,
                                                                    onAtomTypeLevel, knownCarbonHybridizations);
    }
}
