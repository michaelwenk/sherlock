/*
 * MIT License
 *
 * Copyright (c) 2020 Michael Wenk (https://github.com/michaelwenk)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.openscience.sherlock.dbservice.dataset.controller;

import casekit.nmr.analysis.MultiplicitySectionsBuilder;
import casekit.nmr.dbservice.COCONUT;
import casekit.nmr.dbservice.NMRShiftDB;
import casekit.nmr.model.DataSet;
import casekit.nmr.similarity.Similarity;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.BitSetFingerprint;
import org.openscience.sherlock.dbservice.dataset.SherlockDbServiceDatasetApplication;
import org.openscience.sherlock.dbservice.dataset.db.model.DataSetRecord;
import org.openscience.sherlock.dbservice.dataset.db.model.MultiplicitySectionsSettingsRecord;
import org.openscience.sherlock.dbservice.dataset.db.service.mongo.DataSetServiceImplementation;
import org.openscience.sherlock.dbservice.dataset.db.service.mongo.MultiplicitySectionsSettingsServiceImplementation;
import org.openscience.sherlock.dbservice.dataset.utils.SpectralUtilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping(value = "/dataset")
public class DataSetController {

        @Autowired
        private DataSetServiceImplementation dataSetServiceImplementation;
        @Autowired
        private MultiplicitySectionsSettingsServiceImplementation multiplicitySectionsSettingsServiceImplementation;

        @GetMapping(value = "/count")
        public Mono<Long> getCount() {
                return this.dataSetServiceImplementation.count();
        }

        @GetMapping(value = "/getByIds", produces = "application/json")
        public Flux<DataSetRecord> getByIds(@RequestParam final Iterable<String> ids) {
                return this.dataSetServiceImplementation.findAllById(ids);
        }

        @GetMapping(value = "/getById", produces = "application/json")
        public Mono<DataSetRecord> getById(@RequestParam final String id) {
                return this.dataSetServiceImplementation.findById(id);
        }

        @GetMapping(value = "/getAll", produces = "application/stream+json")
        public Flux<DataSetRecord> getAll() {
                return this.dataSetServiceImplementation.findAll();
        }

        @GetMapping(value = "/getByMf", produces = "application/stream+json")
        public Flux<DataSetRecord> getByMf(@RequestParam final String mf) {
                return this.dataSetServiceImplementation.findByMf(mf);
        }

        @GetMapping(value = "/getBySource", produces = "application/stream+json")
        public Flux<DataSetRecord> getBySource(@RequestParam final String source) {
                return this.dataSetServiceImplementation.findBySource(source);
        }

        @GetMapping(value = "/getByNuclei", produces = "application/stream+json")
        public Flux<DataSetRecord> getByDataSetSpectrumNuclei(@RequestParam final String[] nuclei) {
                return this.dataSetServiceImplementation.findByDataSetSpectrumNuclei(nuclei);
        }

        @GetMapping(value = "/getByNucleiAndSource", produces = "application/stream+json")
        public Flux<DataSetRecord> getByDataSetSpectrumNucleiAndSource(@RequestParam final String[] nuclei,
                        @RequestParam final String source) {
                return this.dataSetServiceImplementation.findByDataSetSpectrumNucleiAndSource(nuclei, source);
        }

        @GetMapping(value = "/getByNucleiAndSetBits", produces = "application/stream+json")
        public Flux<DataSetRecord> getByDataSetSpectrumNucleiAndAttachmentSetBits(@RequestParam final String[] nuclei,
                        @RequestParam final int[] setBits) {
                return this.dataSetServiceImplementation.findByDataSetSpectrumNucleiAndAttachmentSetBits(nuclei,
                                setBits);
        }

        @GetMapping(value = "/getByNucleiAndSetBitsAndMf", produces = "application/stream+json")
        public Flux<DataSetRecord> getByDataSetSpectrumNucleiAndAttachmentSetBitsAndMf(
                        @RequestParam final String[] nuclei,
                        @RequestParam final int[] setBits,
                        final String mf) {
                return this.dataSetServiceImplementation.findByDataSetSpectrumNucleiAndAttachmentSetBitsAndMf(nuclei,
                                setBits,
                                mf);
        }

        @GetMapping(value = "/getByNucleiAndSignalCount", produces = "application/stream+json")
        public Flux<DataSetRecord> getByDataSetSpectrumNucleiAndDataSetSpectrumSignalCount(
                        @RequestParam final String[] nuclei, @RequestParam final int signalCount) {
                return this.dataSetServiceImplementation.findByDataSetSpectrumNucleiAndDataSetSpectrumSignalCount(
                                nuclei,
                                signalCount);
        }

        @GetMapping(value = "/getByNucleiAndSignalCountAndMf", produces = "application/stream+json")
        public Flux<DataSetRecord> getByDataSetSpectrumNucleiAndDataSetSpectrumSignalCountAndMf(
                        @RequestParam final String[] nuclei, @RequestParam final int signalCount,
                        @RequestParam final String mf) {
                return this.dataSetServiceImplementation.findByDataSetSpectrumNucleiAndDataSetSpectrumSignalCountAndMf(
                                nuclei,
                                signalCount,
                                mf);
        }

        @PostMapping(value = "/insert", consumes = "application/json")
        public Mono<DataSetRecord> insert(@RequestBody final DataSetRecord dataSetRecord) {
                return this.dataSetServiceImplementation.insert(dataSetRecord);
        }

        @DeleteMapping(value = "/deleteAll")
        public Mono<Void> deleteAll() {
                return this.dataSetServiceImplementation.deleteAll();
        }

        @PostMapping(value = "/insertByDBNameAndFileIndex")
        public void insertByDBNameAndFileIndex(@RequestParam final String nucleus, @RequestParam final String dbName,
                        @RequestParam final int fileIndex, @RequestParam final int minShift,
                        @RequestParam final int maxShift) {

                // get multiplicity sections settings
                final int[] multiplicitySectionsSettings = this.multiplicitySectionsSettingsServiceImplementation
                                .findByNucleus(nucleus)
                                .blockFirst()
                                .getMultiplicitySectionsSettings();

                List<DataSet> dataSetList = new ArrayList<>();
                try {
                        if (dbName.equals("nmrshiftdb")) {
                                System.out.println(" -> datasets creation for \""
                                                + dbName
                                                + "\" ...");
                                dataSetList = NMRShiftDB.getDataSetsFromNMRShiftDB(
                                                SherlockDbServiceDatasetApplication.PATH_TO_NMRSHIFTDB,
                                                new String[] { nucleus });
                                dataSetList = SpectralUtilities.filterByShift(dataSetList, minShift, maxShift);
                        } else if (dbName.equals("coconut")) {
                                if (fileIndex >= SherlockDbServiceDatasetApplication.PATHS_TO_COCONUT.length) {
                                        System.out.println("!!! File index too large!!!");
                                } else {
                                        System.out.println(" -> datasets creation for \""
                                                        + dbName
                                                        + "\" and file index \""
                                                        + fileIndex
                                                        + "\" -> \""
                                                        + SherlockDbServiceDatasetApplication.PATHS_TO_COCONUT[fileIndex]
                                                        + "\" ...");
                                        dataSetList = COCONUT.getDataSetsWithShiftPredictionFromCOCONUT(
                                                        SherlockDbServiceDatasetApplication.PATHS_TO_COCONUT[fileIndex],
                                                        new String[] { nucleus });
                                        dataSetList = SpectralUtilities.filterByShift(dataSetList, minShift, maxShift);
                                }
                        }
                } catch (final IOException | CDKException e) {
                        e.printStackTrace();
                }
                System.out.println(" -> dataset size -> " + dataSetList.size());

                // set multiplicities based on protons count
                System.out.println(" -> setting multiplicities by protons count ...");
                SpectralUtilities.setMultiplicityByProtonsCount(dataSetList, nucleus);
                System.out.println(" -> setting multiplicities by protons count done.");

                System.out.println(" -> insert datasets ...");

                final AtomicInteger count = new AtomicInteger(0);
                final int dataSetListSize = dataSetList.size();
                for (final DataSet dataSet : dataSetList) {
                        final MultiplicitySectionsBuilder multiplicitySectionsBuilder = new MultiplicitySectionsBuilder();
                        multiplicitySectionsBuilder.setMinLimit(
                                        multiplicitySectionsSettings[0]);
                        multiplicitySectionsBuilder.setMaxLimit(
                                        multiplicitySectionsSettings[1]);
                        multiplicitySectionsBuilder.setStepSize(
                                        multiplicitySectionsSettings[2]);
                        final BitSetFingerprint bitSetFingerprint = Similarity.getBitSetFingerprint(
                                        dataSet.getSpectrum()
                                                        .toSpectrum(),
                                        0,
                                        multiplicitySectionsBuilder);

                        dataSet.addAttachment("fpSize", bitSetFingerprint.size());
                        dataSet.addAttachment("setBits",
                                        bitSetFingerprint.getSetbits());
                        this.dataSetServiceImplementation.insert(new DataSetRecord(null, dataSet))
                                        .doAfterTerminate(() -> {
                                                final int currentCount = count.incrementAndGet();
                                                if (currentCount % 10000 == 0) {
                                                        System.out.println(" --> inserted "
                                                                        + currentCount + " / " + dataSetListSize
                                                                        + " datasets");
                                                }
                                        })
                                        .block();
                }

                System.out.println(" --> inserted dataset list complete");
        }

        @GetMapping(value = "/getMultiplicitySectionsSettings", produces = "application/json")
        public Map<String, int[]> getMultiplicitySectionsSettings() {
                final Map<String, int[]> multiplicitySectionsSettingsMap = new HashMap<>();
                final List<MultiplicitySectionsSettingsRecord> multiplicitySectionsSettingsRecordList = this.multiplicitySectionsSettingsServiceImplementation
                                .findAll()
                                .collectList()
                                .block();
                if (multiplicitySectionsSettingsRecordList != null) {
                        for (final MultiplicitySectionsSettingsRecord multiplicitySectionsSettingsRecord : multiplicitySectionsSettingsRecordList) {
                                multiplicitySectionsSettingsMap.put(multiplicitySectionsSettingsRecord.getNucleus(),
                                                multiplicitySectionsSettingsRecord.getMultiplicitySectionsSettings());
                        }
                }

                return multiplicitySectionsSettingsMap;
        }

        @GetMapping(value = "/updateMultiplicitySectionsSettings", produces = "application/json")
        public void updateMultiplicitySectionsSettings(@RequestParam final String nucleus,
                        @RequestParam final int minShift,
                        @RequestParam final int maxShift, @RequestParam final int binSize) {

                // delete previously stored multiplicity sections settings
                this.multiplicitySectionsSettingsServiceImplementation
                                .deleteByNucleus(nucleus).block();

                // set new limits
                final int[] multiplicitySectionsSettings = new int[3];
                multiplicitySectionsSettings[0] = minShift
                                - binSize; // extend by one more step
                multiplicitySectionsSettings[1] = maxShift
                                // extend by one more step
                                + binSize;
                multiplicitySectionsSettings[2] = binSize;
                final MultiplicitySectionsSettingsRecord multiplicitySectionsSettingsRecord = new MultiplicitySectionsSettingsRecord(
                                null, nucleus, multiplicitySectionsSettings);
                this.multiplicitySectionsSettingsServiceImplementation.insert(
                                multiplicitySectionsSettingsRecord)
                                .block();
        }
}
