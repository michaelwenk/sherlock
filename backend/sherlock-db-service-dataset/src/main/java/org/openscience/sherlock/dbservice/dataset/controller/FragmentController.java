package org.openscience.sherlock.dbservice.dataset.controller;

import casekit.nmr.analysis.MultiplicitySectionsBuilder;
import casekit.nmr.fragments.FragmentUtilities;
import casekit.nmr.fragments.fragmentation.Fragmentation;
import casekit.nmr.model.Assignment;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import casekit.nmr.similarity.Similarity;
import casekit.nmr.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.BitSetFingerprint;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.sherlock.dbservice.dataset.db.model.FragmentRecord;
import org.openscience.sherlock.dbservice.dataset.db.model.MultiplicitySectionsSettingsRecord;
import org.openscience.sherlock.dbservice.dataset.db.service.CustomFragmentRepositoryImplementation;
import org.openscience.sherlock.dbservice.dataset.db.service.DataSetServiceImplementation;
import org.openscience.sherlock.dbservice.dataset.db.service.FragmentRepository;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/fragment")
public class FragmentController {

    public final static String[] SINGLE_BIT_STRINGS = BitUtilities.buildSingleBitBigIntegerStrings(209);

    private final Gson gson = new GsonBuilder().create();
    private final FragmentRepository fragmentRepository;
    private final DataSetServiceImplementation dataSetServiceImplementation;
    private final CustomFragmentRepositoryImplementation customFragmentRepositoryImplementation;
    private final MultiplicitySectionsSettingsServiceImplementation multiplicitySectionsSettingsServiceImplementation;

    public FragmentController(final FragmentRepository fragmentRepository,
                              final DataSetServiceImplementation dataSetServiceImplementation,
                              final CustomFragmentRepositoryImplementation customFragmentRepositoryImplementation,
                              final MultiplicitySectionsSettingsServiceImplementation multiplicitySectionsSettingsServiceImplementation) {
        this.fragmentRepository = fragmentRepository;
        this.dataSetServiceImplementation = dataSetServiceImplementation;
        this.customFragmentRepositoryImplementation = customFragmentRepositoryImplementation;
        this.multiplicitySectionsSettingsServiceImplementation = multiplicitySectionsSettingsServiceImplementation;
    }

    @GetMapping(value = "/count")
    public long getCount() {
        return this.fragmentRepository.count();
    }

    @GetMapping(value = "/getAll")
    public List<FragmentRecord> getAll() {
        return this.fragmentRepository.findAll();
    }

    public List<DataSet> getFragments(final Spectrum querySpectrum, final BitSetFingerprint bitSetFingerprint,
                                      final int bitLength, final double shiftTolerance,
                                      final double maximumAverageDeviation, final String mf,
                                      final List<List<Integer>> hybridizationList) {
        final BigInteger bigInteger = BitUtilities.buildBits(bitSetFingerprint, bitLength);
        //        final BigInteger flippedBigInteger = BitUtilities.flipBits(bigInteger, bitLength);
        //        final List<String> singleBitStringList = new ArrayList<>();
        //        for (int i = 0; i
        //                < bitLength; i++) {
        //            if (flippedBigInteger.testBit(i)) {
        //                singleBitStringList.add(SINGLE_BIT_STRINGS[i]);
        //            }
        //        }

        System.out.println(" -> query bits: "
                                   + BitUtilities.buildBitsString(bigInteger, bitLength));
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

        final List<String> subDataSetStringList = this.customFragmentRepositoryImplementation.findBySetBits("B'"
                                                                                                                    + BitUtilities.buildBitsString(
                bigInteger, bitLength)
                                                                                                                    + "'");
        // ###########################################################


        System.out.println(" -> subDataSetStringList size: "
                                   + subDataSetStringList.size());

        final Map<String, DataSet> fragmentsMap = new HashMap<>();
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
                                                              true);
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
        System.out.println(" -> fine search size: "
                                   + fragmentsMap.size());
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
            // also set neighbor bits to give more flexibility in request
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
                                                       fragmentsDetectionTransfer.getHybridizationList()));
        }

        return Flux.fromIterable(new ArrayList<>());
    }

    @PostMapping(value = "/replaceAll")
    public void replaceAll(@RequestParam final String[] nuclei) {
        System.out.println("-> deleting fragments in DB...");
        this.fragmentRepository.deleteAll();
        System.out.println("-> deleted fragments in DB");

        final Map<String, Integer[]> multiplicitySectionSettings = new HashMap<>();
        multiplicitySectionSettings.put("13C", new Integer[]{-123, 296, 2});

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

                                                                      this.fragmentRepository.insertFragmentRecord(
                                                                              nucleus, fragmentDataSet.getSpectrum()
                                                                                                      .getSignals().length,
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

    @PostMapping(value = "/createIndexStrings")
    public void createIndexString() {
        final StringBuilder stringBuilder = new StringBuilder();
        final int bitLength = 209;
        BigInteger bigInteger;
        String bigIntegerString;
        for (int i = 0; i
                < bitLength; i++) {
            bigInteger = BitUtilities.buildBits(i, bitLength);

            bigIntegerString = "B'"
                    + BitUtilities.buildBitsString(bigInteger, bitLength)
                    + "'";
            stringBuilder.append("CREATE INDEX set_bits_index_")
                         .append(i)
                         .append("_not_equal ON fragment_record(id) WHERE set_bits & ")
                         .append(bigIntegerString)
                         .append(" != ")
                         .append(bigIntegerString)
                         .append(";")
                         .append("\n");
        }

        System.out.println(stringBuilder);
    }
}
