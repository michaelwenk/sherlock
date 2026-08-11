package org.openscience.sherlock.dbservice.statistics.controller;

import casekit.nmr.elucidation.Constants;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import casekit.nmr.utils.Utils;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.sherlock.configuration.OpenApiConfiguration;
import org.openscience.sherlock.dbservice.dataset.db.model.DataSetRecord;
import org.openscience.sherlock.dbservice.job.ReplaceAllJobRegistry;
import org.openscience.sherlock.dbservice.job.ReplaceAllJobStatus;
import org.openscience.sherlock.dbservice.statistics.service.HybridizationServiceImplementation;
import org.openscience.sherlock.dbservice.statistics.service.model.HybridizationRecord;
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
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Hybridization Statistics", description = "Endpoints for querying and rebuilding hybridization statistics.")
@SecurityRequirement(name = OpenApiConfiguration.BASIC_AUTH_SCHEME)
@RestController
@RequestMapping(value = "/statistics/hybridization")
public class HybridizationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(HybridizationController.class);
    private static final String JOB_KIND = "hybridization";
    private static final int PROGRESS_REPORT_EVERY = 500;

    @Autowired
    private HybridizationServiceImplementation hybridizationServiceImplementation;
    @Autowired
    private Utilities utilities;
    @Autowired
    private ReplaceAllJobRegistry jobRegistry;

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

    @Operation(summary = "Start hybridization statistics rebuild", description = "Starts a background rebuild of the hybridization statistics collection for the selected nuclei. The endpoint returns immediately with HTTP 202 and the current rebuild status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Rebuild job accepted and started asynchronously"),
            @ApiResponse(responseCode = "409", description = "A rebuild job is already running")
    })
    @PostMapping(value = "/replaceAll")
    public ResponseEntity<ReplaceAllJobStatus> replaceAll(@RequestParam final String[] nuclei) {
        LOGGER.info("Received hybridization replaceAll request with nuclei={}", Arrays.toString(nuclei));
        final Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("nuclei", List.of(nuclei.clone()));
        final ReplaceAllJobStatus started = this.jobRegistry.startJob(JOB_KIND, parameters);
        final Flux<DataSet> dataSetFlux = this.utilities.getByDataSetSpectrumNuclei(nuclei)
                .map(DataSetRecord::getDataSet);
        this.startBackground(started.jobId(), dataSetFlux);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                this.jobRegistry.getCurrentJobStatus(JOB_KIND).orElse(started));
    }

    @Operation(summary = "Get hybridization statistics rebuild status", description = "Returns the current hybridization statistics rebuild status. If no active job exists, returns the latest finished status.")
    @GetMapping(value = "/replaceAll/status")
    public ReplaceAllJobStatus getReplaceAllStatus() {
        return this.jobRegistry.getCurrentJobStatus(JOB_KIND)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No hybridization statistics replaceAll status available yet"));
    }

    private void startBackground(final String jobId, final Flux<DataSet> dataSetFlux) {
        final Disposable subscription = this.replaceAll(dataSetFlux,
                batchSize -> this.jobRegistry.onBatchProcessed(jobId, batchSize))
                .doOnSubscribe(unused -> this.jobRegistry.markRunning(jobId))
                .doOnSuccess(unused -> this.jobRegistry.markCompleted(jobId))
                .doOnError(error -> {
                    LOGGER.error("Hybridization statistics replaceAll job {} failed", jobId, error);
                    this.jobRegistry.markFailed(jobId, error);
                })
                .doOnCancel(() -> {
                    LOGGER.warn("Hybridization statistics replaceAll job {} cancelled", jobId);
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
            LOGGER.info("Replacing hybridization statistics");
            LOGGER.info("Deleting previous hybridization statistics");

            // nucleus -> shift -> multiplicity -> elemental composition -> list of
            // hybridizations
            final ConcurrentHashMap<String, ConcurrentHashMap<Integer, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentLinkedDeque<String>>>>> entries = new ConcurrentHashMap<>();
            final AtomicInteger counter = new AtomicInteger(0);
            final AtomicInteger localReportCounter = new AtomicInteger(0);

            return this.hybridizationServiceImplementation.deleteAll()
                    .doOnSuccess(unused -> LOGGER.info("Previous hybridization statistics deleted"))
                    .thenMany(dataSetFlux)
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
                            LOGGER.info("Processed {} datasets while building hybridization statistics", currentCount);
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
                        LOGGER.info("Datasets processed; inserting hybridization statistics");
                    })
                    .then(this.persistEntries(entries))
                    .doOnSuccess(unused -> LOGGER.info("Hybridization statistics done"))
                    .doOnCancel(() -> LOGGER.warn("Hybridization statistics replaceAll pipeline cancelled"))
                    .doOnError(error -> LOGGER.error("Failed to rebuild hybridization statistics", error))
                    .onErrorMap(error -> error instanceof ResponseStatusException
                            ? error
                            : new ResponseStatusException(
                                    HttpStatus.INTERNAL_SERVER_ERROR,
                                    "Failed to rebuild hybridization statistics. See server logs for the root cause.",
                                    error));
        });
    }

    private Mono<Void> persistEntries(
            final ConcurrentHashMap<String, ConcurrentHashMap<Integer, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentLinkedDeque<String>>>>> entries) {
        return Flux.fromIterable(entries.entrySet())
                .concatMap(nucleusEntry -> Flux.fromIterable(nucleusEntry.getValue().entrySet())
                        .concatMap(shiftEntry -> Flux.fromIterable(shiftEntry.getValue().entrySet())
                                .concatMap(
                                        multiplicityEntry -> Flux.fromIterable(multiplicityEntry.getValue().entrySet())
                                                .concatMap(elementsEntry -> {
                                                    final Map<String, Integer[]> hybridizationCounts = new HashMap<>();
                                                    int counterTotal = 0;
                                                    for (final String hybridization : elementsEntry.getValue()) {
                                                        hybridizationCounts.putIfAbsent(hybridization,
                                                                new Integer[] { 0, 0 });
                                                        hybridizationCounts.get(hybridization)[0]++;
                                                        counterTotal++;
                                                    }
                                                    for (final String hybridization : elementsEntry.getValue()) {
                                                        hybridizationCounts.get(hybridization)[1] = counterTotal;
                                                    }
                                                    return this.hybridizationServiceImplementation.insert(
                                                            new HybridizationRecord(null,
                                                                    nucleusEntry.getKey(),
                                                                    shiftEntry.getKey(),
                                                                    multiplicityEntry.getKey(),
                                                                    elementsEntry.getKey(),
                                                                    hybridizationCounts));
                                                }))))
                .then();
    }

    private String buildElementsString(final String mf) {
        final List<String> elements = new ArrayList<>(Utils.getMolecularFormulaElementCounts(mf)
                .keySet());
        elements.remove("H");
        Collections.sort(elements);
        return String.join(",", elements);
    }
}
