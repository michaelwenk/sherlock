package org.openscience.sherlock.dbservice.statistics.controller;

import casekit.nmr.analysis.ConnectivityStatistics;
import casekit.nmr.model.DataSet;

import org.openscience.cdk.interfaces.IAtomContainer;
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

@RestController
@RequestMapping(value = "/statistics/heavyAtomStatistics")
public class HeavyAtomStatisticsController {

    @Autowired
    private HeavyAtomStatisticsServiceImplementation heavyAtomStatisticsServiceImplementation;
    @Autowired
    private Utilities utilities;

    @GetMapping(value = "/count", produces = "application/json")
    public Mono<Long> getCount() {
        return this.heavyAtomStatisticsServiceImplementation.count();
    }

    @GetMapping(value = "/getAll", produces = "application/stream+json")
    public Flux<HeavyAtomStatisticsRecord> getAll() {
        return this.heavyAtomStatisticsServiceImplementation.findAll();
    }

    @GetMapping(value = "/findByMf", produces = "application/stream+json")
    public Flux<HeavyAtomStatisticsRecord> findByMf(@RequestParam final String mf) {
        final String elementsString = ConnectivityStatistics.buildElementsString(
                ConnectivityStatistics.buildElements(mf));
        return this.heavyAtomStatisticsServiceImplementation.findHeavyAtomStatisticsRecordByElementsString(
                elementsString);
    }

    @GetMapping(value = "/findByAtomPair", produces = "application/stream+json")
    public Flux<HeavyAtomStatisticsRecord> findByAtomPair(@RequestParam final String atomType1,
            @RequestParam final String atomType2) {
        final String atomPair = ConnectivityStatistics.buildAtomPairString(atomType1, atomType2);
        return this.heavyAtomStatisticsServiceImplementation.findHeavyAtomStatisticsRecordByAtomPair(atomPair);
    }

    @PostMapping(value = "/deleteAll")
    public Mono<Void> deleteAll() {
        return this.heavyAtomStatisticsServiceImplementation.deleteAll();
    }

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
