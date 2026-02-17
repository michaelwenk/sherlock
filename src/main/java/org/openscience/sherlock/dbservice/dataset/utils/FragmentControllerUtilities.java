package org.openscience.sherlock.dbservice.dataset.utils;

import java.math.BigInteger;
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
import org.openscience.sherlock.dbservice.dataset.db.service.mongo.MultiplicitySectionsSettingsServiceImplementation;

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

public class FragmentControllerUtilities {

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

        public static void replaceAll(final Flux<DataSet> dataSetFlux, final String nucleus,
                        final CustomFragmentRepositoryImplementation customFragmentRepositoryImplementation,
                        final MultiplicitySectionsSettingsServiceImplementation multiplicitySectionsSettingsServiceImplementation) {
                System.out.println("-> deleting fragments in DB...");
                customFragmentRepositoryImplementation.dropTable(DatasetJpaConfig.FRAGMENT_TABLE_NAME);
                System.out.println("-> deleted fragments in DB");

                final MultiplicitySectionsSettingsRecord multiplicitySectionsSettingsRecord = multiplicitySectionsSettingsServiceImplementation
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

                final int nBits = multiplicitySectionsBuilder.getSteps();
                System.out.println(" -> steps: "
                                + nBits
                                + "\n");
                customFragmentRepositoryImplementation.createFragmentsTable(nBits);

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

                                                        customFragmentRepositoryImplementation
                                                                        .insertIntoTable(
                                                                                        nucleus,
                                                                                        BitUtilities.buildBitStringFromBigInteger(
                                                                                                        BitUtilities.buildBits(
                                                                                                                        bitSetFingerprint,
                                                                                                                        nBits),
                                                                                                        nBits),
                                                                                        nBits,
                                                                                        gson.toJson(
                                                                                                        fragmentDataSet,
                                                                                                        DataSet.class));
                                                });
                        }
                        if (counter.incrementAndGet() % 1000 == 0) {
                                System.out.println(" -> processed datasets: "
                                                + counter.get());
                        }
                }).doAfterTerminate(() -> {
                        customFragmentRepositoryImplementation
                                        .createIndicesAndAnalyze();
                        System.out.println(
                                        " -> fragments stored in DB for: "
                                                        + nucleus);
                        System.out.println(
                                        "--------------------------------------------------\n");
                }).subscribe();
        }

}
