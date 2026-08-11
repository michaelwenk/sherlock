package org.openscience.sherlock.dbservice.dataset.controller;

import org.openscience.sherlock.dbservice.job.ReplaceAllJobRegistry;
import org.openscience.sherlock.dbservice.job.ReplaceAllJobStatus;
import org.openscience.sherlock.dbservice.statistics.controller.ConnectivityController;
import org.openscience.sherlock.dbservice.statistics.controller.HOSECodeController;
import org.openscience.sherlock.dbservice.statistics.controller.HeavyAtomStatisticsController;
import org.openscience.sherlock.dbservice.statistics.controller.HybridizationController;
import org.openscience.sherlock.configuration.OpenApiConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import casekit.nmr.model.DataSet;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.IntConsumer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Database Maintenance", description = "Endpoints for rebuilding Sherlock dataset-derived statistics collections.")
@SecurityRequirement(name = OpenApiConfiguration.BASIC_AUTH_SCHEME)
@RestController
@RequestMapping(value = "/database")
public class DatabaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseController.class);
    private static final String JOB_KIND = "buildStatistics";

    private final DataSetController dataSetController;
    private final ConnectivityController connectivityController;
    private final HybridizationController hybridizationController;
    private final HeavyAtomStatisticsController heavyAtomStatisticsController;
    private final HOSECodeController hoseCodeController;
    private final FragmentController fragmentController;
    private final ReplaceAllJobRegistry jobRegistry;

    public DatabaseController(final DataSetController dataSetController,
            final ConnectivityController connectivityController,
            final HybridizationController hybridizationController,
            final HeavyAtomStatisticsController heavyAtomStatisticsController,
            final HOSECodeController hoseCodeController,
            final FragmentController fragmentController,
            final ReplaceAllJobRegistry jobRegistry) {
        this.dataSetController = dataSetController;
        this.connectivityController = connectivityController;
        this.hybridizationController = hybridizationController;
        this.heavyAtomStatisticsController = heavyAtomStatisticsController;
        this.hoseCodeController = hoseCodeController;
        this.fragmentController = fragmentController;
        this.jobRegistry = jobRegistry;
    }

    @Operation(summary = "Fill derived databases", description = "Starts a background rebuild of connectivity, hybridization, heavy atom, HOSE code, and fragment statistics for the selected nucleus. Stage 1 runs connectivity, hybridization and heavy-atom statistics in parallel; stage 2 rebuilds HOSE codes and fragments in parallel. The endpoint returns immediately with HTTP 202 and the current composite status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Composite rebuild job accepted and started asynchronously"),
            @ApiResponse(responseCode = "409", description = "A composite rebuild job is already running")
    })
    @PostMapping("/buildStatistics")
    public ResponseEntity<ReplaceAllJobStatus> buildStatistics(@RequestParam String nucleus,
            @RequestParam int maxSphere) {
        LOGGER.info("Received buildStatistics request with nucleus={} and maxSphere={}", nucleus, maxSphere);

        final Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("nucleus", nucleus);
        parameters.put("maxSphere", maxSphere);
        parameters.put("stages", java.util.List.of(
                java.util.List.of("connectivity", "hybridization", "heavyAtom"),
                java.util.List.of("hosecode", "fragment")));
        final ReplaceAllJobStatus started = this.jobRegistry.startJob(JOB_KIND, parameters);
        final String jobId = started.jobId();
        // Every sub-stage reports its progress under its own key in `stageProgress` so
        // all
        // five sub-services (connectivity, hybridization, heavyAtom, HOSE, fragment)
        // are
        // individually visible. `datasetsProcessed` is kept as the max across stage
        // counters (see ReplaceAllJobRegistry.onStageBatchProcessed) so it always
        // reflects
        // the actual number of distinct DataSet objects handled by the pipeline,
        // without
        // being inflated by sub-services that re-process the same set.
        final IntConsumer stage1ConnectivityReporter = batchSize -> this.jobRegistry
                .onStageBatchProcessed(jobId, "stage1-connectivity", batchSize);
        final IntConsumer stage1HybridizationReporter = batchSize -> this.jobRegistry
                .onStageBatchProcessed(jobId, "stage1-hybridization", batchSize);
        final IntConsumer stage1HeavyAtomReporter = batchSize -> this.jobRegistry
                .onStageBatchProcessed(jobId, "stage1-heavyAtom", batchSize);
        final IntConsumer stage2HoseReporter = batchSize -> this.jobRegistry
                .onStageBatchProcessed(jobId, "stage2-hose", batchSize);
        final IntConsumer stage2FragmentReporter = batchSize -> this.jobRegistry
                .onStageBatchProcessed(jobId, "stage2-fragment", batchSize);

        final String[] nuclei = new String[] { nucleus };
        final Flux<DataSet> connectivityDataSets = this.dataSetController
                .getByDataSetSpectrumNuclei(nuclei)
                .map(dataSetRecord -> dataSetRecord.getDataSet());
        final Flux<DataSet> hybridizationDataSets = this.dataSetController
                .getByDataSetSpectrumNuclei(nuclei)
                .map(dataSetRecord -> dataSetRecord.getDataSet());
        final Flux<DataSet> heavyAtomDataSets = this.dataSetController
                .getByDataSetSpectrumNuclei(nuclei)
                .map(dataSetRecord -> dataSetRecord.getDataSet());
        final Flux<DataSet> hoseDataSets = this.dataSetController
                .getByDataSetSpectrumNuclei(nuclei)
                .map(dataSetRecord -> dataSetRecord.getDataSet());
        final Flux<DataSet> fragmentDataSets = this.dataSetController
                .getByDataSetSpectrumNuclei(nuclei)
                .map(dataSetRecord -> dataSetRecord.getDataSet());

        final Mono<Void> stage1 = Mono.when(
                this.connectivityController.replaceAll(connectivityDataSets, stage1ConnectivityReporter)
                        .doOnSubscribe(unused -> LOGGER.info("buildStatistics: stage 1 - connectivity started"))
                        .doOnSuccess(unused -> LOGGER.info("buildStatistics: stage 1 - connectivity done")),
                this.hybridizationController.replaceAll(hybridizationDataSets, stage1HybridizationReporter)
                        .doOnSubscribe(unused -> LOGGER.info("buildStatistics: stage 1 - hybridization started"))
                        .doOnSuccess(unused -> LOGGER.info("buildStatistics: stage 1 - hybridization done")),
                this.heavyAtomStatisticsController.replaceAll(heavyAtomDataSets, stage1HeavyAtomReporter)
                        .doOnSubscribe(unused -> LOGGER.info("buildStatistics: stage 1 - heavy atom stats started"))
                        .doOnSuccess(unused -> LOGGER.info("buildStatistics: stage 1 - heavy atom stats done")));

        final Mono<Void> stage2 = Mono.when(
                this.hoseCodeController.replaceAll(hoseDataSets, maxSphere, true, stage2HoseReporter)
                        .doOnSubscribe(unused -> LOGGER.info("buildStatistics: stage 2 - HOSE codes started"))
                        .doOnSuccess(unused -> LOGGER.info("buildStatistics: stage 2 - HOSE codes done")),
                this.fragmentController.replaceAll(fragmentDataSets, nucleus, stage2FragmentReporter)
                        .doOnSubscribe(unused -> LOGGER.info("buildStatistics: stage 2 - fragments started"))
                        .doOnSuccess(unused -> LOGGER.info("buildStatistics: stage 2 - fragments done")));

        final Disposable subscription = stage1
                .doOnSubscribe(unused -> {
                    this.jobRegistry.markRunning(jobId);
                    LOGGER.info("buildStatistics: starting stage 1 (connectivity, hybridization, heavy atom)");
                })
                .doOnSuccess(
                        unused -> LOGGER.info("buildStatistics: stage 1 completed; starting stage 2 (HOSE, fragments)"))
                .then(stage2)
                .doOnSuccess(unused -> {
                    LOGGER.info("buildStatistics job {} completed", jobId);
                    this.jobRegistry.markCompleted(jobId);
                })
                .doOnError(error -> {
                    LOGGER.error("buildStatistics job {} failed", jobId, error);
                    this.jobRegistry.markFailed(jobId, error);
                })
                .doOnCancel(() -> {
                    LOGGER.warn("buildStatistics job {} cancelled", jobId);
                    this.jobRegistry.markCancelled(jobId);
                })
                .subscribe();
        this.jobRegistry.attachSubscription(jobId, subscription);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                this.jobRegistry.getCurrentJobStatus(JOB_KIND).orElse(started));
    }

    @Operation(summary = "Get buildStatistics status", description = "Returns the current composite buildStatistics status. If no active job exists, returns the latest finished status.")
    @GetMapping("/buildStatistics/status")
    public ReplaceAllJobStatus getBuildStatisticsStatus() {
        return this.jobRegistry.getCurrentJobStatus(JOB_KIND)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No buildStatistics status available yet"));
    }

}
