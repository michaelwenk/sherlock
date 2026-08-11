package org.openscience.sherlock.dbservice.statistics.controller;

import casekit.nmr.analysis.ConnectivityStatistics;
import casekit.nmr.model.DataSet;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.sherlock.configuration.OpenApiConfiguration;
import org.openscience.sherlock.dbservice.dataset.db.model.DataSetRecord;
import org.openscience.sherlock.dbservice.job.ReplaceAllJobRegistry;
import org.openscience.sherlock.dbservice.job.ReplaceAllJobStatus;
import org.openscience.sherlock.dbservice.statistics.service.HeavyAtomStatisticsServiceImplementation;
import org.openscience.sherlock.dbservice.statistics.service.model.HeavyAtomStatisticsRecord;
import org.openscience.sherlock.dbservice.statistics.utils.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Heavy Atom Statistics", description = "Endpoints for querying and rebuilding heavy atom statistics.")
@SecurityRequirement(name = OpenApiConfiguration.BASIC_AUTH_SCHEME)
@RestController
@RequestMapping(value = "/statistics/heavyAtomStatistics")
public class HeavyAtomStatisticsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeavyAtomStatisticsController.class);
    private static final String JOB_KIND = "heavyAtom";
    private static final int PROGRESS_REPORT_EVERY = 500;

    @Autowired
    private HeavyAtomStatisticsServiceImplementation heavyAtomStatisticsServiceImplementation;
    @Autowired
    private Utilities utilities;
    @Autowired
    private ReplaceAllJobRegistry jobRegistry;

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

    @Operation(summary = "Start heavy atom statistics rebuild", description = "Starts a background rebuild of the heavy atom statistics collection from all stored datasets. The endpoint returns immediately with HTTP 202 and the current rebuild status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Rebuild job accepted and started asynchronously"),
            @ApiResponse(responseCode = "409", description = "A rebuild job is already running")
    })
    @PostMapping(value = "/replaceAll")
    public ResponseEntity<ReplaceAllJobStatus> replaceAll() {
        LOGGER.info("Received heavy atom statistics replaceAll request");
        final ReplaceAllJobStatus started = this.jobRegistry.startJob(JOB_KIND, Map.of());
        final Flux<DataSet> dataSetFlux = this.utilities.getAllDataSets()
                .map(DataSetRecord::getDataSet);
        this.startBackground(started.jobId(), dataSetFlux);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                this.jobRegistry.getCurrentJobStatus(JOB_KIND).orElse(started));
    }

    @Operation(summary = "Get heavy atom statistics rebuild status", description = "Returns the current heavy atom statistics rebuild status. If no active job exists, returns the latest finished status.")
    @GetMapping(value = "/replaceAll/status")
    public ReplaceAllJobStatus getReplaceAllStatus() {
        return this.jobRegistry.getCurrentJobStatus(JOB_KIND)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No heavy atom statistics replaceAll status available yet"));
    }

    private void startBackground(final String jobId, final Flux<DataSet> dataSetFlux) {
        final Disposable subscription = this.replaceAll(dataSetFlux,
                batchSize -> this.jobRegistry.onBatchProcessed(jobId, batchSize))
                .doOnSubscribe(unused -> this.jobRegistry.markRunning(jobId))
                .doOnSuccess(unused -> this.jobRegistry.markCompleted(jobId))
                .doOnError(error -> {
                    LOGGER.error("Heavy atom statistics replaceAll job {} failed", jobId, error);
                    this.jobRegistry.markFailed(jobId, error);
                })
                .doOnCancel(() -> {
                    LOGGER.warn("Heavy atom statistics replaceAll job {} cancelled", jobId);
                    this.jobRegistry.markCancelled(jobId);
                })
                .subscribe();
        this.jobRegistry.attachSubscription(jobId, subscription);
    }

    public Mono<Void> replaceAll(final Flux<DataSet> dataSetFlux) {
        return this.replaceAll(dataSetFlux, batchSize -> {
        });
    }

    public Mono<Void> replaceAll(final Flux<DataSet> dataSetFlux, final IntConsumer onBatchProcessed) {
        return Mono.defer(() -> {
            LOGGER.info("Replacing heavy atom statistics");
            LOGGER.info("Deleting previous heavy atom statistics");

            final AtomicInteger counter = new AtomicInteger(0);
            final AtomicInteger localReportCounter = new AtomicInteger(0);
            final Map<String, Map<String, Integer>> heavyAtomStatistics = new ConcurrentHashMap<>();

            return this.deleteAll()
                    .doOnSuccess(unused -> LOGGER.info("Previous heavy atom statistics deleted"))
                    .thenMany(dataSetFlux)
                    .doOnNext(dataSet -> {
                        final IAtomContainer structure = dataSet.getStructure()
                                .toAtomContainer();
                        ConnectivityStatistics.buildHeavyAtomsStatistics(structure, heavyAtomStatistics);

                        final int currentCount = counter.incrementAndGet();
                        if (currentCount % 50000 == 0) {
                            LOGGER.info("Processed {} datasets while building heavy atom statistics", currentCount);
                        }
                        if (localReportCounter.incrementAndGet() >= PROGRESS_REPORT_EVERY) {
                            final int reported = localReportCounter.getAndSet(0);
                            onBatchProcessed.accept(reported);
                        }
                    })
                    .then()
                    .doOnSuccess(unused -> {
                        final int remainder = localReportCounter.getAndSet(0);
                        if (remainder > 0) {
                            onBatchProcessed.accept(remainder);
                        }
                        LOGGER.info("Datasets processed; inserting heavy atom statistics");
                    })
                    .then(Flux.fromIterable(heavyAtomStatistics.entrySet())
                            .concatMap(entryPerElementsString -> Flux.fromIterable(
                                    entryPerElementsString.getValue().entrySet())
                                    .concatMap(entryByAtomPair -> this.heavyAtomStatisticsServiceImplementation.insert(
                                            new HeavyAtomStatisticsRecord(null,
                                                    entryPerElementsString.getKey(),
                                                    entryByAtomPair.getKey(),
                                                    entryByAtomPair.getValue()))))
                            .then())
                    .doOnSuccess(unused -> LOGGER.info("Heavy atom statistics done"))
                    .doOnCancel(() -> LOGGER.warn("Heavy atom statistics replaceAll pipeline cancelled"))
                    .doOnError(error -> LOGGER.error("Failed to rebuild heavy atom statistics", error))
                    .onErrorMap(error -> error instanceof ResponseStatusException
                            ? error
                            : new ResponseStatusException(
                                    HttpStatus.INTERNAL_SERVER_ERROR,
                                    "Failed to rebuild heavy atom statistics. See server logs for the root cause.",
                                    error));
        });
    }
}
