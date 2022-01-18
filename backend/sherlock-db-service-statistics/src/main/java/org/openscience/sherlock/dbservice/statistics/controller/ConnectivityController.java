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

import java.util.*;
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
        final Map<String, Map<String, Map<String, Map<Integer, Map<String, Map<String, Map<Integer, Integer>>>>>>> connectivityStatistics = new ConcurrentHashMap<>();
        // nucleus -> multiplicity -> hybridization -> shift (int) -> "elemental composition" -> connected atom symbol -> [#found, #notFound]
        final Map<String, Map<String, Map<String, Map<Integer, Map<String, Map<String, Integer[]>>>>>> occurrenceStatistics = new HashMap<>();
        Utilities.getByDataSetSpectrumNuclei(this.webClientBuilder, this.exchangeStrategies, nuclei)
                 .map(DataSetRecord::getDataSet)
                 .doOnNext(dataSet -> {
                     final String nucleus = dataSet.getSpectrum()
                                                   .getNuclei()[0];
                     final String atomType = Utils.getAtomTypeFromNucleus(nucleus);
                     connectivityStatistics.putIfAbsent(nucleus, new ConcurrentHashMap<>());
                     ConnectivityStatistics.buildConnectivityStatistics(dataSet, atomType,
                                                                        connectivityStatistics.get(nucleus));
                     occurrenceStatistics.putIfAbsent(nucleus, new ConcurrentHashMap<>());
                     ConnectivityStatistics.buildOccurrenceStatistics(dataSet, atomType,
                                                                      occurrenceStatistics.get(nucleus));
                 })
                 .doAfterTerminate(() -> {
                     System.out.println(" -> connectivityStatistics done");
                     connectivityStatistics.keySet()
                                           .forEach(nucleus -> connectivityStatistics.get(nucleus)
                                                                                     .keySet()
                                                                                     .forEach(
                                                                                             multiplicity -> connectivityStatistics.get(
                                                                                                                                           nucleus)
                                                                                                                                   .get(multiplicity)
                                                                                                                                   .keySet()
                                                                                                                                   .forEach(
                                                                                                                                           hybridization -> connectivityStatistics.get(
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
                                                                                                                                                                                                                  connectivityStatistics.get(
                                                                                                                                                                                                                                                nucleus)
                                                                                                                                                                                                                                        .get(multiplicity)
                                                                                                                                                                                                                                        .get(hybridization)
                                                                                                                                                                                                                                        .get(shift),
                                                                                                                                                                                                                  occurrenceStatistics.get(
                                                                                                                                                                                                                                              nucleus)
                                                                                                                                                                                                                                      .get(multiplicity)
                                                                                                                                                                                                                                      .get(hybridization)
                                                                                                                                                                                                                                      .get(shift)))
                                                                                                                                                                                                                                    .subscribe();
                                                                                                                                                                                          }))));
                 })
                 .subscribe();
    }

    @GetMapping(value = "/detectConnectivityCounts", produces = "application/json")
    public Map<String, Map<Integer, Map<Integer, Integer>>> detectConnectivityCounts(@RequestParam final String nucleus,
                                                                                     @RequestParam final int[] hybridizations,
                                                                                     @RequestParam final String multiplicity,
                                                                                     @RequestParam final int minShift,
                                                                                     @RequestParam final int maxShift,
                                                                                     @RequestParam final String mf) {
        final Map<String, Map<Integer, Map<Integer, Integer>>> extractedConnectivities = new HashMap<>();
        final Set<String> atomTypesByMf = Utils.getMolecularFormulaElementCounts(mf)
                                               .keySet();

        List<Map<String, Map<String, Map<Integer, Integer>>>> detectedConnectivitiesList;
        Map<String, Map<Integer, Map<Integer, Integer>>> convertedMap;

        // loop through all given hybridization states
        for (final int hybridization : hybridizations) {
            detectedConnectivitiesList = this.findByNucleusAndHybridizationAndMultiplicityAndShift(nucleus, "SP"
                                                     + hybridization, multiplicity, minShift, maxShift)
                                             .map(ConnectivityRecord::getConnectivityCounts)
                                             .collectList()
                                             .block();
            detectedConnectivitiesList.forEach(foundExtractedConnectivityCountsMap -> {
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
            for (final Map<String, Map<String, Map<Integer, Integer>>> extractedConnectivityTemp : detectedConnectivitiesList) {
                convertedMap = Utilities.convertToNumericHybridizationMapKeys(extractedConnectivityTemp);
                for (final String extractedNeighborAtomType : convertedMap.keySet()) {
                    extractedConnectivities.putIfAbsent(extractedNeighborAtomType, new HashMap<>());
                    for (final int extractedNeighborHybridization : convertedMap.get(extractedNeighborAtomType)
                                                                                .keySet()) {
                        extractedConnectivities.get(extractedNeighborAtomType)
                                               .putIfAbsent(extractedNeighborHybridization, new HashMap<>());
                        for (final Map.Entry<Integer, Integer> entryPerProtonsCount : convertedMap.get(
                                                                                                          extractedNeighborAtomType)
                                                                                                  .get(extractedNeighborHybridization)
                                                                                                  .entrySet()) {
                            extractedConnectivities.get(extractedNeighborAtomType)
                                                   .get(extractedNeighborHybridization)
                                                   .putIfAbsent(entryPerProtonsCount.getKey(), 0);
                            extractedConnectivities.get(extractedNeighborAtomType)
                                                   .get(extractedNeighborHybridization)
                                                   .put(entryPerProtonsCount.getKey(), extractedConnectivities.get(
                                                                                                                      extractedNeighborAtomType)
                                                                                                              .get(extractedNeighborHybridization)
                                                                                                              .get(entryPerProtonsCount.getKey())
                                                           + entryPerProtonsCount.getValue());
                        }
                    }
                }
            }
        }

        return extractedConnectivities;
    }

    @GetMapping(value = "/detectOccurrenceCounts", produces = "application/json")
    public Map<String, Integer[]> detectOccurrenceCounts(@RequestParam final String nucleus,
                                                         @RequestParam final int[] hybridizations,
                                                         @RequestParam final String multiplicity,
                                                         @RequestParam final int minShift,
                                                         @RequestParam final int maxShift,
                                                         @RequestParam final String mf) {
        final Map<String, Integer[]> extractedOccurrences = new HashMap<>();
        final List<String> elements = new ArrayList<>(Utils.getMolecularFormulaElementCounts(mf)
                                                           .keySet());
        elements.remove("H");
        Collections.sort(elements);
        final String elementsString = String.join(",", elements);

        List<Map<String, Map<String, Integer[]>>> extractedOccurrencesList;
        List<Map<String, Integer[]>> extractedOccurrencesSimplifiedList; // no elemental composition distinguishes anymore since we will filter out one only (through mf)
        List<String> foundElementalComposition;
        // loop through all given hybridization states
        for (final int hybridization : hybridizations) {
            extractedOccurrencesList = this.findByNucleusAndHybridizationAndMultiplicityAndShift(nucleus, "SP"
                                                   + hybridization, multiplicity, minShift, maxShift)
                                           .map(ConnectivityRecord::getOccurrenceCounts)
                                           .collectList()
                                           .block();
            extractedOccurrencesSimplifiedList = new ArrayList<>();
            for (final Map<String, Map<String, Integer[]>> foundExtractedOccurrenceCountMap : extractedOccurrencesList) {
                foundElementalComposition = foundExtractedOccurrenceCountMap.keySet()
                                                                            .stream()
                                                                            .filter(foundElementalCompositionTemp -> foundElementalCompositionTemp.equals(
                                                                                    elementsString))
                                                                            .collect(Collectors.toList());
                if (!foundElementalComposition.isEmpty()) {
                    extractedOccurrencesSimplifiedList.add(
                            foundExtractedOccurrenceCountMap.get(foundElementalComposition.get(0)));
                }
            }
            // loop over all results from DB in case a chemical shift range is given (minShift != maxShift)
            for (final Map<String, Integer[]> extractedOccurrencesSimplified : extractedOccurrencesSimplifiedList) {
                for (final Map.Entry<String, Integer[]> entryPerNeighborAtomType : extractedOccurrencesSimplified.entrySet()) {
                    extractedOccurrences.putIfAbsent(entryPerNeighborAtomType.getKey(), new Integer[]{0, 0});
                    extractedOccurrences.get(
                            entryPerNeighborAtomType.getKey())[0] += entryPerNeighborAtomType.getValue()[0];
                    extractedOccurrences.get(
                            entryPerNeighborAtomType.getKey())[1] += entryPerNeighborAtomType.getValue()[1];
                }
            }
        }

        return extractedOccurrences;
    }
}
