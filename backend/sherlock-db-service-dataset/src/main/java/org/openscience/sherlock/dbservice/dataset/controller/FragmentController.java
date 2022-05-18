package org.openscience.sherlock.dbservice.dataset.controller;

import casekit.nmr.analysis.MultiplicitySectionsBuilder;
import casekit.nmr.fragments.FragmentUtilities;
import casekit.nmr.fragments.fragmentation.Fragmentation;
import casekit.nmr.model.Assignment;
import casekit.nmr.model.DataSet;
import casekit.nmr.similarity.Similarity;
import org.openscience.cdk.fingerprint.BitSetFingerprint;
import org.openscience.sherlock.dbservice.dataset.db.model.FragmentRecord;
import org.openscience.sherlock.dbservice.dataset.db.model.MultiplicitySectionsSettingsRecord;
import org.openscience.sherlock.dbservice.dataset.db.service.DataSetServiceImplementation;
import org.openscience.sherlock.dbservice.dataset.db.service.FragmentServiceImplementation;
import org.openscience.sherlock.dbservice.dataset.db.service.MultiplicitySectionsSettingsServiceImplementation;
import org.openscience.sherlock.dbservice.dataset.model.exchange.Transfer;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping(value = "/fragment")
public class FragmentController {

    private final FragmentServiceImplementation fragmentServiceImplementation;
    private final DataSetServiceImplementation dataSetServiceImplementation;
    private final MultiplicitySectionsSettingsServiceImplementation multiplicitySectionsSettingsServiceImplementation;

    public FragmentController(final FragmentServiceImplementation fragmentServiceImplementation,
                              final DataSetServiceImplementation dataSetServiceImplementation,
                              final MultiplicitySectionsSettingsServiceImplementation multiplicitySectionsSettingsServiceImplementation) {
        this.fragmentServiceImplementation = fragmentServiceImplementation;
        this.dataSetServiceImplementation = dataSetServiceImplementation;
        this.multiplicitySectionsSettingsServiceImplementation = multiplicitySectionsSettingsServiceImplementation;
    }

    @GetMapping(value = "/count")
    public Mono<Long> getCount() {
        return this.fragmentServiceImplementation.count();
    }

    @GetMapping(value = "/getById", produces = "application/json")
    public Mono<FragmentRecord> getById(@RequestParam final String id) {
        return this.fragmentServiceImplementation.findById(id);
    }

    @GetMapping(value = "/getAll", produces = "application/stream+json")
    public Flux<FragmentRecord> getAll() {
        return this.fragmentServiceImplementation.findAll();
    }

    @GetMapping(value = "/getByNuclei", produces = "application/stream+json")
    public Flux<FragmentRecord> getByDataSetSpectrumNuclei(@RequestParam final String[] nuclei) {
        return this.fragmentServiceImplementation.findByDataSetSpectrumNuclei(nuclei);
    }

    @PostMapping(value = "/insert", consumes = "application/json")
    public Mono<FragmentRecord> insert(@RequestBody final FragmentRecord functionalGroupRecord) {
        return this.fragmentServiceImplementation.insert(functionalGroupRecord);
    }

    @DeleteMapping(value = "/deleteAll")
    public Mono<Void> deleteAll() {
        return this.fragmentServiceImplementation.deleteAll();
    }

    @PostMapping(value = "/replaceAll")
    public void replaceAll(@RequestParam final String[] nuclei) {
        System.out.println("-> deleting fragments in DB...");
        this.deleteAll()
            .block();


        System.out.println("-> deleted fragments in DB");
        for (final String nucleus : nuclei) {
            System.out.println("-> build and store fragments in DB for nucleus: "
                                       + nucleus
                                       + " ...");
            final MultiplicitySectionsSettingsRecord multiplicitySectionsSettingsRecord = this.multiplicitySectionsSettingsServiceImplementation.findByNucleus(
                                                                                                      nucleus)
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
                final String[] nucleiTemp = new String[]{nucleus};
                this.dataSetServiceImplementation.findByDataSetSpectrumNuclei(nucleiTemp)
                                                 .doOnNext(dataSetRecord -> {
                                                     final List<DataSet> fragments = Fragmentation.buildFragmentDataSets(
                                                             dataSetRecord.getDataSet(), 3, 1, 6, false);
                                                     if (fragments
                                                             != null) {
                                                         final Stream<FragmentRecord> fragmentRecordStream = fragments.stream()
                                                                                                                      .map(fragmentDataSet -> {
                                                                                                                          final BitSetFingerprint bitSetFingerprint = Similarity.getBitSetFingerprint(
                                                                                                                                  fragmentDataSet.getSpectrum()
                                                                                                                                                 .toSpectrum(),
                                                                                                                                  0,
                                                                                                                                  multiplicitySectionsBuilder);
                                                                                                                          fragmentDataSet.addAttachment(
                                                                                                                                  "fpSize",
                                                                                                                                  bitSetFingerprint.size());
                                                                                                                          fragmentDataSet.addAttachment(
                                                                                                                                  "setBits",
                                                                                                                                  bitSetFingerprint.getSetbits());

                                                                                                                          return new FragmentRecord(
                                                                                                                                  null,
                                                                                                                                  fragmentDataSet);
                                                                                                                      });
                                                         this.fragmentServiceImplementation.insertMany(
                                                                     Flux.fromStream(fragmentRecordStream))
                                                                                           .subscribe();
                                                     }
                                                 })
                                                 .doAfterTerminate(() -> System.out.println(
                                                         "-> fragments stored in DB for: "
                                                                 + nucleus))
                                                 .subscribe();
            }
        }
    }

    @GetMapping(value = "/getByDataSetByNucleiAndSignalCountAndSetBits", produces = "application/stream+json")
    public Flux<FragmentRecord> getByDataSetByNucleiAndSignalCountAndSetBits(@RequestParam final String[] nuclei,
                                                                             @RequestParam final int signalCount,
                                                                             @RequestParam final int[] bits) {

        return this.fragmentServiceImplementation.getByDataSetByNucleiAndSignalCountAndSetBits(nuclei, signalCount,
                                                                                               bits);
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

            return this.getByDataSetByNucleiAndSignalCountAndSetBits(fragmentsDetectionTransfer.getQuerySpectrum()
                                                                                               .getNuclei(),
                                                                     fragmentsDetectionTransfer.getQuerySpectrum()
                                                                                               .getSignalCount(),
                                                                     bitSetFingerprint.getSetbits())
                       .map(FragmentRecord::getDataSet)
                       .filter(dataSet -> {
                           // fine search
                           final Assignment matchAssignment = Similarity.matchSpectra(dataSet.getSpectrum()
                                                                                             .toSpectrum(),
                                                                                      fragmentsDetectionTransfer.getQuerySpectrum(),
                                                                                      0, 0,
                                                                                      fragmentsDetectionTransfer.getShiftTol(),
                                                                                      fragmentsDetectionTransfer.isCheckMultiplicity(),
                                                                                      true, true);
                           return FragmentUtilities.isMatch(dataSet, fragmentsDetectionTransfer.getQuerySpectrum(),
                                                            fragmentsDetectionTransfer.getMf(), matchAssignment,
                                                            fragmentsDetectionTransfer.getMaximumAverageDeviation(),
                                                            fragmentsDetectionTransfer.getHybridizationList());
                       });
        }

        return Flux.fromIterable(new ArrayList<>());
    }
}
