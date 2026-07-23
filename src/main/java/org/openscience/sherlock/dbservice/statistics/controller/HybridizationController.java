package org.openscience.sherlock.dbservice.statistics.controller;

import casekit.nmr.elucidation.Constants;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import casekit.nmr.utils.Utils;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.sherlock.configuration.OpenApiConfiguration;
import org.openscience.sherlock.dbservice.dataset.db.model.DataSetRecord;
import org.openscience.sherlock.dbservice.statistics.service.HybridizationServiceImplementation;
import org.openscience.sherlock.dbservice.statistics.service.model.HybridizationRecord;
import org.openscience.sherlock.dbservice.statistics.utils.Utilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Hybridization Statistics", description = "Endpoints for querying and rebuilding hybridization statistics.")
@SecurityRequirement(name = OpenApiConfiguration.BASIC_AUTH_SCHEME)
@RestController
@RequestMapping(value = "/statistics/hybridization")
public class HybridizationController {

    @Autowired
    private HybridizationServiceImplementation hybridizationServiceImplementation;
    @Autowired
    private Utilities utilities;

    @Operation(summary = "Count hybridization statistics", description = "Returns the number of stored hybridization statistic records.")
    @GetMapping(value = "/count", produces = "application/json")
    public Mono<Long> getCount() {
        return this.hybridizationServiceImplementation.count();
    }

    @Operation(summary = "List hybridization statistics", description = "Streams all stored hybridization statistic records.")
    @GetMapping(value = "/getAll", produces = "application/stream+json")
    public Flux<HybridizationRecord> getAll() {
        return this.hybridizationServiceImplementation.findAll();
    }

    @Operation(summary = "Detect valid hybridizations", description = "Determines which hybridization states satisfy the requested occurrence threshold for the given spectrum constraints and molecular formula.")
    @GetMapping(value = "/detectHybridizations", produces = "application/json")
    public List<Integer> detectHybridizations(@RequestParam final String nucleus,
            @RequestParam final String multiplicity, @RequestParam final int minShift,
            @RequestParam final int maxShift, @RequestParam final float threshold,
            @RequestParam final String mf) {
        final List<HybridizationRecord> hybridizationRecordList = this.hybridizationServiceImplementation
                .findByNucleusAndMultiplicityAndElementsStringAndShift(
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
                    / totalCount) >= threshold) {
                validHydridizations.add(Constants.hybridizationConversionMap.get(entry.getKey()));
            }
        }

        return validHydridizations;
    }

    @Operation(summary = "Rebuild hybridization statistics", description = "Recomputes hybridization statistics for the selected nuclei from the stored datasets.")
    @PostMapping(value = "/replaceAll")
    public void replaceAll(@RequestParam final String[] nuclei) {
        this.replaceAll(utilities.getByDataSetSpectrumNuclei(nuclei)
                .map(DataSetRecord::getDataSet));
    }

    public void replaceAll(Flux<DataSet> dataSetFlux) {
        System.out.println("-> replacing hybridisation statistics ...");
        System.out.println(" -> deleting old hybridisation statistics ...");
        this.hybridizationServiceImplementation.deleteAll()
                .block();
        System.out.println(" -> previous hybridisation statistics deleted.");

        // nucleus -> shift -> multiplicity -> elemental composition -> list of
        // hybridizations
        final ConcurrentHashMap<String, ConcurrentHashMap<Integer, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentLinkedDeque<String>>>>> entries = new ConcurrentHashMap<>();
        System.out.println(" -> building new hybridisation statistics ...");
        final AtomicInteger counter = new AtomicInteger(0);

        dataSetFlux
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
                    IAtom atom;
                    for (int signalIndex = 0; signalIndex < assignmentValues[0].length; signalIndex++) {
                        multiplicity = spectrum.getSignal(signalIndex)
                                .getMultiplicity();
                        if (multiplicity == null) {
                            continue;
                        }
                        shift = null;
                        if (spectrum.getSignals()
                                .get(signalIndex)
                                .getShifts()[0] != null) {
                            shift = spectrum.getSignal(signalIndex)
                                    .getShift(0)
                                    .intValue();
                        }
                        for (int equivalenceIndex = 0; equivalenceIndex < assignmentValues[0][signalIndex].length; equivalenceIndex++) {
                            atomIndex = assignmentValues[0][signalIndex][equivalenceIndex];
                            atom = structure.getAtom(atomIndex);
                            if (atom.getHybridization() == null) {
                                continue;
                            }
                            hybridization = atom
                                    .getHybridization()
                                    .name();
                            if (shift == null
                                    || atom.getSymbol() == null
                                    || !atom.getSymbol().equals(atomType)) {
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

                    final int currentCount = counter.incrementAndGet();
                    if (currentCount % 50000 == 0) {
                        System.out.println(" --> processed " + currentCount + " datasets");
                    }
                })
                .doAfterTerminate(() -> {
                    System.out.println(" -> datasets processed: building hybridisation statistics entries ...");
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
                                        hybridizationCounts.putIfAbsent(hybridization, new Integer[] { 0, 0 });
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
                                            .doOnError(Throwable::printStackTrace)
                                            .subscribe();
                                }
                            }
                        }
                    }
                    System.out.println(" -> hybridisation statistics done");
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
