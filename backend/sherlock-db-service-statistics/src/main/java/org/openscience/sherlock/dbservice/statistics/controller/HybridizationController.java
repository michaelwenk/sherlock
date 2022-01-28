package org.openscience.sherlock.dbservice.statistics.controller;

import casekit.nmr.lsd.Constants;
import casekit.nmr.model.Spectrum;
import casekit.nmr.utils.Utils;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.sherlock.dbservice.statistics.service.HybridizationServiceImplementation;
import org.openscience.sherlock.dbservice.statistics.service.model.DataSetRecord;
import org.openscience.sherlock.dbservice.statistics.service.model.HybridizationRecord;
import org.openscience.sherlock.dbservice.statistics.utils.Utilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@RestController
@RequestMapping(value = "/hybridization")
public class HybridizationController {

    private final WebClient.Builder webClientBuilder;
    private final ExchangeStrategies exchangeStrategies;
    private final HybridizationServiceImplementation hybridizationServiceImplementation;

    @Autowired
    public HybridizationController(final WebClient.Builder webClientBuilder,
                                   final ExchangeStrategies exchangeStrategies,
                                   final HybridizationServiceImplementation hybridizationServiceImplementation) {
        this.webClientBuilder = webClientBuilder;
        this.exchangeStrategies = exchangeStrategies;
        this.hybridizationServiceImplementation = hybridizationServiceImplementation;
    }


    @GetMapping(value = "/count", produces = "application/json")
    public Mono<Long> getCount() {
        return this.hybridizationServiceImplementation.count();
    }

    @GetMapping(value = "/getAll", produces = "application/stream+json")
    public Flux<HybridizationRecord> getAll() {
        return this.hybridizationServiceImplementation.findAll();
    }

    @GetMapping(value = "/detectHybridizations", produces = "application/json")
    public List<Integer> detectHybridization(@RequestParam final String nucleus,
                                             @RequestParam final String multiplicity, @RequestParam final int minShift,
                                             @RequestParam final int maxShift, @RequestParam final float threshold,
                                             @RequestParam final String mf) {
        final List<HybridizationRecord> hybridizationRecordList = this.hybridizationServiceImplementation.findByNucleusAndMultiplicityAndElementsStringAndShift(
                                                                              nucleus, multiplicity, this.buildElementsString(mf), minShift, maxShift)
                                                                                                         .collectList()
                                                                                                         .block();
        final Map<String, Integer> totalHybridizationCounts = new HashMap<>();
        int totalCount = 0;
        for (final HybridizationRecord hybridizationRecord : hybridizationRecordList) {
            for (final String hybridization : hybridizationRecord.getHybridizationCounts()
                                                                 .keySet()) {
                if (Constants.hybridizationConversionMap.containsKey(hybridization)) {
                    totalHybridizationCounts.putIfAbsent(hybridization, 0);
                    totalHybridizationCounts.put(hybridization, totalHybridizationCounts.get(hybridization)
                            + hybridizationRecord.getHybridizationCounts()
                                                 .get(hybridization)[0]);
                    totalCount += hybridizationRecord.getHybridizationCounts()
                                                     .get(hybridization)[1];
                }
            }
        }
        final List<Integer> validHydridizations = new ArrayList<>();
        for (final Map.Entry<String, Integer> entry : totalHybridizationCounts.entrySet()) {
            if (((double) entry.getValue()
                    / totalCount)
                    >= threshold) {
                validHydridizations.add(Constants.hybridizationConversionMap.get(entry.getKey()));
            }
        }

        return validHydridizations;
    }

    @PostMapping(value = "/replaceAll")
    public void replaceAll(@RequestParam final String[] nuclei) {
        this.hybridizationServiceImplementation.deleteAll()
                                               .block();

        // nucleus -> shift -> multiplicity -> elemental composition -> list of hybridizations
        final ConcurrentHashMap<String, ConcurrentHashMap<Integer, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentLinkedDeque<String>>>>> entries = new ConcurrentHashMap<>();
        System.out.println("\nstarting\n");
        Utilities.getByDataSetSpectrumNuclei(this.webClientBuilder, this.exchangeStrategies, nuclei)
                 .map(DataSetRecord::getDataSet)
                 .doOnNext(dataSet -> {
                     final Spectrum spectrum = dataSet.getSpectrum()
                                                      .toSpectrum();
                     final String nucleus = spectrum.getNuclei()[0];
                     final String atomType = Utils.getAtomTypeFromNucleus(nucleus);
                     final IAtomContainer structure = dataSet.getStructure()
                                                             .toAtomContainer();
                     final int[][][] assignmentValues = dataSet.getAssignment()
                                                               .getAssignments();
                     final String elementsString = this.buildElementsString(dataSet.getMeta()
                                                                                   .get("mf"));

                     String multiplicity, hybridization;
                     Integer shift;
                     int atomIndex;
                     for (int signalIndex = 0; signalIndex
                             < assignmentValues[0].length; signalIndex++) {
                         multiplicity = spectrum.getSignal(signalIndex)
                                                .getMultiplicity();
                         if (multiplicity
                                 == null) {
                             continue;
                         }
                         shift = null;
                         if (spectrum.getSignals()
                                     .get(signalIndex)
                                     .getShifts()[0]
                                 != null) {
                             shift = spectrum.getSignal(signalIndex)
                                             .getShift(0)
                                             .intValue();
                         }
                         for (int equivalenceIndex = 0; equivalenceIndex
                                 < assignmentValues[0][signalIndex].length; equivalenceIndex++) {
                             atomIndex = assignmentValues[0][signalIndex][equivalenceIndex];
                             hybridization = structure.getAtom(atomIndex)
                                                      .getHybridization()
                                                      .name();
                             if (shift
                                     == null
                                     || structure.getAtom(atomIndex)
                                                 .getSymbol()
                                     == null
                                     || !structure.getAtom(atomIndex)
                                                  .getSymbol()
                                                  .equals(atomType)) {
                                 continue;
                             }
                             entries.putIfAbsent(nucleus, new ConcurrentHashMap<>());
                             entries.get(nucleus)
                                    .putIfAbsent(shift, new ConcurrentHashMap<>());
                             entries.get(nucleus)
                                    .get(shift)
                                    .putIfAbsent(multiplicity, new ConcurrentHashMap<>());
                             entries.get(nucleus)
                                    .get(shift)
                                    .get(multiplicity)
                                    .putIfAbsent(elementsString, new ConcurrentLinkedDeque<>());
                             entries.get(nucleus)
                                    .get(shift)
                                    .get(multiplicity)
                                    .get(elementsString)
                                    .add(hybridization);
                         }
                     }
                 })
                 .doAfterTerminate(() -> {
                     System.out.println("\nreached doAfterTerminate\n");
                     Map<String, Integer[]> hybridizationCounts;
                     for (final String nucleus : entries.keySet()) {
                         for (final int shift : entries.get(nucleus)
                                                       .keySet()) {
                             for (final String multiplicity : entries.get(nucleus)
                                                                     .get(shift)
                                                                     .keySet()) {
                                 for (final String elementsString : entries.get(nucleus)
                                                                           .get(shift)
                                                                           .get(multiplicity)
                                                                           .keySet()) {
                                     hybridizationCounts = new HashMap<>();
                                     int counterTotal = 0;
                                     for (final String hybridization : entries.get(nucleus)
                                                                              .get(shift)
                                                                              .get(multiplicity)
                                                                              .get(elementsString)) {
                                         hybridizationCounts.putIfAbsent(hybridization, new Integer[]{0, 0});
                                         hybridizationCounts.get(hybridization)[0]++;
                                         counterTotal++;
                                     }
                                     for (final String hybridization : entries.get(nucleus)
                                                                              .get(shift)
                                                                              .get(multiplicity)
                                                                              .get(elementsString)) {
                                         hybridizationCounts.get(hybridization)[1] = counterTotal;
                                     }
                                     this.hybridizationServiceImplementation.insert(
                                                 new HybridizationRecord(null, nucleus, shift, multiplicity, elementsString,
                                                                         hybridizationCounts))
                                                                            .subscribe();
                                 }
                             }
                         }
                     }
                     System.out.println(" -> done");
                 })
                 .subscribe();
    }

    private String buildElementsString(final String mf) {
        final List<String> elements = new ArrayList<>(Utils.getMolecularFormulaElementCounts(mf)
                                                           .keySet());
        elements.remove("H");
        Collections.sort(elements);
        return String.join(",", elements);
    }
}
