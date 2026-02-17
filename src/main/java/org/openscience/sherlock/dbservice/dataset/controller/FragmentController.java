package org.openscience.sherlock.dbservice.dataset.controller;

import java.util.ArrayList;
import org.openscience.cdk.fingerprint.BitSetFingerprint;
import org.openscience.sherlock.dbservice.dataset.db.model.MultiplicitySectionsSettingsRecord;
import org.openscience.sherlock.dbservice.dataset.db.service.jpa.CustomFragmentRepositoryImplementation;
import org.openscience.sherlock.dbservice.dataset.db.service.mongo.DataSetServiceImplementation;
import org.openscience.sherlock.dbservice.dataset.db.service.mongo.MultiplicitySectionsSettingsServiceImplementation;
import org.openscience.sherlock.dbservice.dataset.utils.FragmentControllerUtilities;
import org.openscience.sherlock.model.exchange.Transfer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import casekit.nmr.analysis.MultiplicitySectionsBuilder;
import casekit.nmr.model.DataSet;
import casekit.nmr.similarity.Similarity;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping(value = "/fragment")
public class FragmentController {

        @Autowired
        private DataSetServiceImplementation dataSetServiceImplementation;
        @Autowired
        private CustomFragmentRepositoryImplementation customFragmentRepositoryImplementation;
        @Autowired
        private MultiplicitySectionsSettingsServiceImplementation multiplicitySectionsSettingsServiceImplementation;

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

        @PostMapping(value = "/replaceAll")
        public void replaceAll(@RequestParam final String nucleus) {
                final Flux<DataSet> dataSetFlux = this.dataSetServiceImplementation
                                .findByDataSetSpectrumNuclei(new String[] {
                                                nucleus })
                                .map(dataSetRecord -> dataSetRecord.getDataSet());
                this.replaceAll(dataSetFlux, nucleus);
        }

        public void replaceAll(final Flux<DataSet> dataSetFlux, final String nucleus) {
                FragmentControllerUtilities.replaceAll(dataSetFlux, nucleus,
                                this.customFragmentRepositoryImplementation,
                                this.multiplicitySectionsSettingsServiceImplementation);
        }

}
