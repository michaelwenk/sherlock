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
import org.openscience.sherlock.configuration.OpenApiConfiguration;
import org.openscience.sherlock.dbservice.dataset.db.model.DataSetRecord;
import org.openscience.sherlock.dbservice.dataset.db.model.MultiplicitySectionsSettingsRecord;
import org.openscience.sherlock.dbservice.dataset.db.service.mongo.DataSetServiceImplementation;
import org.openscience.sherlock.dbservice.dataset.db.service.mongo.MultiplicitySectionsSettingsServiceImplementation;
import org.openscience.sherlock.dbservice.dataset.utils.SpectralUtilities;
import org.openscience.sherlock.utils.Utilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Datasets", description = "Endpoints for querying, importing, and maintaining dataset records and multiplicity settings.")
@SecurityRequirement(name = OpenApiConfiguration.BASIC_AUTH_SCHEME)
@RestController
@RequestMapping(value = "/dataset")
public class DataSetController {

        @Autowired
        private DataSetServiceImplementation dataSetServiceImplementation;
        @Autowired
        private MultiplicitySectionsSettingsServiceImplementation multiplicitySectionsSettingsServiceImplementation;

        @Operation(summary = "Count datasets", description = "Returns the number of dataset records currently stored in the dataset service.")
        @GetMapping(value = "/count")
        public Mono<Long> getCount() {
                return this.dataSetServiceImplementation.count();
        }

        @Operation(summary = "Get datasets by IDs", description = "Returns all dataset records whose IDs match the provided collection of identifiers.")
        @GetMapping(value = "/getByIds", produces = "application/json")
        public Flux<DataSetRecord> getByIds(@RequestParam final Iterable<String> ids) {
                return this.dataSetServiceImplementation.findAllById(ids);
        }

        @Operation(summary = "Get a dataset by ID", description = "Returns a single dataset record for the provided dataset identifier.")
        @GetMapping(value = "/getById", produces = "application/json")
        public Mono<DataSetRecord> getById(@RequestParam final String id) {
                return this.dataSetServiceImplementation.findById(id);
        }

        @Operation(summary = "List all datasets", description = "Streams every dataset record stored in the dataset service.")
        @GetMapping(value = "/getAll", produces = "application/stream+json")
        public Flux<DataSetRecord> getAll() {
                return this.dataSetServiceImplementation.findAll();
        }

        @Operation(summary = "Find datasets by molecular formula", description = "Streams dataset records whose molecular formula matches the provided value.")
        @GetMapping(value = "/getByMf", produces = "application/stream+json")
        public Flux<DataSetRecord> getByMf(@RequestParam final String mf) {
                return this.dataSetServiceImplementation.findByMf(mf);
        }

        @Operation(summary = "Find datasets by source", description = "Streams dataset records originating from the selected source database.")
        @GetMapping(value = "/getBySource", produces = "application/stream+json")
        public Flux<DataSetRecord> getBySource(@RequestParam final String source) {
                return this.dataSetServiceImplementation.findBySource(source);
        }

        @Operation(summary = "Find datasets by nuclei", description = "Streams dataset records whose spectra use the provided nuclei.")
        @GetMapping(value = "/getByNuclei", produces = "application/stream+json")
        public Flux<DataSetRecord> getByDataSetSpectrumNuclei(@RequestParam final String[] nuclei) {
                return this.dataSetServiceImplementation.findByDataSetSpectrumNuclei(nuclei);
        }

        @Operation(summary = "Find datasets by nuclei and source", description = "Streams dataset records filtered by both spectrum nuclei and source database.")
        @GetMapping(value = "/getByNucleiAndSource", produces = "application/stream+json")
        public Flux<DataSetRecord> getByDataSetSpectrumNucleiAndSource(@RequestParam final String[] nuclei,
                        @RequestParam final String source) {
                return this.dataSetServiceImplementation.findByDataSetSpectrumNucleiAndSource(nuclei, source);
        }

        @Operation(summary = "Find datasets by nuclei and set bits", description = "Streams dataset records whose spectra and attachment fingerprints match the provided nuclei and set bit values.")
        @GetMapping(value = "/getByNucleiAndSetBits", produces = "application/stream+json")
        public Flux<DataSetRecord> getByDataSetSpectrumNucleiAndAttachmentSetBits(@RequestParam final String[] nuclei,
                        @RequestParam final int[] setBits) {
                return this.dataSetServiceImplementation.findByDataSetSpectrumNucleiAndAttachmentSetBits(nuclei,
                                setBits);
        }

        @Operation(summary = "Find datasets by nuclei, set bits, and molecular formula", description = "Streams dataset records filtered by nuclei, attachment fingerprint set bits, and molecular formula.")
        @GetMapping(value = "/getByNucleiAndSetBitsAndMf", produces = "application/stream+json")
        public Flux<DataSetRecord> getByDataSetSpectrumNucleiAndAttachmentSetBitsAndMf(
                        @RequestParam final String[] nuclei,
                        @RequestParam final int[] setBits,
                        final String mf) {
                return this.dataSetServiceImplementation.findByDataSetSpectrumNucleiAndAttachmentSetBitsAndMf(nuclei,
                                setBits,
                                mf);
        }

        @Operation(summary = "Find datasets by nuclei and signal count", description = "Streams dataset records filtered by nuclei and the number of spectrum signals.")
        @GetMapping(value = "/getByNucleiAndSignalCount", produces = "application/stream+json")
        public Flux<DataSetRecord> getByDataSetSpectrumNucleiAndDataSetSpectrumSignalCount(
                        @RequestParam final String[] nuclei, @RequestParam final int signalCount) {
                return this.dataSetServiceImplementation.findByDataSetSpectrumNucleiAndDataSetSpectrumSignalCount(
                                nuclei,
                                signalCount);
        }

        @Operation(summary = "Find datasets by nuclei, signal count, and molecular formula", description = "Streams dataset records filtered by nuclei, signal count, and molecular formula.")
        @GetMapping(value = "/getByNucleiAndSignalCountAndMf", produces = "application/stream+json")
        public Flux<DataSetRecord> getByDataSetSpectrumNucleiAndDataSetSpectrumSignalCountAndMf(
                        @RequestParam final String[] nuclei, @RequestParam final int signalCount,
                        @RequestParam final String mf) {
                return this.dataSetServiceImplementation.findByDataSetSpectrumNucleiAndDataSetSpectrumSignalCountAndMf(
                                nuclei,
                                signalCount,
                                mf);
        }

        @Operation(summary = "Insert a dataset", description = "Stores a new dataset record in the dataset service.")
        @PostMapping(value = "/insert", consumes = "application/json")
        public Mono<DataSetRecord> insert(@RequestBody final DataSetRecord dataSetRecord) {
                return this.dataSetServiceImplementation.insert(dataSetRecord);
        }

        @Operation(summary = "Delete all datasets", description = "Removes every dataset record from the dataset service.")
        @DeleteMapping(value = "/deleteAll")
        public Mono<Void> deleteAll() {
                return this.dataSetServiceImplementation.deleteAll();
        }

        @Operation(summary = "Import datasets from a source database", description = "Loads SD files from the selected directory, applies spectral preprocessing, and inserts the generated dataset records.")
        @PostMapping(value = "/insertByDirPathAndDbName")
        public void insertByDirPathAndDbName(@RequestParam final String pathToDir, @RequestParam final String dbName,
                        @RequestParam final String nucleus, @RequestParam final int minShift,
                        @RequestParam final int maxShift) {

                final List<Path> sdfFiles = Utilities.collectFiles(pathToDir, "sdf");

                if (sdfFiles.isEmpty()) {
                        System.out.println("!!! No SDF files found in the specified directory: " + pathToDir);
                        return;
                }

                // get multiplicity sections settings
                final int[] multiplicitySectionsSettings = this.multiplicitySectionsSettingsServiceImplementation
                                .findByNucleus(nucleus)
                                .blockFirst()
                                .getMultiplicitySectionsSettings();

                List<DataSet> dataSetList = new ArrayList<>();
                for (final Path sdfFile : sdfFiles) {
                        System.out.println(" -> processing SDF file: " + sdfFile.toAbsolutePath().toString());

                        try {
                                if (dbName.equals("nmrshiftdb")) {
                                        System.out.println(" -> datasets creation for \""
                                                        + dbName + "\"...");
                                        dataSetList = NMRShiftDB.getDataSetsFromNMRShiftDB(
                                                        sdfFile.toAbsolutePath().toString(),
                                                        new String[] { nucleus });
                                } else if (dbName.equals("acd_labs_predictions")) {
                                        System.out.println(" -> datasets creation for \""
                                                        + dbName + "\"...");
                                        dataSetList = COCONUT.getDataSetsWithShiftPredictionFromCOCONUT(
                                                        sdfFile.toAbsolutePath()
                                                                        .toString(),
                                                        new String[] { nucleus });

                                }
                                dataSetList = SpectralUtilities.filterByShift(dataSetList, minShift,
                                                maxShift);

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
                                                                                        + currentCount + " / "
                                                                                        + dataSetListSize
                                                                                        + " datasets");
                                                                }
                                                        })
                                                        .block();
                                }

                                System.out.println(
                                                " -> processing SDF file done: " + sdfFile.toAbsolutePath().toString());
                        } catch (final IOException | CDKException e) {
                                e.printStackTrace();
                        }
                }

                System.out.println(" --> inserted dataset list complete");
        }

        @Operation(summary = "Get multiplicity section settings", description = "Returns the configured multiplicity section settings per nucleus.")
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

        @Operation(summary = "Update multiplicity section settings", description = "Replaces the stored multiplicity section settings for the selected nucleus.")
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

        @Operation(summary = "Update dataset indexes", description = "Triggers index creation or refresh for the dataset collection.")
        @PostMapping("/updateIndexes")
        public void updateIndexes() {
                this.dataSetServiceImplementation.updateIndexes().block();
        }
}
