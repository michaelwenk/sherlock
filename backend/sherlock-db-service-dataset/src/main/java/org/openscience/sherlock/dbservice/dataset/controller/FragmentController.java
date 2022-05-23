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
import org.openscience.sherlock.dbservice.dataset.db.model.FragmentLookupRecord;
import org.openscience.sherlock.dbservice.dataset.db.model.FragmentRecord;
import org.openscience.sherlock.dbservice.dataset.db.model.MultiplicitySectionsSettingsRecord;
import org.openscience.sherlock.dbservice.dataset.db.service.DataSetServiceImplementation;
import org.openscience.sherlock.dbservice.dataset.db.service.FragmentLookupRepository;
import org.openscience.sherlock.dbservice.dataset.db.service.FragmentRepository;
import org.openscience.sherlock.dbservice.dataset.db.service.MultiplicitySectionsSettingsServiceImplementation;
import org.openscience.sherlock.dbservice.dataset.model.exchange.Transfer;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/fragment")
public class FragmentController {

    private final Gson gson = new GsonBuilder().create();
    private final FragmentRepository fragmentRepository;
    private final FragmentLookupRepository fragmentLookupRepository;
    private final DataSetServiceImplementation dataSetServiceImplementation;
    private final MultiplicitySectionsSettingsServiceImplementation multiplicitySectionsSettingsServiceImplementation;

    public FragmentController(final FragmentRepository fragmentRepository,
                              final FragmentLookupRepository fragmentLookupRepository,
                              final DataSetServiceImplementation dataSetServiceImplementation,
                              final MultiplicitySectionsSettingsServiceImplementation multiplicitySectionsSettingsServiceImplementation) {
        this.fragmentRepository = fragmentRepository;
        this.fragmentLookupRepository = fragmentLookupRepository;
        this.dataSetServiceImplementation = dataSetServiceImplementation;
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

    @GetMapping(value = "/getByNucleusAndSignalCountAndSetBits")
    public List<DataSet> getByNucleusAndSignalCountAndSetBits(@RequestParam final String nucleus,
                                                              @RequestParam final int signalCount,
                                                              @RequestParam final int[] setBits) {
        final String setBitsString = Arrays.toString(setBits)
                                           .replaceAll("\\[", "{")
                                           .replaceAll("\\]", "}");
        return this.fragmentRepository.findByNucleusAndSignalCountAndSetBits(nucleus, signalCount, setBitsString)
                                      .stream()
                                      .map(fragment -> this.gson.fromJson(fragment.getSubDataSetString(),
                                                                          DataSet.class))
                                      .collect(Collectors.toList());
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
                final String[] nucleiTemp = new String[]{nucleus};
                this.dataSetServiceImplementation.findByDataSetSpectrumNuclei(nucleiTemp)
                                                 .doOnNext(dataSetRecord -> {
                                                     final List<DataSet> fragments = Fragmentation.buildFragmentDataSets(
                                                             dataSetRecord.getDataSet(), 3, 1, 6, true);
                                                     if (fragments
                                                             != null) {
                                                         final List<FragmentRecord> fragmentList = fragments.stream()
                                                                                                            .filter(fragmentDataSet -> !Utils.isSaturated(
                                                                                                                    fragmentDataSet.getStructure()
                                                                                                                                   .toAtomContainer()))
                                                                                                            .map(fragmentDataSet -> {
                                                                                                                final FragmentRecord fragmentRecord = new FragmentRecord();
                                                                                                                fragmentRecord.setMf(
                                                                                                                        fragmentDataSet.getMeta()
                                                                                                                                       .get("mf"));
                                                                                                                fragmentRecord.setSmiles(
                                                                                                                        fragmentDataSet.getMeta()
                                                                                                                                       .get("smiles"));
                                                                                                                fragmentRecord.setNucleus(
                                                                                                                        nucleus);
                                                                                                                fragmentRecord.setSignalCount(
                                                                                                                        fragmentDataSet.getSpectrum()
                                                                                                                                       .getSignals().length);

                                                                                                                final BitSetFingerprint bitSetFingerprint = Similarity.getBitSetFingerprint(
                                                                                                                        fragmentDataSet.getSpectrum()
                                                                                                                                       .toSpectrum(),
                                                                                                                        0,
                                                                                                                        multiplicitySectionsBuilder);
                                                                                                                fragmentRecord.setSetBits(
                                                                                                                        bitSetFingerprint.getSetbits());
                                                                                                                fragmentRecord.setFpSize(
                                                                                                                        bitSetFingerprint.size());


                                                                                                                fragmentRecord.setSubDataSetString(
                                                                                                                        this.gson.toJson(
                                                                                                                                fragmentDataSet,
                                                                                                                                DataSet.class));

                                                                                                                return fragmentRecord;
                                                                                                            })
                                                                                                            .collect(
                                                                                                                    Collectors.toList());

                                                         this.fragmentRepository.saveAll(fragmentList);
                                                     }
                                                 })
                                                 .doAfterTerminate(() -> System.out.println(
                                                         " -> fragments stored in DB for: "
                                                                 + nucleus))
                                                 .subscribe();
            }
        }
    }

    @PostMapping(value = "/getBySpectrumAndMfAndSetBits", produces = "application/stream+json")
    public Flux<DataSet> getBySpectrumAndMfAndSetBits(@RequestBody final Transfer fragmentsDetectionTransfer) {
        List<DataSet> fragmentList = new ArrayList<>();
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

            final Map<String, DataSet> fragmentsMap = new HashMap<>();
            //            this.getByNucleusAndSignalCountAndSetBits(fragmentsDetectionTransfer.getQuerySpectrum()
            //                                                                                .getNuclei()[0],
            //                                                      fragmentsDetectionTransfer.getQuerySpectrum()
            //                                                                                .getSignalCount(),
            //                                                      bitSetFingerprint.getSetbits())
            this.lookupBySetBits(bitSetFingerprint.getSetbits())
                .stream()
                .map(fragmentRecord -> this.gson.fromJson(fragmentRecord.getSubDataSetString(), DataSet.class))
                .forEach(fragment -> {
                    // fine search
                    final Spectrum spectrum = fragment.getSpectrum()
                                                      .toSpectrum();
                    final Assignment spectralMatchAssignment = Similarity.matchSpectra(spectrum,
                                                                                       fragmentsDetectionTransfer.getQuerySpectrum(),
                                                                                       0, 0,
                                                                                       fragmentsDetectionTransfer.getShiftTol(),
                                                                                       true, true, true);

                    final boolean isMatch = FragmentUtilities.isMatch(fragment,
                                                                      fragmentsDetectionTransfer.getQuerySpectrum(),
                                                                      fragmentsDetectionTransfer.getMf(),
                                                                      spectralMatchAssignment,
                                                                      fragmentsDetectionTransfer.getMaximumAverageDeviation(),
                                                                      fragmentsDetectionTransfer.getHybridizationList());
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
                            final DataSet fragmentTemp = fragmentsMap.get(smiles);
                            if ((double) fragment.getAttachment()
                                                 .get("averageDeviation")
                                    < (double) fragmentTemp.getAttachment()
                                                           .get("averageDeviation")) {
                                fragmentsMap.put(smiles, fragment);
                            }
                        }
                    }
                });
            fragmentList = new ArrayList<>(fragmentsMap.values()).stream()
                                                                 .sorted((dataSet1, dataSet2) -> {
                                                                     final int atomCountComparison = -1
                                                                             * Integer.compare(dataSet1.getStructure()
                                                                                                       .atomCount(),
                                                                                               dataSet2.getStructure()
                                                                                                       .atomCount());
                                                                     if (atomCountComparison
                                                                             != 0) {
                                                                         return atomCountComparison;
                                                                     }

                                                                     return Double.compare(
                                                                             (Double) dataSet1.getAttachment()
                                                                                              .get("averageDeviation"),
                                                                             (Double) dataSet2.getAttachment()
                                                                                              .get("averageDeviation"));
                                                                 })
                                                                 .collect(Collectors.toList());
        }

        return Flux.fromIterable(fragmentList);
    }

    @PostMapping(value = "/buildLookup")
    public void buildLookup() {
        System.out.println(" -> delete lookup...");
        this.fragmentLookupRepository.deleteAll();
        System.out.println(" -> delete lookup done");
        System.out.println(" -> build lookup...");
        final Map<String, List<Long>> lookupMap = new HashMap<>();
        this.fragmentRepository.findAllSetBits()
                               .forEach(fragmentRecordLight -> {
                                   final String setBitsString = Arrays.toString(fragmentRecordLight.getSetBits());
                                   lookupMap.putIfAbsent(setBitsString, new ArrayList<>());
                                   lookupMap.get(setBitsString)
                                            .add(fragmentRecordLight.getId());
                               });
        System.out.println(" -> build lookup done");
        System.out.println(" -> store lookup...");
        FragmentLookupRecord fragmentLookupRecord;
        String[] split;
        int[] setBits;
        for (final Map.Entry<String, List<Long>> entry : lookupMap.entrySet()) {
            split = entry.getKey()
                         .replaceAll("\\[|\\]|\\s", "")
                         .split(",");
            setBits = new int[split.length];
            for (int i = 0; i
                    < split.length; i++) {
                setBits[i] = Integer.parseInt(split[i]);
            }
            fragmentLookupRecord = new FragmentLookupRecord();

            fragmentLookupRecord.setSetBits(setBits);
            final long[] ids = new long[entry.getValue()
                                             .size()];
            for (int i = 0; i
                    < entry.getValue()
                           .size(); i++) {
                ids[i] = entry.getValue()
                              .get(i);
            }
            fragmentLookupRecord.setIds(ids);
            this.fragmentLookupRepository.save(fragmentLookupRecord);
        }
        System.out.println(" -> store lookup done");
    }

    @GetMapping(value = "/lookupCount")
    public long getLookupCount() {
        return this.fragmentLookupRepository.count();
    }

    @GetMapping(value = "/lookupBySetBits")
    public List<FragmentRecord> lookupBySetBits(@RequestParam final int[] setBits) {
        final String setBitsString = Arrays.toString(setBits)
                                           .replaceAll("\\[", "{")
                                           .replaceAll("\\]", "}");
        final List<FragmentRecord> fragmentRecordList = new ArrayList<>();
        final List<FragmentLookupRecord> fragmentLookupRecordList = this.fragmentLookupRepository.findByNucleusAndSetBits(
                setBitsString);
        fragmentLookupRecordList.forEach(fragmentLookupRecordLight -> {
            for (final long id : fragmentLookupRecordLight.getIds()) {
                final Optional<FragmentRecord> fragmentRecordOptional = this.fragmentRepository.findById(id);
                fragmentRecordOptional.ifPresent(fragmentRecordList::add);
            }
        });

        return fragmentRecordList;
    }
}
