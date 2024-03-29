package org.openscience.sherlock.dbservice.dataset.controller;

import casekit.nmr.analysis.MultiplicitySectionsBuilder;
import casekit.nmr.fragments.FragmentUtilities;
import casekit.nmr.fragments.fragmentation.Fragmentation;
import casekit.nmr.model.Assignment;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import casekit.nmr.similarity.Similarity;
import casekit.nmr.utils.Utils;
import casekit.threading.MultiThreading;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.BitSetFingerprint;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.sherlock.dbservice.dataset.db.model.MultiplicitySectionsSettingsRecord;
import org.openscience.sherlock.dbservice.dataset.db.service.CustomFragmentRepositoryImplementation;
import org.openscience.sherlock.dbservice.dataset.db.service.DataSetServiceImplementation;
import org.openscience.sherlock.dbservice.dataset.db.service.MultiplicitySectionsSettingsServiceImplementation;
import org.openscience.sherlock.dbservice.dataset.model.exchange.Transfer;
import org.openscience.sherlock.dbservice.dataset.utils.BitUtilities;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/fragment")
public class FragmentController {

    private final Gson gson = new GsonBuilder().create();
    private final DataSetServiceImplementation dataSetServiceImplementation;
    private final CustomFragmentRepositoryImplementation customFragmentRepositoryImplementation;
    private final MultiplicitySectionsSettingsServiceImplementation multiplicitySectionsSettingsServiceImplementation;

    public FragmentController(final DataSetServiceImplementation dataSetServiceImplementation,
                              final CustomFragmentRepositoryImplementation customFragmentRepositoryImplementation,
                              final MultiplicitySectionsSettingsServiceImplementation multiplicitySectionsSettingsServiceImplementation) {
        this.dataSetServiceImplementation = dataSetServiceImplementation;
        this.customFragmentRepositoryImplementation = customFragmentRepositoryImplementation;
        this.multiplicitySectionsSettingsServiceImplementation = multiplicitySectionsSettingsServiceImplementation;
    }

    public List<DataSet> getFragments(final Spectrum querySpectrum, final BitSetFingerprint bitSetFingerprint,
                                      final int bitLength, final double shiftTolerance,
                                      final double maximumAverageDeviation, final String mf,
                                      final List<List<Integer>> hybridizationList, final int nThreads) {
        final BigInteger bigInteger = BitUtilities.buildBits(bitSetFingerprint, bitLength);
        //        final BigInteger flippedBigInteger = BitUtilities.flipBits(bigInteger, bitLength);
        //        final List<String> singleBitStringList = new ArrayList<>();
        //        for (int i = 0; i
        //                < bitLength; i++) {
        //            if (flippedBigInteger.testBit(i)) {
        //                singleBitStringList.add(SINGLE_BIT_STRINGS[i]);
        //            }
        //        }

        System.out.println(" -> query bits ("
                                   + bitLength
                                   + "): "
                                   + BitUtilities.buildBitsString(bigInteger, bitLength)
                                   + " -> with nThreads: "
                                   + nThreads);
        //        System.out.println(" -> query flip: "
        //                                   + BitUtilities.buildBitsString(flippedBigInteger, bitLength));


        // ###########################################################

        //        final List<Integer> resultIDs = this.customFragmentRepositoryImplementation.customFindByWITH(
        //                singleBitStringList);
        //        System.out.println(" -> roughly search size: "
        //                                   + resultIDs.size());
        //
        //        final List<String> subDataSetStringList = this.customFragmentRepositoryImplementation.customFindAllSubDataSetStringsById(
        //                resultIDs);

        final Map<String, DataSet> fragmentsMap = new ConcurrentHashMap<>();
        try {
            final List<Callable<Map<String, DataSet>>> callables = new ArrayList<>();
            final List<Integer> indices = new ArrayList<>();
            for (int i = 1; i
                    <= 50; i++) {
                indices.add(i);
            }
            final String setBitsString = "B'"
                    + BitUtilities.buildBitsString(bigInteger, bitLength)
                    + "'";

            for (final int i : indices) {
                final String tableName = "fragment_record_"
                        + i;
                callables.add(() -> {
                    final List<String> subDataSetStringList = this.customFragmentRepositoryImplementation.findBySetBits(
                            tableName, setBitsString);
                    this.fineSearch(fragmentsMap, subDataSetStringList, querySpectrum, shiftTolerance,
                                    maximumAverageDeviation, hybridizationList, mf);

                    return fragmentsMap;
                });
            }
            final Consumer<Map<String, DataSet>> consumer = (fragmentsMapTemp) -> {
            };
            MultiThreading.processTasks(callables, consumer, nThreads, 5);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        System.out.println(" --> fragments: "
                                   + fragmentsMap.size());
        // ###########################################################

        return fragmentsMap.values()
                           .stream()
                           .sorted((dataSet1, dataSet2) -> {
                               final int atomCountComparison = -1
                                       * Integer.compare(dataSet1.getStructure()
                                                                 .atomCount(), dataSet2.getStructure()
                                                                                       .atomCount());
                               if (atomCountComparison
                                       != 0) {
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
            if (spectrum.getSignalCount()
                    > querySpectrum.getSignalCount()) {
                continue;
            }
            spectralMatchAssignment = Similarity.matchSpectra(spectrum, querySpectrum, 0, 0, shiftTolerance, true, true,
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
                                         .get("averageDeviation")
                            < (double) fragmentTemp.getAttachment()
                                                   .get("averageDeviation")) {
                        fragmentsMap.put(smiles, fragment);
                    }
                }
            }
        }
    }

    @PostMapping(value = "/getBySpectrumAndMfAndSetBits", produces = "application/stream+json")
    public Flux<DataSet> getBySpectrumAndMfAndSetBits(@RequestBody final Transfer fragmentsDetectionTransfer) {
        final MultiplicitySectionsSettingsRecord multiplicitySectionsSettingsRecord = this.multiplicitySectionsSettingsServiceImplementation.findByNucleus(
                                                                                                  fragmentsDetectionTransfer.getQuerySpectrum()
                                                                                                                            .getNuclei()[0])
                                                                                                                                            .block();
        if (multiplicitySectionsSettingsRecord
                != null) {
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
                if (setBit
                        > multiplicitySectionsBuilder.getMinLimit()) {
                    bitSetFingerprint.set(setBit
                                                  - 1, true);
                }
                if (setBit
                        < multiplicitySectionsBuilder.getMaxLimit()) {
                    bitSetFingerprint.set(setBit
                                                  + 1, true);
                }
            }
            return Flux.fromIterable(this.getFragments(fragmentsDetectionTransfer.getQuerySpectrum(), bitSetFingerprint,
                                                       multiplicitySectionsBuilder.getSteps(),
                                                       fragmentsDetectionTransfer.getShiftTolerance(),
                                                       fragmentsDetectionTransfer.getMaximumAverageDeviation(),
                                                       fragmentsDetectionTransfer.getMf(),
                                                       fragmentsDetectionTransfer.getHybridizationList(),
                                                       fragmentsDetectionTransfer.getNThreads()));
        }

        return Flux.fromIterable(new ArrayList<>());
    }

    @PostMapping(value = "/replaceAll")
    public void replaceAll(@RequestParam final String[] nuclei, @RequestParam final String tableName) {
        System.out.println("-> deleting fragments in DB...");
        this.customFragmentRepositoryImplementation.dropTable(tableName);
        System.out.println("-> deleted fragments in DB");

        final Map<String, int[]> multiplicitySectionSettings = new HashMap<>();
        MultiplicitySectionsSettingsRecord multiplicitySectionsSettingsRecord;
        for (final String nucleus : nuclei) {
            multiplicitySectionsSettingsRecord = this.multiplicitySectionsSettingsServiceImplementation.findByNucleus(
                                                             nucleus)
                                                                                                       .block();
            multiplicitySectionSettings.put(multiplicitySectionsSettingsRecord.getNucleus(),
                                            multiplicitySectionsSettingsRecord.getMultiplicitySectionsSettings());
        }

        for (final String nucleus : nuclei) {
            System.out.println("-> build and store fragments in DB for nucleus: "
                                       + nucleus
                                       + " ...");

            if (multiplicitySectionSettings.containsKey(nucleus)) {
                final MultiplicitySectionsBuilder multiplicitySectionsBuilder = new MultiplicitySectionsBuilder();
                multiplicitySectionsBuilder.setMinLimit(multiplicitySectionSettings.get(nucleus)[0]);
                multiplicitySectionsBuilder.setMaxLimit(multiplicitySectionSettings.get(nucleus)[1]);
                multiplicitySectionsBuilder.setStepSize(multiplicitySectionSettings.get(nucleus)[2]);
                System.out.println(" -> "
                                           + nucleus
                                           + " -> steps: "
                                           + multiplicitySectionsBuilder.getSteps()
                                           + "\n");
                this.customFragmentRepositoryImplementation.createTable(tableName,
                                                                        multiplicitySectionsBuilder.getSteps());


                final String[] nucleiTemp = new String[]{nucleus};
                this.dataSetServiceImplementation.findByDataSetSpectrumNuclei(nucleiTemp)
                                                 .doOnNext(dataSetRecord -> {
                                                     final List<DataSet> fragments = Fragmentation.buildFragmentDataSets(
                                                             dataSetRecord.getDataSet(), 3, 1, 6, true);
                                                     if (fragments
                                                             != null) {
                                                         fragments.stream()
                                                                  .filter(fragmentDataSet -> !Utils.isSaturated(
                                                                          fragmentDataSet.getStructure()
                                                                                         .toAtomContainer()))
                                                                  .forEach(fragmentDataSet -> {
                                                                      final BitSetFingerprint bitSetFingerprint = Similarity.getBitSetFingerprint(
                                                                              fragmentDataSet.getSpectrum()
                                                                                             .toSpectrum(), 0,
                                                                              multiplicitySectionsBuilder);

                                                                      this.customFragmentRepositoryImplementation.insertIntoTable(
                                                                              tableName, nucleus,
                                                                              BitUtilities.buildBitsString(
                                                                                      BitUtilities.buildBits(
                                                                                              bitSetFingerprint,
                                                                                              multiplicitySectionsBuilder.getSteps()),
                                                                                      multiplicitySectionsBuilder.getSteps()),
                                                                              multiplicitySectionsBuilder.getSteps(),
                                                                              this.gson.toJson(fragmentDataSet,
                                                                                               DataSet.class));
                                                                  });
                                                     }
                                                 })
                                                 .doAfterTerminate(() -> System.out.println(
                                                         " -> fragments stored in DB for: "
                                                                 + nucleus))
                                                 .subscribe();
            }
        }
    }

    //    @PostMapping(value = "/filterByShift")
    //    public void filterByShift(@RequestParam final int minShift, @RequestParam final int maxShift) {
    //        final String nucleus = "13C";
    //        final MultiplicitySectionsSettingsRecord multiplicitySectionsSettingsRecord = this.multiplicitySectionsSettingsServiceImplementation.findByNucleus(
    //                                                                                                  nucleus)
    //                                                                                                                                            .block();
    //        if (multiplicitySectionsSettingsRecord
    //                != null) {
    //            String tableName;
    //            for (int i = 1; i
    //                    <= 50; i++) {
    //                tableName = "fragment_record_"
    //                        + i;
    //                final List<String> subDataSetStringList = this.customFragmentRepositoryImplementation.findByTableName(
    //                        tableName);
    //
    //                System.out.println(" --> i: "
    //                                           + i
    //                                           + " -> "
    //                                           + tableName
    //                                           + " -> "
    //                                           + subDataSetStringList.size());
    //
    //                tableName = tableName
    //                        + "x";
    //
    //                final MultiplicitySectionsBuilder multiplicitySectionsBuilder = new MultiplicitySectionsBuilder();
    //                multiplicitySectionsBuilder.setMinLimit(
    //                        multiplicitySectionsSettingsRecord.getMultiplicitySectionsSettings()[0]);
    //                multiplicitySectionsBuilder.setMaxLimit(
    //                        multiplicitySectionsSettingsRecord.getMultiplicitySectionsSettings()[1]);
    //                multiplicitySectionsBuilder.setStepSize(
    //                        multiplicitySectionsSettingsRecord.getMultiplicitySectionsSettings()[2]);
    //                this.customFragmentRepositoryImplementation.createTable(tableName,
    //                                                                        multiplicitySectionsBuilder.getSteps());
    //
    //                DataSet fragmentDataSet;
    //                BitSetFingerprint bitSetFingerprint;
    //                for (int k = 0; k
    //                        < subDataSetStringList.size(); k++) {
    //                    fragmentDataSet = this.gson.fromJson(subDataSetStringList.get(k), DataSet.class);
    //
    //                    if (ShiftUtilities.checkShifts(fragmentDataSet, minShift, maxShift)) {
    //                        bitSetFingerprint = Similarity.getBitSetFingerprint(fragmentDataSet.getSpectrum()
    //                                                                                           .toSpectrum(), 0,
    //                                                                            multiplicitySectionsBuilder);
    //
    //                        this.customFragmentRepositoryImplementation.insertIntoTable(tableName, nucleus,
    //                                                                                    BitUtilities.buildBitsString(
    //                                                                                            BitUtilities.buildBits(
    //                                                                                                    bitSetFingerprint,
    //                                                                                                    multiplicitySectionsBuilder.getSteps()),
    //                                                                                            multiplicitySectionsBuilder.getSteps()),
    //                                                                                    multiplicitySectionsBuilder.getSteps(),
    //                                                                                    this.gson.toJson(fragmentDataSet,
    //                                                                                                     DataSet.class));
    //
    //                    } else {
    //                        System.out.println(" -> fragment "
    //                                                   + k
    //                                                   + " is NOT valid! -> "
    //                                                   + Arrays.toString(fragmentDataSet.getSpectrum()
    //                                                                                    .getSignals()));
    //                    }
    //                }
    //            }
    //        }
    //    }
    //
    //    @PostMapping(value = "/replace")
    //    public void replace() {
    //        String tableName;
    //        for (int i = 1; i
    //                <= 50; i++) {
    //            tableName = "fragment_record_"
    //                    + i;
    //            this.customFragmentRepositoryImplementation.dropTable(tableName);
    //            this.customFragmentRepositoryImplementation.renameTable(tableName
    //                                                                            + "x", tableName);
    //        }
    //    }
}
