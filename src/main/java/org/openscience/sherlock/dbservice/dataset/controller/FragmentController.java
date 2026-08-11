package org.openscience.sherlock.dbservice.dataset.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.openscience.cdk.fingerprint.BitSetFingerprint;
import org.openscience.sherlock.configuration.OpenApiConfiguration;
import org.openscience.sherlock.dbservice.dataset.db.model.MultiplicitySectionsSettingsRecord;
import org.openscience.sherlock.dbservice.dataset.db.service.jpa.CustomFragmentRepositoryImplementation;
import org.openscience.sherlock.dbservice.dataset.db.service.mongo.DataSetServiceImplementation;
import org.openscience.sherlock.dbservice.dataset.db.service.mongo.MultiplicitySectionsSettingsServiceImplementation;
import org.openscience.sherlock.dbservice.dataset.utils.FragmentControllerUtilities;
import org.openscience.sherlock.dbservice.job.ReplaceAllJobRegistry;
import org.openscience.sherlock.dbservice.job.ReplaceAllJobStatus;
import org.openscience.sherlock.model.exchange.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import casekit.nmr.analysis.MultiplicitySectionsBuilder;
import casekit.nmr.model.DataSet;
import casekit.nmr.similarity.Similarity;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Fragments", description = "Endpoints for fragment lookup and fragment collection rebuilds.")
@SecurityRequirement(name = OpenApiConfiguration.BASIC_AUTH_SCHEME)
@RestController
@RequestMapping(value = "/fragment")
public class FragmentController {

        private static final Logger LOGGER = LoggerFactory.getLogger(FragmentController.class);
        private static final String JOB_KIND = "fragment";

        @Autowired
        private DataSetServiceImplementation dataSetServiceImplementation;
        @Autowired
        private CustomFragmentRepositoryImplementation customFragmentRepositoryImplementation;
        @Autowired
        private MultiplicitySectionsSettingsServiceImplementation multiplicitySectionsSettingsServiceImplementation;
        @Autowired
        private ReplaceAllJobRegistry jobRegistry;

        @Operation(summary = "Find fragments by spectrum and formula", description = "Generates fragment candidates for the submitted spectrum using multiplicity settings, fingerprint set bits, and optional molecular formula filters.")
        @PostMapping(value = "/getBySpectrumAndMfAndSetBits", produces = "application/stream+json")
        public Flux<DataSet> getBySpectrumAndMfAndSetBits(@RequestBody final Transfer fragmentsDetectionTransfer) {
                final MultiplicitySectionsSettingsRecord multiplicitySectionsSettingsRecord = this.multiplicitySectionsSettingsServiceImplementation
                                .findByNucleus(
                                                fragmentsDetectionTransfer.getQuerySpectrum()
                                                                .getNuclei()[0])
                                .blockFirst();
                if (multiplicitySectionsSettingsRecord != null) {
                        final MultiplicitySectionsBuilder multiplicitySectionsBuilder = new MultiplicitySectionsBuilder();
                        multiplicitySectionsBuilder.setMinLimit(
                                        multiplicitySectionsSettingsRecord.getMultiplicitySectionsSettings()[0]);
                        multiplicitySectionsBuilder.setMaxLimit(
                                        multiplicitySectionsSettingsRecord.getMultiplicitySectionsSettings()[1]);
                        multiplicitySectionsBuilder.setStepSize(
                                        multiplicitySectionsSettingsRecord.getMultiplicitySectionsSettings()[2]);

                        final BitSetFingerprint bitSetFingerprint = Similarity.getBitSetFingerprint(
                                        fragmentsDetectionTransfer.getQuerySpectrum(), 0, multiplicitySectionsBuilder);
                        // also set neighbour bits to give more flexibility in request
                        for (final int setBit : bitSetFingerprint.getSetbits()) {
                                if (setBit > multiplicitySectionsBuilder.getMinLimit()) {
                                        bitSetFingerprint.set(setBit
                                                        - 1, true);
                                }
                                if (setBit < multiplicitySectionsBuilder.getMaxLimit()) {
                                        bitSetFingerprint.set(setBit
                                                        + 1, true);
                                }
                        }

                        return Flux.fromIterable(FragmentControllerUtilities.getFragments(
                                        fragmentsDetectionTransfer.getQuerySpectrum(),
                                        bitSetFingerprint,
                                        multiplicitySectionsBuilder.getSteps(),
                                        fragmentsDetectionTransfer.getShiftTolerance(),
                                        fragmentsDetectionTransfer.getMaximumAverageDeviation(),
                                        fragmentsDetectionTransfer.getMf(),
                                        fragmentsDetectionTransfer.getHybridizationList(),
                                        this.customFragmentRepositoryImplementation));
                }

                return Flux.fromIterable(new ArrayList<>());
        }

        @Operation(summary = "Start fragment collection rebuild", description = "Starts a background rebuild of the fragment collection for the selected nucleus from the stored dataset records. The endpoint returns immediately with HTTP 202 and the current rebuild status.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "202", description = "Rebuild job accepted and started asynchronously"),
                        @ApiResponse(responseCode = "409", description = "A rebuild job is already running")
        })
        @PostMapping(value = "/replaceAll")
        public ResponseEntity<ReplaceAllJobStatus> replaceAll(@RequestParam final String nucleus) {
                LOGGER.info("Received fragment replaceAll request for nucleus={}", nucleus);
                final Map<String, Object> parameters = new LinkedHashMap<>();
                parameters.put("nucleus", nucleus);
                final ReplaceAllJobStatus started = this.jobRegistry.startJob(JOB_KIND, parameters);
                final Flux<DataSet> dataSetFlux = this.dataSetServiceImplementation
                                .findByDataSetSpectrumNuclei(new String[] { nucleus })
                                .map(dataSetRecord -> dataSetRecord.getDataSet());
                this.startBackground(started.jobId(), dataSetFlux, nucleus);
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                                this.jobRegistry.getCurrentJobStatus(JOB_KIND).orElse(started));
        }

        @Operation(summary = "Get fragment rebuild status", description = "Returns the current fragment rebuild status. If no active job exists, returns the latest finished status.")
        @GetMapping(value = "/replaceAll/status")
        public ReplaceAllJobStatus getReplaceAllStatus() {
                return this.jobRegistry.getCurrentJobStatus(JOB_KIND)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "No fragment replaceAll status available yet"));
        }

        private void startBackground(final String jobId, final Flux<DataSet> dataSetFlux, final String nucleus) {
                final Disposable subscription = this.replaceAll(dataSetFlux, nucleus,
                                batchSize -> this.jobRegistry.onBatchProcessed(jobId, batchSize))
                                .doOnSubscribe(unused -> this.jobRegistry.markRunning(jobId))
                                .doOnSuccess(unused -> this.jobRegistry.markCompleted(jobId))
                                .doOnError(error -> {
                                        LOGGER.error("Fragment replaceAll job {} failed", jobId, error);
                                        this.jobRegistry.markFailed(jobId, error);
                                })
                                .doOnCancel(() -> {
                                        LOGGER.warn("Fragment replaceAll job {} cancelled", jobId);
                                        this.jobRegistry.markCancelled(jobId);
                                })
                                .subscribe();
                this.jobRegistry.attachSubscription(jobId, subscription);
        }

        public Mono<Void> replaceAll(final Flux<DataSet> dataSetFlux, final String nucleus) {
                return this.replaceAll(dataSetFlux, nucleus, batchSize -> {
                });
        }

        public Mono<Void> replaceAll(final Flux<DataSet> dataSetFlux, final String nucleus,
                        final java.util.function.IntConsumer onBatchProcessed) {
                return FragmentControllerUtilities.replaceAll(dataSetFlux, nucleus,
                                this.customFragmentRepositoryImplementation,
                                this.multiplicitySectionsSettingsServiceImplementation,
                                onBatchProcessed);
        }

}
