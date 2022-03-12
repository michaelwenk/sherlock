package org.openscience.sherlock.dbservice.statistics.controller;

import casekit.nmr.analysis.ConnectivityStatistics;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.sherlock.dbservice.statistics.service.HeavyAtomStatisticsServiceImplementation;
import org.openscience.sherlock.dbservice.statistics.service.model.DataSetRecord;
import org.openscience.sherlock.dbservice.statistics.service.model.HeavyAtomStatisticsRecord;
import org.openscience.sherlock.dbservice.statistics.utils.Utilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping(value = "/heavyAtomStatistics")
public class HeavyAtomStatisticsController {

    private final WebClient.Builder webClientBuilder;
    private final ExchangeStrategies exchangeStrategies;
    private final HeavyAtomStatisticsServiceImplementation heavyAtomStatisticsServiceImplementation;

    @Autowired
    public HeavyAtomStatisticsController(final WebClient.Builder webClientBuilder,
                                         final ExchangeStrategies exchangeStrategies,
                                         final HeavyAtomStatisticsServiceImplementation heavyAtomStatisticsServiceImplementation) {
        this.webClientBuilder = webClientBuilder;
        this.exchangeStrategies = exchangeStrategies;
        this.heavyAtomStatisticsServiceImplementation = heavyAtomStatisticsServiceImplementation;
    }

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
        System.out.println(" --> delete all DB entries...");
        this.deleteAll()
            .block();
        System.out.println(" --> deleted all DB entries!");

        System.out.println(" --> fetching all datasets, build heavy atom statistics and store...");
        final Map<String, Map<String, Integer>> heavyAtomStatistics = new ConcurrentHashMap<>();
        Utilities.getAllDataSets(this.webClientBuilder, this.exchangeStrategies)
                 .map(DataSetRecord::getDataSet)
                 .doOnNext(dataSet -> {
                     final IAtomContainer structure = dataSet.getStructure()
                                                             .toAtomContainer();
                     ConnectivityStatistics.buildHeavyAtomsStatistics(structure, heavyAtomStatistics);
                 })
                 .doAfterTerminate(() -> {
                     System.out.println("\nreached doAfterTerminate\n");

                     for (final Map.Entry<String, Map<String, Integer>> entryPerElementsString : heavyAtomStatistics.entrySet()) {
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

                     System.out.println(" -> done");
                 })
                 .subscribe();
    }
}
