package org.openscience.sherlock.dbservice.dataset.controller;

import casekit.nmr.analysis.MultiplicitySectionsBuilder;
import casekit.nmr.fragments.FragmentUtilities;
import casekit.nmr.fragments.functionalgroup.ErtlFunctionalGroupsUtilities;
import casekit.nmr.model.Assignment;
import casekit.nmr.model.DataSet;
import casekit.nmr.similarity.Similarity;
import org.openscience.cdk.fingerprint.BitSetFingerprint;
import org.openscience.sherlock.dbservice.dataset.db.model.FunctionalGroupRecord;
import org.openscience.sherlock.dbservice.dataset.db.model.MultiplicitySectionsSettingsRecord;
import org.openscience.sherlock.dbservice.dataset.db.service.DataSetServiceImplementation;
import org.openscience.sherlock.dbservice.dataset.db.service.FunctionalGroupServiceImplementation;
import org.openscience.sherlock.dbservice.dataset.db.service.MultiplicitySectionsSettingsServiceImplementation;
import org.openscience.sherlock.dbservice.dataset.model.exchange.Transfer;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping(value = "/functionalGroup")
public class FunctionalGroupController {

    private final FunctionalGroupServiceImplementation functionalGroupServiceImplementation;
    private final DataSetServiceImplementation dataSetServiceImplementation;
    private final MultiplicitySectionsSettingsServiceImplementation multiplicitySectionsSettingsServiceImplementation;

    public FunctionalGroupController(final FunctionalGroupServiceImplementation functionalGroupServiceImplementation,
                                     final DataSetServiceImplementation dataSetServiceImplementation,
                                     final MultiplicitySectionsSettingsServiceImplementation multiplicitySectionsSettingsServiceImplementation) {
        this.functionalGroupServiceImplementation = functionalGroupServiceImplementation;
        this.dataSetServiceImplementation = dataSetServiceImplementation;
        this.multiplicitySectionsSettingsServiceImplementation = multiplicitySectionsSettingsServiceImplementation;
    }

    @GetMapping(value = "/count")
    public Mono<Long> getCount() {
        return this.functionalGroupServiceImplementation.count();
    }

    @GetMapping(value = "/getById", produces = "application/json")
    public Mono<FunctionalGroupRecord> getById(@RequestParam final String id) {
        return this.functionalGroupServiceImplementation.findById(id);
    }

    @GetMapping(value = "/getAll", produces = "application/stream+json")
    public Flux<FunctionalGroupRecord> getAll() {
        return this.functionalGroupServiceImplementation.findAll();
    }

    @GetMapping(value = "/getByNuclei", produces = "application/stream+json")
    public Flux<FunctionalGroupRecord> getByDataSetSpectrumNuclei(@RequestParam final String[] nuclei) {
        return this.functionalGroupServiceImplementation.findByDataSetSpectrumNuclei(nuclei);
    }

    @PostMapping(value = "/insert", consumes = "application/json")
    public Mono<FunctionalGroupRecord> insert(@RequestBody final FunctionalGroupRecord functionalGroupRecord) {
        return this.functionalGroupServiceImplementation.insert(functionalGroupRecord);
    }

    @DeleteMapping(value = "/deleteAll")
    public Mono<Void> deleteAll() {
        return this.functionalGroupServiceImplementation.deleteAll();
    }

    @PostMapping(value = "/replaceAll")
    public void replaceAll(@RequestParam final String[] nuclei) {
        System.out.println("-> deleting functional groups in DB...");
        this.deleteAll()
            .block();


        System.out.println("-> deleted functional groups in DB");
        for (final String nucleus : nuclei) {
            System.out.println("-> build and store functional groups in DB for nucleus: "
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
                                                     final List<DataSet> dataSetList = new ArrayList<>();
                                                     dataSetList.add(dataSetRecord.getDataSet());
                                                     final Stream<FunctionalGroupRecord> functionalGroupRecordStream = ErtlFunctionalGroupsUtilities.buildFunctionalGroupDataSets(
                                                                                                                                                            dataSetList, nucleiTemp)
                                                                                                                                                    .stream()
                                                                                                                                                    .map(functionalGroupDataSet -> {
                                                                                                                                                        final BitSetFingerprint bitSetFingerprint = Similarity.getBitSetFingerprint(
                                                                                                                                                                functionalGroupDataSet.getSpectrum()
                                                                                                                                                                                      .toSpectrum(),
                                                                                                                                                                0,
                                                                                                                                                                multiplicitySectionsBuilder);
                                                                                                                                                        functionalGroupDataSet.addAttachment(
                                                                                                                                                                "fpSize",
                                                                                                                                                                bitSetFingerprint.size());
                                                                                                                                                        functionalGroupDataSet.addAttachment(
                                                                                                                                                                "setBits",
                                                                                                                                                                bitSetFingerprint.getSetbits());

                                                                                                                                                        return new FunctionalGroupRecord(
                                                                                                                                                                null,
                                                                                                                                                                functionalGroupDataSet);
                                                                                                                                                    });
                                                     this.functionalGroupServiceImplementation.insertMany(
                                                                 Flux.fromStream(functionalGroupRecordStream))
                                                                                              .subscribe();
                                                 })
                                                 .doAfterTerminate(() -> System.out.println(
                                                         "-> functional groups stored in DB for: "
                                                                 + nucleus))
                                                 .subscribe();
            }
        }
    }

    @GetMapping(value = "/getByDataSetByNucleiAndSignalCountAndSetBits", produces = "application/stream+json")
    public Flux<FunctionalGroupRecord> getByDataSetByNucleiAndSignalCountAndSetBits(@RequestParam final String[] nuclei,
                                                                                    @RequestParam final int signalCount,
                                                                                    @RequestParam final int[] bits) {

        return this.functionalGroupServiceImplementation.getByDataSetByNucleiAndSignalCountAndSetBits(nuclei,
                                                                                                      signalCount,
                                                                                                      bits);
    }

    @PostMapping(value = "/getBySpectrumAndMfAndSetBits", produces = "application/stream+json")
    public Flux<DataSet> getBySpectrumAndMfAndSetBits(@RequestBody final Transfer functionalGroupDetectionTransfer) {
        final MultiplicitySectionsSettingsRecord multiplicitySectionsSettingsRecord = this.multiplicitySectionsSettingsServiceImplementation.findByNucleus(
                                                                                                  functionalGroupDetectionTransfer.getQuerySpectrum()
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
                    functionalGroupDetectionTransfer.getQuerySpectrum(), 0, multiplicitySectionsBuilder);
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
            //            final Set<String> functionalGroupIDs = new HashSet<>();
            //            for (final int setBit : bitSetFingerprint.getSetbits()) {
            //                System.out.println(setBit);
            //                this.functionalGroupLookupServiceImplementation.findBySetBit(setBit)
            //                                                               .doOnNext(
            //                                                                       functionalGroupLookupRecord -> functionalGroupIDs.addAll(
            //                                                                               functionalGroupLookupRecord.getIds()))
            //                                                               .collectList()
            //                                                               .block();
            //                System.out.println(" -> ids total: "
            //                                           + functionalGroupIDs.size());
            //            }

            return this.getByDataSetByNucleiAndSignalCountAndSetBits(functionalGroupDetectionTransfer.getQuerySpectrum()
                                                                                                     .getNuclei(),
                                                                     functionalGroupDetectionTransfer.getQuerySpectrum()
                                                                                                     .getSignalCount(),
                                                                     bitSetFingerprint.getSetbits())
                       .map(FunctionalGroupRecord::getDataSet)
                       .filter(dataSet -> {
                           // fine search
                           final Assignment matchAssignment = Similarity.matchSpectra(dataSet.getSpectrum()
                                                                                             .toSpectrum(),
                                                                                      functionalGroupDetectionTransfer.getQuerySpectrum(),
                                                                                      0, 0,
                                                                                      functionalGroupDetectionTransfer.getShiftTol(),
                                                                                      functionalGroupDetectionTransfer.isCheckMultiplicity(),
                                                                                      true, true);
                           return FragmentUtilities.isMatch(dataSet,
                                                            functionalGroupDetectionTransfer.getQuerySpectrum(),
                                                            functionalGroupDetectionTransfer.getMf(), matchAssignment,
                                                            functionalGroupDetectionTransfer.getMaximumAverageDeviation(),
                                                            functionalGroupDetectionTransfer.getHybridizationList());
                       });
        }

        return Flux.fromIterable(new ArrayList<>());
    }
}
