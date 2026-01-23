package org.openscience.sherlock.dbservice.dataset.controller;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.BitSetFingerprint;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.sherlock.dbservice.dataset.config.DatasetJpaConfig;
import org.openscience.sherlock.dbservice.dataset.db.model.MultiplicitySectionsSettingsRecord;
import org.openscience.sherlock.dbservice.dataset.db.service.jpa.CustomFragmentRepositoryImplementation;
import org.openscience.sherlock.dbservice.dataset.db.service.mongo.DataSetServiceImplementation;
import org.openscience.sherlock.dbservice.dataset.db.service.mongo.MultiplicitySectionsSettingsServiceImplementation;
import org.openscience.sherlock.dbservice.dataset.utils.BitUtilities;
import org.openscience.sherlock.model.exchange.Transfer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

@RestController
@RequestMapping(value = "/fragment")
public class FragmentController {

        private final Gson gson = new GsonBuilder().create();
        @Autowired
        private DataSetServiceImplementation dataSetServiceImplementation;
        @Autowired
        private CustomFragmentRepositoryImplementation customFragmentRepositoryImplementation;
        @Autowired
        private MultiplicitySectionsSettingsServiceImplementation multiplicitySectionsSettingsServiceImplementation;

        public List<DataSet> getFragments(final Spectrum querySpectrum, final BitSetFingerprint bitSetFingerprint,
                        final int bitLength, final double shiftTolerance,
                        final double maximumAverageDeviation, final String mf,
                        final List<List<Integer>> hybridizationList) {
                final BigInteger bigInteger = BitUtilities.buildBits(bitSetFingerprint, bitLength);

                System.out.println(" -> query: nucleus: " + querySpectrum.getNuclei()[0] + ", bits ("
                                + bitLength
                                + "): "
                                + BitUtilities.buildBitStringFromBigInteger(bigInteger, bitLength));

                final String bitString = BitUtilities.buildBitStringFromBigInteger(bigInteger, bitLength);
                final List<String> subDataSetStringList = this.customFragmentRepositoryImplementation
                                .findBySetBits(querySpectrum.getNuclei()[0], bitString);
                final Map<String, DataSet> fragmentsMap = new HashMap<>();
                this.fineSearch(fragmentsMap, subDataSetStringList, querySpectrum,
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

        private void fineSearch(final Map<String, DataSet> fragmentsMap, final List<String> subDataSetStringList,
                        final Spectrum querySpectrum, final double shiftTolerance,
                        final double maximumAverageDeviation, final List<List<Integer>> hybridizationList,
                        final String mf) {
                DataSet fragment, fragmentTemp;
                Spectrum spectrum;
                Assignment spectralMatchAssignment;
                boolean isMatch;
                for (final String subDataSetString : subDataSetStringList) {
                        // fine search
                        fragment = this.gson.fromJson(subDataSetString, DataSet.class);
                        spectrum = fragment.getSpectrum()
                                        .toSpectrum();
                        if (spectrum.getSignalCount() > querySpectrum.getSignalCount()) {
                                continue;
                        }
                        spectralMatchAssignment = Similarity.matchSpectra(spectrum, querySpectrum, 0, 0, shiftTolerance,
                                        true, true,
                                        false);
                        isMatch = FragmentUtilities.isMatch(fragment, querySpectrum, mf, spectralMatchAssignment,
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
                        return Flux.fromIterable(this.getFragments(fragmentsDetectionTransfer.getQuerySpectrum(),
                                        bitSetFingerprint,
                                        multiplicitySectionsBuilder.getSteps(),
                                        fragmentsDetectionTransfer.getShiftTolerance(),
                                        fragmentsDetectionTransfer.getMaximumAverageDeviation(),
                                        fragmentsDetectionTransfer.getMf(),
                                        fragmentsDetectionTransfer.getHybridizationList()));
                }

                return Flux.fromIterable(new ArrayList<>());
        }

        @PostMapping(value = "/replaceAll")
        public void replaceAll(@RequestParam final String nucleus) {
                final Flux<DataSet> dataSetFlux = this.dataSetServiceImplementation
                                .findByDataSetSpectrumNuclei(new String[] {
                                                nucleus })
                                .map(dataSetRecord -> dataSetRecord.getDataSet());
                this.replaceAll(dataSetFlux, nucleus);
        }

        public void replaceAll(final Flux<DataSet> dataSetFlux, final String nucleus) {
                System.out.println("-> deleting fragments in DB...");
                this.customFragmentRepositoryImplementation.dropTable(DatasetJpaConfig.BITS_TABLE_NAME);
                this.customFragmentRepositoryImplementation.dropTable(DatasetJpaConfig.FRAGMENT_TABLE_NAME);
                System.out.println("-> deleted fragments in DB");

                final MultiplicitySectionsSettingsRecord multiplicitySectionsSettingsRecord = this.multiplicitySectionsSettingsServiceImplementation
                                .findByNucleus(
                                                nucleus)
                                .blockFirst();
                if (multiplicitySectionsSettingsRecord == null) {
                        System.out.println("-> no multiplicity section settings found for nucleus: " + nucleus);
                        return;
                }
                final int[] multiplicitySectionSettings = multiplicitySectionsSettingsRecord
                                .getMultiplicitySectionsSettings();
                System.out.println("-> build and store fragments in DB for nucleus: "
                                + nucleus
                                + " ...");

                final MultiplicitySectionsBuilder multiplicitySectionsBuilder = new MultiplicitySectionsBuilder();
                multiplicitySectionsBuilder.setMinLimit(multiplicitySectionSettings[0]);
                multiplicitySectionsBuilder.setMaxLimit(multiplicitySectionSettings[1]);
                multiplicitySectionsBuilder.setStepSize(multiplicitySectionSettings[2]);
                System.out.println(" -> steps: "
                                + multiplicitySectionsBuilder.getSteps()
                                + "\n");
                this.customFragmentRepositoryImplementation.createFragmentsTable();
                this.customFragmentRepositoryImplementation.createBitsTable(
                                multiplicitySectionsBuilder.getSteps());

                AtomicInteger counter = new AtomicInteger(0);
                dataSetFlux.doOnNext(dataSet -> {
                        final List<DataSet> fragments = Fragmentation
                                        .buildFragmentDataSets(
                                                        dataSet, 3,
                                                        1, 6, true);
                        if (fragments != null) {
                                fragments.stream()
                                                .filter(fragmentDataSet -> !Utils
                                                                .isSaturated(
                                                                                fragmentDataSet.getStructure()
                                                                                                .toAtomContainer()))
                                                .forEach(fragmentDataSet -> {
                                                        final BitSetFingerprint bitSetFingerprint = Similarity
                                                                        .getBitSetFingerprint(
                                                                                        fragmentDataSet.getSpectrum()
                                                                                                        .toSpectrum(),
                                                                                        0,
                                                                                        multiplicitySectionsBuilder);

                                                        this.customFragmentRepositoryImplementation
                                                                        .insertIntoTable(
                                                                                        nucleus,
                                                                                        BitUtilities.buildBitStringFromBigInteger(
                                                                                                        BitUtilities.buildBits(
                                                                                                                        bitSetFingerprint,
                                                                                                                        multiplicitySectionsBuilder
                                                                                                                                        .getSteps()),
                                                                                                        multiplicitySectionsBuilder
                                                                                                                        .getSteps()),
                                                                                        multiplicitySectionsBuilder
                                                                                                        .getSteps(),
                                                                                        this.gson.toJson(
                                                                                                        fragmentDataSet,
                                                                                                        DataSet.class));
                                                });
                        }
                        if (counter.incrementAndGet() % 1000 == 0) {
                                System.out.println(" -> processed datasets: "
                                                + counter.get());
                        }
                }).doAfterTerminate(() -> {
                        this.customFragmentRepositoryImplementation
                                        .createIndices(
                                                        multiplicitySectionsBuilder
                                                                        .getSteps());
                        System.out.println(
                                        " -> fragments stored in DB for: "
                                                        + nucleus);
                        System.out.println(
                                        "--------------------------------------------------\n");
                }).subscribe();
        }

}
