package org.openscience.sherlock.dbservice.statistics.controller;

import casekit.nmr.analysis.ConnectivityStatistics;
import casekit.nmr.model.DataSet;
import casekit.nmr.utils.Utils;

import org.openscience.sherlock.configuration.OpenApiConfiguration;
import org.openscience.sherlock.dbservice.dataset.db.model.DataSetRecord;
import org.openscience.sherlock.dbservice.job.ReplaceAllJobRegistry;
import org.openscience.sherlock.dbservice.job.ReplaceAllJobStatus;
import org.openscience.sherlock.dbservice.statistics.service.ConnectivityServiceImplementation;
import org.openscience.sherlock.dbservice.statistics.service.model.ConnectivityRecord;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Connectivity Statistics", description = "Endpoints for querying and rebuilding connectivity statistics.")
@SecurityRequirement(name = OpenApiConfiguration.BASIC_AUTH_SCHEME)
@RestController
@RequestMapping(value = "/statistics/connectivity")
public class ConnectivityController {

        private static final Logger LOGGER = LoggerFactory.getLogger(ConnectivityController.class);
        private static final String JOB_KIND = "connectivity";
        private static final int PROGRESS_REPORT_EVERY = 500;

        @Autowired
        private ConnectivityServiceImplementation connectivityServiceImplementation;
        @Autowired
        private Utilities utilities;
        @Autowired
        private ReplaceAllJobRegistry jobRegistry;

        @Operation(summary = "Count connectivity statistics", description = "Returns the number of stored connectivity statistic records.")
        @GetMapping(value = "/count", produces = "application/json")
        public Mono<Long> getCount() {
                return this.connectivityServiceImplementation.count();
        }

        @Operation(summary = "List connectivity statistics", description = "Streams all stored connectivity statistic records.")
        @GetMapping(value = "/getAll", produces = "application/stream+json")
        public Flux<ConnectivityRecord> getAll() {
                return this.connectivityServiceImplementation.findAll();
        }

        @Operation(summary = "Find connectivity counts", description = "Streams connectivity statistics filtered by nucleus, hybridization, multiplicity, and shift range.")
        @GetMapping(value = "/getConnectivityCounts", produces = "application/stream+json")
        public Flux<ConnectivityRecord> findByNucleusAndHybridizationAndMultiplicityAndShift(
                        @RequestParam final String nucleus, @RequestParam final String hybridization,
                        @RequestParam final String multiplicity, @RequestParam final int minShift,
                        @RequestParam final int maxShift) {
                return this.connectivityServiceImplementation.findByNucleusAndHybridizationAndMultiplicityAndShift(
                                nucleus,
                                hybridization,
                                multiplicity,
                                minShift,
                                maxShift);
        }

        @Operation(summary = "Delete connectivity statistics", description = "Deletes every stored connectivity statistic record.")
        @PostMapping(value = "/deleteAll")
        public Mono<Void> deleteAll() {
                return this.connectivityServiceImplementation.deleteAll();
        }

        @Operation(summary = "Start connectivity statistics rebuild", description = "Starts a background rebuild of the connectivity statistics collection for the selected nuclei. The endpoint returns immediately with HTTP 202 and the current rebuild status.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "202", description = "Rebuild job accepted and started asynchronously"),
                        @ApiResponse(responseCode = "409", description = "A rebuild job is already running")
        })
        @PostMapping(value = "/replaceAll")
        public ResponseEntity<ReplaceAllJobStatus> replaceAll(@RequestParam final String[] nuclei) {
                LOGGER.info("Received connectivity replaceAll request with nuclei={}", Arrays.toString(nuclei));
                final Map<String, Object> parameters = new LinkedHashMap<>();
                parameters.put("nuclei", List.of(nuclei.clone()));
                final ReplaceAllJobStatus started = this.jobRegistry.startJob(JOB_KIND, parameters);
                final Flux<DataSet> dataSetFlux = this.utilities.getByDataSetSpectrumNuclei(nuclei)
                                .map(DataSetRecord::getDataSet);
                this.startBackground(started.jobId(), dataSetFlux);
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                                this.jobRegistry.getCurrentJobStatus(JOB_KIND).orElse(started));
        }

        @Operation(summary = "Get connectivity statistics rebuild status", description = "Returns the current connectivity statistics rebuild status. If no active job exists, returns the latest finished status.")
        @GetMapping(value = "/replaceAll/status")
        public ReplaceAllJobStatus getReplaceAllStatus() {
                return this.jobRegistry.getCurrentJobStatus(JOB_KIND)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "No connectivity statistics replaceAll status available yet"));
        }

        private void startBackground(final String jobId, final Flux<DataSet> dataSetFlux) {
                final Disposable subscription = this.replaceAll(dataSetFlux,
                                batchSize -> this.jobRegistry.onBatchProcessed(jobId, batchSize))
                                .doOnSubscribe(unused -> this.jobRegistry.markRunning(jobId))
                                .doOnSuccess(unused -> this.jobRegistry.markCompleted(jobId))
                                .doOnError(error -> {
                                        LOGGER.error("Connectivity statistics replaceAll job {} failed", jobId, error);
                                        this.jobRegistry.markFailed(jobId, error);
                                })
                                .doOnCancel(() -> {
                                        LOGGER.warn("Connectivity statistics replaceAll job {} cancelled", jobId);
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
                        LOGGER.info("Replacing connectivity statistics");
                        LOGGER.info("Deleting previous connectivity statistics");

                        final AtomicInteger counter = new AtomicInteger(0);
                        final AtomicInteger localReportCounter = new AtomicInteger(0);
                        // nucleus -> multiplicity -> hybridization -> shift (int) -> "elemental
                        // composition" -> connected atom symbol -> [#found, #notFound]
                        final Map<String, Map<String, Map<String, Map<Integer, Map<String, Map<String, Integer[]>>>>>> occurrenceStatistics = new ConcurrentHashMap<>();

                        return this.deleteAll()
                                        .doOnSuccess(unused -> LOGGER.info("Previous connectivity statistics deleted"))
                                        .thenMany(dataSetFlux)
                                        .doOnNext(dataSet -> {
                                                final String nucleus = dataSet.getSpectrum()
                                                                .getNuclei()[0];
                                                final String atomType = Utils.getAtomTypeFromNucleus(nucleus);
                                                occurrenceStatistics.putIfAbsent(nucleus, new ConcurrentHashMap<>());
                                                ConnectivityStatistics.buildOccurrenceStatistics(dataSet, atomType,
                                                                occurrenceStatistics.get(nucleus));

                                                final int currentCount = counter.incrementAndGet();
                                                if (currentCount % 50000 == 0) {
                                                        LOGGER.info(
                                                                        "Processed {} datasets while building connectivity statistics",
                                                                        currentCount);
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
                                                LOGGER.info("Datasets processed; inserting connectivity statistics");
                                        })
                                        .then(this.persistOccurrenceStatistics(occurrenceStatistics))
                                        .doOnSuccess(unused -> LOGGER.info("Connectivity statistics done"))
                                        .doOnCancel(() -> LOGGER
                                                        .warn("Connectivity statistics replaceAll pipeline cancelled"))
                                        .doOnError(error -> LOGGER
                                                        .error("Failed to rebuild connectivity statistics", error))
                                        .onErrorMap(error -> error instanceof ResponseStatusException
                                                        ? error
                                                        : new ResponseStatusException(
                                                                        HttpStatus.INTERNAL_SERVER_ERROR,
                                                                        "Failed to rebuild connectivity statistics. See server logs for the root cause.",
                                                                        error));
                });
        }

        private Mono<Void> persistOccurrenceStatistics(
                        final Map<String, Map<String, Map<String, Map<Integer, Map<String, Map<String, Integer[]>>>>>> occurrenceStatistics) {
                return Flux.fromIterable(occurrenceStatistics.entrySet())
                                .concatMap(nucleusEntry -> Flux.fromIterable(nucleusEntry.getValue().entrySet())
                                                .concatMap(multiplicityEntry -> Flux
                                                                .fromIterable(multiplicityEntry.getValue().entrySet())
                                                                .concatMap(hybridizationEntry -> Flux.fromIterable(
                                                                                hybridizationEntry.getValue()
                                                                                                .entrySet())
                                                                                .concatMap(shiftEntry -> this.connectivityServiceImplementation
                                                                                                .insert(new ConnectivityRecord(
                                                                                                                null,
                                                                                                                nucleusEntry.getKey(),
                                                                                                                hybridizationEntry
                                                                                                                                .getKey(),
                                                                                                                multiplicityEntry
                                                                                                                                .getKey(),
                                                                                                                shiftEntry.getKey(),
                                                                                                                shiftEntry
                                                                                                                                .getValue()))))))
                                .then();
        }

        @Operation(summary = "Detect occurrence counts", description = "Aggregates connectivity occurrence counts for the given nucleus, hybridizations, multiplicity, shift range, and molecular formula.")
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
                List<Map<String, Integer[]>> extractedOccurrencesSimplifiedList; // no elemental composition
                                                                                 // distinguishes
                                                                                 // anymore since we will filter out one
                                                                                 // only
                                                                                 // (through mf)
                List<String> foundElementalComposition;
                // loop through all given hybridization states
                for (final int hybridization : hybridizations) {
                        extractedOccurrencesList = this
                                        .findByNucleusAndHybridizationAndMultiplicityAndShift(nucleus, "SP"
                                                        + hybridization, multiplicity, minShift, maxShift)
                                        .map(ConnectivityRecord::getOccurrenceCounts)
                                        .collectList()
                                        .block();
                        extractedOccurrencesSimplifiedList = new ArrayList<>();
                        for (final Map<String, Map<String, Integer[]>> foundExtractedOccurrenceCountMap : extractedOccurrencesList) {
                                foundElementalComposition = foundExtractedOccurrenceCountMap.keySet()
                                                .stream()
                                                .filter(foundElementalCompositionTemp -> foundElementalCompositionTemp
                                                                .equals(
                                                                                elementsString))
                                                .collect(Collectors.toList());
                                if (!foundElementalComposition.isEmpty()) {
                                        extractedOccurrencesSimplifiedList.add(
                                                        foundExtractedOccurrenceCountMap
                                                                        .get(foundElementalComposition.get(0)));
                                }
                        }
                        // loop over all results from DB in case a chemical shift range is given
                        // (minShift != maxShift)
                        for (final Map<String, Integer[]> extractedOccurrencesSimplified : extractedOccurrencesSimplifiedList) {
                                for (final Map.Entry<String, Integer[]> entryPerNeighborAtomType : extractedOccurrencesSimplified
                                                .entrySet()) {
                                        extractedOccurrences.putIfAbsent(entryPerNeighborAtomType.getKey(),
                                                        new Integer[] { 0, 0 });
                                        extractedOccurrences.get(
                                                        entryPerNeighborAtomType
                                                                        .getKey())[0] += entryPerNeighborAtomType
                                                                                        .getValue()[0];
                                        extractedOccurrences.get(
                                                        entryPerNeighborAtomType
                                                                        .getKey())[1] += entryPerNeighborAtomType
                                                                                        .getValue()[1];
                                }
                        }
                }

                return extractedOccurrences;
        }
}
