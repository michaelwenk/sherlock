package org.openscience.sherlock.dbservice.statistics.controller;

import casekit.nmr.analysis.ConnectivityStatistics;
import casekit.nmr.model.DataSet;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.sherlock.configuration.OpenApiConfiguration;
import org.openscience.sherlock.dbservice.dataset.db.model.DataSetRecord;
import org.openscience.sherlock.dbservice.statistics.service.HeavyAtomStatisticsServiceImplementation;
import org.openscience.sherlock.dbservice.statistics.service.model.HeavyAtomStatisticsRecord;
import org.openscience.sherlock.dbservice.statistics.utils.Utilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Heavy Atom Statistics", description = "Endpoints for querying and rebuilding heavy atom statistics.")
@SecurityRequirement(name = OpenApiConfiguration.BASIC_AUTH_SCHEME)
@RestController
@RequestMapping(value = "/statistics/heavyAtomStatistics")
public class HeavyAtomStatisticsController {

    @Autowired
    private HeavyAtomStatisticsServiceImplementation heavyAtomStatisticsServiceImplementation;
    @Autowired
    private Utilities utilities;

    @Operation(summary = "Count heavy atom statistics", description = "Returns the number of stored heavy atom statistic records.")
    @GetMapping(value = "/count", produces = "application/json")
    public Mono<Long> getCount() {
        return this.heavyAtomStatisticsServiceImplementation.count();
    }

    @Operation(summary = "List heavy atom statistics", description = "Streams all stored heavy atom statistic records.")
    @GetMapping(value = "/getAll", produces = "application/stream+json")
    public Flux<HeavyAtomStatisticsRecord> getAll() {
        return this.heavyAtomStatisticsServiceImplementation.findAll();
    }

    @Operation(summary = "Find heavy atom statistics by molecular formula", description = "Streams heavy atom statistics matching the normalized element set of the provided molecular formula.")
    @GetMapping(value = "/findByMf", produces = "application/stream+json")
    public Flux<HeavyAtomStatisticsRecord> findByMf(@RequestParam final String mf) {
        final String elementsString = ConnectivityStatistics.buildElementsString(
                ConnectivityStatistics.buildElements(mf));
        return this.heavyAtomStatisticsServiceImplementation.findHeavyAtomStatisticsRecordByElementsString(
                elementsString);
    }

    @Operation(summary = "Find heavy atom statistics by atom pair", description = "Streams heavy atom statistics for the specified atom type pair.")
    @GetMapping(value = "/findByAtomPair", produces = "application/stream+json")
    public Flux<HeavyAtomStatisticsRecord> findByAtomPair(@RequestParam final String atomType1,
            @RequestParam final String atomType2) {
        final String atomPair = ConnectivityStatistics.buildAtomPairString(atomType1, atomType2);
        return this.heavyAtomStatisticsServiceImplementation.findHeavyAtomStatisticsRecordByAtomPair(atomPair);
    }

    @Operation(summary = "Delete heavy atom statistics", description = "Deletes every stored heavy atom statistic record.")
    @PostMapping(value = "/deleteAll")
    public Mono<Void> deleteAll() {
        return this.heavyAtomStatisticsServiceImplementation.deleteAll();
    }

    @Operation(summary = "Rebuild heavy atom statistics", description = "Recomputes heavy atom statistics from all stored datasets.")
    @PostMapping(value = "/replaceAll")
    public void replaceAll() {
        this.replaceAll(utilities.getAllDataSets()
                .map(DataSetRecord::getDataSet));
    }

    public void replaceAll(final Flux<DataSet> dataSetFlux) {
        System.out.println(" -> replacing heavy atom statistics ...");
        System.out.println(" -> deleting previous heavy atom statistics ...");
        this.deleteAll()
                .block();
        System.out.println(" -> previous heavy atom statistics deleted");

        System.out.println(" -> building heavy atom statistics ...");
        final AtomicInteger counter = new AtomicInteger(0);

        final Map<String, Map<String, Integer>> heavyAtomStatistics = new ConcurrentHashMap<>();
        dataSetFlux.doOnNext(dataSet -> {
            final IAtomContainer structure = dataSet.getStructure()
                    .toAtomContainer();
            ConnectivityStatistics.buildHeavyAtomsStatistics(structure, heavyAtomStatistics);

            final int currentCount = counter.incrementAndGet();
            if (currentCount % 50000 == 0) {
                System.out.println(" --> processed " + currentCount + " datasets");
            }
        }).doAfterTerminate(() -> {
            System.out.println(
                    " -> datasets processed: inserting heavy atom statistics ...");
            for (final Map.Entry<String, Map<String, Integer>> entryPerElementsString : heavyAtomStatistics
                    .entrySet()) {
                for (final Map.Entry<String, Integer> entryByAtomPair : entryPerElementsString.getValue()
                        .entrySet()) {
                    this.heavyAtomStatisticsServiceImplementation.insert(
                            new HeavyAtomStatisticsRecord(null, entryPerElementsString.getKey(),
                                    entryByAtomPair.getKey(),
                                    entryByAtomPair.getValue()))
                            .doOnError(Throwable::printStackTrace)
                            .subscribe();
                }
            }
            System.out.println(" -> heavy atom statistics done");
        }).subscribe();
    }
}
