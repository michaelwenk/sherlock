package org.openscience.sherlock.dbservice.dataset.utils;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.BitSetFingerprint;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.sherlock.dbservice.dataset.config.DatasetJpaConfig;
import org.openscience.sherlock.dbservice.dataset.db.model.MultiplicitySectionsSettingsRecord;
import org.openscience.sherlock.dbservice.dataset.db.service.jpa.CustomFragmentRepositoryImplementation;
import org.openscience.sherlock.dbservice.dataset.db.service.mongo.MultiplicitySectionsSettingsServiceImplementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import casekit.nmr.analysis.MultiplicitySectionsBuilder;
import casekit.nmr.fragments.FragmentUtilities;
import casekit.nmr.fragments.fragmentation.Fragmentation;
import casekit.nmr.model.Assignment;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import casekit.nmr.similarity.Similarity;
import casekit.nmr.utils.Utils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class FragmentControllerUtilities {

        private static final Logger LOGGER = LoggerFactory.getLogger(FragmentControllerUtilities.class);
        private static final int PROGRESS_REPORT_EVERY = 500;

        private static final Gson gson = new GsonBuilder().create();

        public static List<DataSet> getFragments(final Spectrum querySpectrum,
                        final BitSetFingerprint bitSetFingerprint,
                        final int bitLength, final double shiftTolerance,
                        final double maximumAverageDeviation, final String mf,
                        final List<List<Integer>> hybridizationList,
                        final CustomFragmentRepositoryImplementation customFragmentRepositoryImplementation) {
                final BigInteger bigInteger = BitUtilities.buildBits(bitSetFingerprint, bitLength);

                System.out.println(" -> query: nucleus: " + querySpectrum.getNuclei()[0] + ", bits ("
                                + bitLength
                                + "): "
                                + BitUtilities.buildBitStringFromBigInteger(bigInteger, bitLength));

                final String bitString = BitUtilities.buildBitStringFromBigInteger(bigInteger, bitLength);
                final List<String> subDataSetStringList = customFragmentRepositoryImplementation
                                .findBySetBits(querySpectrum.getNuclei()[0], bitString);
                final Map<String, DataSet> fragmentsMap = new HashMap<>();
                fineSearch(fragmentsMap, subDataSetStringList, querySpectrum,
                                shiftTolerance,
                                maximumAverageDeviation, hybridizationList, mf);

                System.out.println(" --> fragments: "
                                + fragmentsMap.size());
                // ###########################################################

                return fragmentsMap.values()
                                .stream()
                                .sorted((dataSet1, dataSet2) -> {
                                        final int atomCountComparison = -1
                                                        * Integer.compare(dataSet1.getStructure()
                                                                        .atomCount(),
                                                                        dataSet2.getStructure()
                                                                                        .atomCount());
                                        if (atomCountComparison != 0) {
                                                return atomCountComparison;
                                        }

                                        return Double.compare((Double) dataSet1.getAttachment()
                                                        .get("averageDeviation"),
                                                        (Double) dataSet2.getAttachment()
                                                                        .get("averageDeviation"));
                                })
                                .collect(Collectors.toList());
        }

        private static void fineSearch(final Map<String, DataSet> fragmentsMap, final List<String> subDataSetStringList,
                        final Spectrum querySpectrum, final double shiftTolerance,
                        final double maximumAverageDeviation, final List<List<Integer>> hybridizationList,
                        final String mf) {
                DataSet fragment, fragmentTemp;
                Spectrum spectrum;
                Assignment spectralMatchAssignment;
                boolean isMatch;
                for (final String subDataSetString : subDataSetStringList) {
                        // fine search
                        fragment = gson.fromJson(subDataSetString, DataSet.class);
                        spectrum = fragment.getSpectrum()
                                        .toSpectrum();
                        if (spectrum.getSignalCount() > querySpectrum.getSignalCount()) {
                                continue;
                        }
                        spectralMatchAssignment = Similarity.matchSpectra(spectrum, querySpectrum, 0, 0, shiftTolerance,
                                        true, true,
                                        false);
                        isMatch = FragmentUtilities.isMatch(fragment, querySpectrum, mf,
                                        spectralMatchAssignment,
                                        maximumAverageDeviation, hybridizationList);
                        if (isMatch) {
                                String smiles;
                                try {
                                        smiles = SmilesGenerator.unique()
                                                        .create(fragment.getStructure()
                                                                        .toAtomContainer());
                                } catch (final CDKException e) {
                                        smiles = fragment.getMeta()
                                                        .get("smiles");
                                }
                                if (!fragmentsMap.containsKey(smiles)) {
                                        fragmentsMap.put(smiles, fragment);
                                } else {
                                        fragmentTemp = fragmentsMap.get(smiles);
                                        if ((double) fragment.getAttachment()
                                                        .get("averageDeviation") < (double) fragmentTemp.getAttachment()
                                                                        .get("averageDeviation")) {
                                                fragmentsMap.put(smiles, fragment);
                                        }
                                }
                        }
                }
        }

        public static Mono<Void> replaceAll(final Flux<DataSet> dataSetFlux, final String nucleus,
                        final CustomFragmentRepositoryImplementation customFragmentRepositoryImplementation,
                        final MultiplicitySectionsSettingsServiceImplementation multiplicitySectionsSettingsServiceImplementation) {
                return replaceAll(dataSetFlux, nucleus, customFragmentRepositoryImplementation,
                                multiplicitySectionsSettingsServiceImplementation, batchSize -> {
                                });
        }

        public static Mono<Void> replaceAll(final Flux<DataSet> dataSetFlux, final String nucleus,
                        final CustomFragmentRepositoryImplementation customFragmentRepositoryImplementation,
                        final MultiplicitySectionsSettingsServiceImplementation multiplicitySectionsSettingsServiceImplementation,
                        final IntConsumer onBatchProcessed) {
                return Mono.<Void>fromRunnable(() -> {
                        LOGGER.info("Deleting fragments table");
                        customFragmentRepositoryImplementation.dropTable(DatasetJpaConfig.FRAGMENT_TABLE_NAME);
                        LOGGER.info("Deleted fragments table");
                })
                                .subscribeOn(Schedulers.boundedElastic())
                                .then(multiplicitySectionsSettingsServiceImplementation.findByNucleus(nucleus).next())
                                .flatMap(multiplicitySectionsSettingsRecord -> buildFragments(dataSetFlux, nucleus,
                                                customFragmentRepositoryImplementation,
                                                multiplicitySectionsSettingsRecord, onBatchProcessed))
                                .switchIfEmpty(Mono.fromRunnable(() -> LOGGER.warn(
                                                "No multiplicity section settings found for nucleus {}; fragment rebuild skipped",
                                                nucleus)))
                                .doOnCancel(() -> LOGGER
                                                .warn("Fragment replaceAll pipeline cancelled for nucleus {}", nucleus))
                                .doOnError(error -> LOGGER
                                                .error("Failed to rebuild fragments for nucleus {}", nucleus, error))
                                .then();
        }

        private static Mono<Void> buildFragments(final Flux<DataSet> dataSetFlux, final String nucleus,
                        final CustomFragmentRepositoryImplementation customFragmentRepositoryImplementation,
                        final MultiplicitySectionsSettingsRecord multiplicitySectionsSettingsRecord,
                        final IntConsumer onBatchProcessed) {
                final int[] multiplicitySectionSettings = multiplicitySectionsSettingsRecord
                                .getMultiplicitySectionsSettings();
                final MultiplicitySectionsBuilder multiplicitySectionsBuilder = new MultiplicitySectionsBuilder();
                multiplicitySectionsBuilder.setMinLimit(multiplicitySectionSettings[0]);
                multiplicitySectionsBuilder.setMaxLimit(multiplicitySectionSettings[1]);
                multiplicitySectionsBuilder.setStepSize(multiplicitySectionSettings[2]);
                final int nBits = multiplicitySectionsBuilder.getSteps();

                LOGGER.info("Building and storing fragments for nucleus {} (steps={})", nucleus, nBits);

                // Shared counters so the trailing remainder (< PROGRESS_REPORT_EVERY) can be
                // flushed after the stream completes; otherwise the reported total would stop
                // at the last full batch (e.g. 36000 instead of 36123).
                final AtomicInteger processedCounter = new AtomicInteger(0);
                final AtomicInteger pendingReportCounter = new AtomicInteger(0);

                return Mono.<Void>fromRunnable(() -> customFragmentRepositoryImplementation.createFragmentsTable(nBits))
                                .subscribeOn(Schedulers.boundedElastic())
                                .thenMany(dataSetFlux)
                                .publishOn(Schedulers.boundedElastic())
                                .doOnNext(new java.util.function.Consumer<DataSet>() {
                                        @Override
                                        public void accept(final DataSet dataSet) {
                                                final List<DataSet> fragments = Fragmentation
                                                                .buildFragmentDataSets(dataSet, 3, 1, 6, true);
                                                if (fragments != null) {
                                                        fragments.stream()
                                                                        .filter(fragmentDataSet -> !Utils.isSaturated(
                                                                                        fragmentDataSet.getStructure()
                                                                                                        .toAtomContainer()))
                                                                        .forEach(fragmentDataSet -> {
                                                                                final BitSetFingerprint bitSetFingerprint = Similarity
                                                                                                .getBitSetFingerprint(
                                                                                                                fragmentDataSet
                                                                                                                                .getSpectrum()
                                                                                                                                .toSpectrum(),
                                                                                                                0,
                                                                                                                multiplicitySectionsBuilder);
                                                                                customFragmentRepositoryImplementation
                                                                                                .insertIntoTable(
                                                                                                                nucleus,
                                                                                                                BitUtilities.buildBitStringFromBigInteger(
                                                                                                                                BitUtilities.buildBits(
                                                                                                                                                bitSetFingerprint,
                                                                                                                                                nBits),
                                                                                                                                nBits),
                                                                                                                nBits,
                                                                                                                gson.toJson(fragmentDataSet,
                                                                                                                                DataSet.class));
                                                                        });
                                                }
                                                final int currentCount = processedCounter.incrementAndGet();
                                                if (currentCount % 1000 == 0) {
                                                        LOGGER.info("Fragments: processed {} datasets for nucleus {}",
                                                                        currentCount, nucleus);
                                                }
                                                if (pendingReportCounter.incrementAndGet() >= PROGRESS_REPORT_EVERY) {
                                                        final int reported = pendingReportCounter.getAndSet(0);
                                                        onBatchProcessed.accept(reported);
                                                }
                                        }
                                })
                                .then(Mono.<Void>fromRunnable(() -> {
                                        // Flush the trailing remainder so the reported total matches the
                                        // actual number of datasets processed (last partial batch).
                                        final int remaining = pendingReportCounter.getAndSet(0);
                                        if (remaining > 0) {
                                                onBatchProcessed.accept(remaining);
                                        }
                                        customFragmentRepositoryImplementation.createIndicesAndAnalyze();
                                        LOGGER.info("Fragments stored for nucleus {} ({} datasets processed)",
                                                        nucleus, processedCounter.get());
                                }).subscribeOn(Schedulers.boundedElastic()));
        }
}
