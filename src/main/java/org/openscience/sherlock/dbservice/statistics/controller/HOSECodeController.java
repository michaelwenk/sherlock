package org.openscience.sherlock.dbservice.statistics.controller;

import casekit.nmr.filterandrank.FilterAndRank;
import casekit.nmr.model.*;
import casekit.nmr.utils.Statistics;
import casekit.nmr.utils.Utils;

import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.nmrshiftdb.util.AtomUtils;
import org.openscience.nmrshiftdb.util.ExtendedHOSECodeGenerator;
import org.openscience.sherlock.configuration.OpenApiConfiguration;
import org.openscience.sherlock.dbservice.statistics.controller.model.HOSEReplaceAllJobStatus;
import org.openscience.sherlock.model.exchange.Transfer;
import org.openscience.sherlock.dbservice.statistics.service.HOSEReplaceAllJobService;
import org.openscience.sherlock.dbservice.statistics.service.HOSECodeServiceImplementation;
import org.openscience.sherlock.dbservice.statistics.service.model.HOSECodeRecord;
import org.openscience.sherlock.dbservice.statistics.utils.Utilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.Disposable;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.IntConsumer;
import java.util.concurrent.atomic.AtomicInteger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "HOSE Code Statistics", description = "Endpoints for querying, rebuilding, and using HOSE code predictions, including chunked bulk rebuilds of the HOSE code collection.")
@SecurityRequirement(name = OpenApiConfiguration.BASIC_AUTH_SCHEME)
@RestController
@RequestMapping(value = "/statistics/hosecode")
public class HOSECodeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(HOSECodeController.class);

    @Value("${sherlock.statistics.hosecode.replace-all-batch-size:250}")
    private int replaceAllBatchSize;

    private final HOSECodeServiceImplementation hoseCodeServiceImplementation;
    private final HOSEReplaceAllJobService hoseReplaceAllJobService;
    private final Utilities utilities;

    private final ExtendedHOSECodeGenerator extendedHOSECodeGenerator = new ExtendedHOSECodeGenerator();

    public HOSECodeController(final HOSECodeServiceImplementation hoseCodeServiceImplementation,
            final HOSEReplaceAllJobService hoseReplaceAllJobService,
            final Utilities utilities) {
        this.hoseCodeServiceImplementation = hoseCodeServiceImplementation;
        this.hoseReplaceAllJobService = hoseReplaceAllJobService;
        this.utilities = utilities;
    }

    private String decode(final String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return "";
    }

    @Operation(summary = "Get a HOSE code record by ID", description = "Returns the HOSE code record identified by the provided encoded record ID.")
    @GetMapping(value = "/getByID")
    public Optional<HOSECodeRecord> getByID(@RequestParam final String id) {
        return this.hoseCodeServiceImplementation.findById(this.decode(id)).blockOptional();
    }

    @Operation(summary = "Count HOSE code records", description = "Returns the total number of stored HOSE code records.")
    @GetMapping(value = "/count")
    public long getCount() {
        return this.hoseCodeServiceImplementation.count().block();
    }

    @Operation(summary = "List HOSE code records", description = "Returns all stored HOSE code records.")
    @GetMapping(value = "/getAll")
    public List<HOSECodeRecord> getAll() {
        return this.hoseCodeServiceImplementation.findAll().collectList().block();
    }

    @Operation(summary = "Delete HOSE code records", description = "Deletes every stored HOSE code record.")
    @DeleteMapping(value = "/deleteAll")
    public void deleteAll() {
        this.hoseCodeServiceImplementation.deleteAll().block();
    }

    @Operation(summary = "Start HOSE code rebuild", description = "Starts a background rebuild of the HOSE code collection for the selected nuclei and maximum sphere size. The endpoint returns immediately with HTTP 202 and the current rebuild status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Rebuild job accepted and started asynchronously"),
            @ApiResponse(responseCode = "409", description = "A rebuild job is already running")
    })
    @PostMapping(value = "/replaceAll")
    public ResponseEntity<HOSEReplaceAllJobStatus> replaceAll(
            @Parameter(description = "Nuclei to include, e.g. 13C or 1H", example = "13C") @RequestParam final String[] nuclei,
            @Parameter(description = "Maximum HOSE sphere size", example = "5") @RequestParam final int maxSphere) {
        try {
            LOGGER.info("Received HOSE replaceAll request with nuclei={} and maxSphere={}", Arrays.toString(nuclei),
                    maxSphere);
            final HOSEReplaceAllJobStatus startedJob = this.hoseReplaceAllJobService.startJob(
                    nuclei,
                    maxSphere,
                    this.replaceAllBatchSize);
            final Flux<DataSet> dataSetFlux = this.utilities.getByDataSetSpectrumNuclei(nuclei)
                    .map(dataSetRecord -> dataSetRecord.getDataSet());

                final String jobId = this.hoseReplaceAllJobService.getActiveJobIdOrThrow();
            final Disposable subscription = this.replaceAll(dataSetFlux, maxSphere, true,
                    batchSize -> this.hoseReplaceAllJobService.onBatchProcessed(jobId, batchSize))
                    .doOnSubscribe(unused -> this.hoseReplaceAllJobService.markRunning(jobId))
                    .doOnSuccess(unused -> this.hoseReplaceAllJobService.markCompleted(jobId))
                    .doOnError(error -> {
                        LOGGER.error("replaceAll background job {} failed", jobId, error);
                        this.hoseReplaceAllJobService.markFailed(jobId, error);
                    })
                    .doOnCancel(() -> {
                        LOGGER.warn("replaceAll background job {} cancelled", jobId);
                        this.hoseReplaceAllJobService.markCancelled(jobId);
                    })
                    .subscribe();
            this.hoseReplaceAllJobService.attachSubscription(jobId, subscription);

                final HOSEReplaceAllJobStatus response = this.hoseReplaceAllJobService
                    .getCurrentJobStatus()
                    .orElse(startedJob);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (final Exception error) {
            if (error instanceof ResponseStatusException responseStatusException) {
                throw responseStatusException;
            }
            LOGGER.error("Failed before creating replaceAll pipeline for nuclei={} and maxSphere={}",
                    Arrays.toString(nuclei), maxSphere, error);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed before HOSE code rebuild pipeline creation: "
                            + error.getClass().getSimpleName() + " - " + error.getMessage(),
                    error);
        }
    }

    @Operation(summary = "Get HOSE rebuild status", description = "Returns the current HOSE replaceAll status. If no active job exists, returns the latest finished status.")
    @GetMapping(value = "/replaceAll/status")
    public HOSEReplaceAllJobStatus getReplaceAllStatus() {
        return this.hoseReplaceAllJobService.getCurrentJobStatus()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No HOSE replaceAll status available yet"));
    }

    @Operation(summary = "Cancel active HOSE rebuild", description = "Cancels the active HOSE replaceAll job.")
    @PostMapping(value = "/replaceAll/cancel")
    public HOSEReplaceAllJobStatus cancelReplaceAll() {
        return this.hoseReplaceAllJobService.cancelActiveJob();
    }

    public Mono<Void> replaceAll(final Flux<DataSet> dataSetFlux, final int maxSphere, final boolean buildStatistics) {
        return this.replaceAll(dataSetFlux, maxSphere, buildStatistics, batchSize -> {
        });
    }

    public Mono<Void> replaceAll(final Flux<DataSet> dataSetFlux, final int maxSphere, final boolean buildStatistics,
            final IntConsumer onBatchProcessed) {
        try {
            return Mono.defer(() -> {
                if (this.replaceAllBatchSize <= 0) {
                    throw new IllegalStateException(
                            "sherlock.statistics.hosecode.replace-all-batch-size must be greater than 0, but was "
                                    + this.replaceAllBatchSize);
                }

                LOGGER.info("Replacing HOSE code collection");
                LOGGER.info("Deleting previous HOSE code collection");
                LOGGER.info("Building new HOSE code collection with batch size {}", this.replaceAllBatchSize);

                final AtomicInteger counter = new AtomicInteger(0);
                final AtomicInteger batchCounter = new AtomicInteger(0);
                return this.hoseCodeServiceImplementation.deleteAll()
                        .doOnSuccess(unused -> LOGGER.info("Previous HOSE code collection deleted"))
                        .thenMany(dataSetFlux.buffer(this.replaceAllBatchSize))
                        .concatMap(dataSetBatch -> {
                            final int currentBatch = batchCounter.incrementAndGet();
                            return Mono
                                    .fromCallable(() -> this.utilities.buildHOSECodeRecords(dataSetBatch, maxSphere))
                                    .flatMap(this.hoseCodeServiceImplementation::upsertValuesBulk)
                                    .doOnSuccess(unused -> {
                                        onBatchProcessed.accept(dataSetBatch.size());
                                        final int previousCount = counter.getAndAdd(dataSetBatch.size());
                                        final int currentCount = previousCount + dataSetBatch.size();
                                        if (previousCount / 10000 < currentCount / 10000) {
                                            LOGGER.info("Reached {} datasets", currentCount);
                                        }
                                    })
                                    .doOnError(error -> LOGGER.error(
                                            "Failed while processing HOSE batch {} (size={})",
                                            currentBatch,
                                            dataSetBatch.size(),
                                            error));
                        })
                        .then()
                        .doOnSuccess(unused -> LOGGER.info("Building HOSE codes done for all datasets"))
                        .doOnSuccess(unused -> LOGGER.info("New HOSE code collection built"))
                        .then(buildStatistics ? this.buildStatistics() : Mono.empty());
            })
                    .doOnCancel(() -> LOGGER.warn("HOSE replaceAll pipeline was cancelled before completion"))
                    .doOnError(error -> LOGGER.error("Failed to rebuild HOSE code collection", error))
                    .onErrorMap(error -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Failed to rebuild HOSE code collection. See server logs for the root cause.",
                            error));
        } catch (final Exception error) {
            LOGGER.error("Failed before HOSE replaceAll reactive pipeline started", error);
            return Mono.error(new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed before HOSE code rebuild started. See server logs for the root cause.",
                    error));
        }

    }

    @Operation(summary = "Build HOSE code statistics", description = "Calculates summary statistics for every stored HOSE code entry in the current collection contents and persists the derived metrics back to each HOSE code record.")
    @PostMapping(value = "/buildStatistics")
    public Mono<Void> buildStatistics() {
        LOGGER.info("Building HOSE code statistics");
        final AtomicInteger count = new AtomicInteger(0);
        return this.hoseCodeServiceImplementation.findAll()
                .concatMap(hoseCodeRecord -> {
                    if (hoseCodeRecord.getValues() == null || hoseCodeRecord.getValues().isEmpty()) {
                        return this.hoseCodeServiceImplementation.save(hoseCodeRecord);
                    }

                    final Map<String, Double[]> statistics = new HashMap<>();
                    List<Double> values;
                    double shift;

                    for (final Map.Entry<String, Map<String, Long>> entryPerSolvent : hoseCodeRecord.getValues()
                            .entrySet()) {
                        values = new ArrayList<>();
                        for (final Map.Entry<String, Long> entryPerShiftString : entryPerSolvent.getValue()
                                .entrySet()) {
                            shift = Double.parseDouble(entryPerShiftString.getKey()
                                    .replaceAll("_",
                                            "\\."));
                            if (shift == 1.0) {
                                continue;
                            }
                            for (long i = 0; i < entryPerShiftString.getValue(); i++) {
                                values.add(shift);
                            }
                        }
                        values = Statistics.removeOutliers(values, 1.5);
                        if (!values.isEmpty()) {
                            statistics.put(entryPerSolvent.getKey(),
                                    new Double[] { (double) values.size(),
                                            Collections.min(values),
                                            Statistics.getMean(values),
                                            Statistics.getMedian(values),
                                            Collections.max(values) });
                        }
                    }
                    hoseCodeRecord.setStatistics(statistics);
                    if (count.incrementAndGet()
                            % 100000 == 0) {
                        LOGGER.info("Reached {} HOSE records while building statistics", count.get());
                    }
                    return this.hoseCodeServiceImplementation.save(hoseCodeRecord);
                })
                .doOnComplete(() -> {
                    LOGGER.info("Build HOSE code statistics done");
                })
                .doOnError(error -> LOGGER.error("Failed while building HOSE code statistics", error))
                .then();

    }

    @Operation(summary = "Predict and filter candidate datasets", description = "Predicts spectra for the submitted SMILES structures and filters the generated datasets against the query spectrum.")
    @PostMapping(value = "/predictAndFilter")
    public Flux<DataSet> predictAndFilter(@RequestBody final Transfer transfer) {
        final String nucleus = transfer.getQuerySpectrum()
                .getNuclei()[0];
        final List<DataSet> dataSetList = new ArrayList<>();
        DataSet dataSet;
        for (final String smiles : transfer.getSmilesList()) {
            dataSet = this.predict(smiles, nucleus, transfer.getMaxSphere());
            if (dataSet != null) {
                dataSet = FilterAndRank.checkDataSet(dataSet, transfer.getQuerySpectrum(), transfer.getShiftTolerance(),
                        transfer.getMaximumAverageDeviation(),
                        transfer.isCheckMultiplicity(),
                        transfer.isCheckEquivalencesCount(),
                        transfer.isAllowLowerEquivalencesCount(),
                        transfer.getMultiplicitySectionsBuilder(), true,
                        transfer.getDetections());
                if (dataSet != null) {
                    dataSetList.add(dataSet);
                }
            }
        }

        return Flux.fromIterable(dataSetList);
    }

    @Operation(summary = "Predict a spectrum", description = "Predicts a dataset spectrum for the submitted SMILES structure, nucleus, and maximum HOSE sphere size.")
    @GetMapping(value = "/predict")
    public DataSet predict(@RequestParam final String smiles, @RequestParam final String nucleus,
            @RequestParam final int maxSphere) {

        final IAtomContainer structure;
        try {
            structure = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(this.decode(smiles));
        } catch (final InvalidSmilesException e) {
            e.printStackTrace();
            return null;
        }
        final String atomType = Utils.getAtomTypeFromNucleus(nucleus);

        final Assignment assignment;
        Signal signal;
        Optional<HOSECodeRecord> hoseCodeRecordOptional;
        HOSECodeRecord hoseCodeRecord;
        double predictedShift;
        String hoseCode;
        Double[] statistics;
        int signalIndex, sphere, count;
        Double min, max;
        List<Double> medians;

        try {
            Utils.placeExplicitHydrogens(structure);
            Utils.setAromaticityAndKekulize(structure);

            final DataSet dataSet = Utils.atomContainerToDataSet(structure, false);

            final Spectrum predictedSpectrum = new Spectrum();
            predictedSpectrum.setNuclei(new String[] { nucleus });
            predictedSpectrum.setSignals(new ArrayList<>());

            final Map<Integer, List<Integer>> assignmentMap = new HashMap<>();
            final Map<Integer, Double[]> predictionMeta = new HashMap<>();
            final Map<Integer, Map<String, List<Integer>>> collection = new HashMap<>();

            for (int i = 0; i < structure.getAtomCount(); i++) {
                if (!structure.getAtom(i)
                        .getSymbol()
                        .equals(atomType)) {
                    continue;
                }
                sphere = maxSphere;
                while (sphere >= 1) {
                    hoseCode = this.extendedHOSECodeGenerator.getHOSECode(structure, structure.getAtom(i), sphere);
                    collection.putIfAbsent(sphere, new HashMap<>());
                    collection.get(sphere)
                            .putIfAbsent(hoseCode, new ArrayList<>());
                    collection.get(sphere)
                            .get(hoseCode)
                            .add(i);

                    sphere--;
                }
            }
            final List<Integer> predictedAtomIndices = new ArrayList<>();

            sphere = maxSphere;
            while (sphere >= 1
                    && predictedAtomIndices.size() < structure.getAtomCount()) {
                for (final Map.Entry<String, List<Integer>> entryPerHOSECode : collection.get(sphere)
                        .entrySet()) {
                    if (predictedAtomIndices.containsAll(entryPerHOSECode.getValue())) {
                        continue;
                    }
                    medians = new ArrayList<>();
                    count = 0;
                    min = null;
                    max = null;
                    hoseCode = entryPerHOSECode.getKey();
                    hoseCodeRecordOptional = this.hoseCodeServiceImplementation.findById(hoseCode).blockOptional();
                    if (hoseCodeRecordOptional.isPresent()) {
                        hoseCodeRecord = hoseCodeRecordOptional.get();
                        for (final Map.Entry<String, Double[]> solventEntry : hoseCodeRecord.getStatistics()
                                .entrySet()) {
                            statistics = hoseCodeRecord.getStatistics()
                                    .get(solventEntry.getKey());
                            medians.add(statistics[3]);
                            count += statistics[0].intValue();
                            min = min == null
                                    ? statistics[1]
                                    : Double.min(min, statistics[1]);
                            max = max == null
                                    ? statistics[4]
                                    : Double.max(max, statistics[4]);
                        }
                    }
                    if (medians.isEmpty()) {
                        continue;
                    }
                    predictedShift = Statistics.getMean(medians);

                    // insert signals
                    for (final int atomIndex : entryPerHOSECode.getValue()) {
                        if (predictedAtomIndices.contains(atomIndex)) {
                            continue;
                        }
                        signal = new Signal();
                        signal.setNuclei(new String[] { nucleus });
                        signal.setShifts(new Double[] { predictedShift });
                        signal.setMultiplicity(Utils.getMultiplicityFromProtonsCount(
                                AtomUtils.getHcount(structure, structure.getAtom(atomIndex)))); // counts explicit H
                        signal.setEquivalencesCount(1);

                        signalIndex = predictedSpectrum.addSignal(signal);

                        assignmentMap.putIfAbsent(signalIndex, new ArrayList<>());
                        assignmentMap.get(signalIndex)
                                .add(atomIndex);

                        if (!predictionMeta.containsKey(signalIndex)) {
                            predictionMeta.put(signalIndex, new Double[] { (double) sphere, (double) count, min, max });
                        }
                        predictedAtomIndices.add(atomIndex);
                    }
                }
                sphere--;
            }

            Utils.convertExplicitToImplicitHydrogens(structure);
            dataSet.setStructure(new StructureCompact(structure));
            dataSet.addMetaInfo("smiles", SmilesGenerator.generic()
                    .create(structure));

            dataSet.setSpectrum(new SpectrumCompact(predictedSpectrum));
            assignment = new Assignment();
            assignment.setNuclei(predictedSpectrum.getNuclei());
            assignment.initAssignments(predictedSpectrum.getSignalCount());

            for (final Map.Entry<Integer, List<Integer>> entry : assignmentMap.entrySet()) {
                for (final int atomIndex : entry.getValue()) {
                    assignment.addAssignmentEquivalence(0, entry.getKey(), atomIndex);
                }
            }
            dataSet.setAssignment(assignment);

            dataSet.addAttachment("predictionMeta", predictionMeta);

            return dataSet;
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}
