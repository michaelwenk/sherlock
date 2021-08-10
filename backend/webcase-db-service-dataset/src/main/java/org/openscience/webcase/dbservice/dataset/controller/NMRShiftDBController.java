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

package org.openscience.webcase.dbservice.dataset.controller;

import casekit.nmr.analysis.MultiplicitySectionsBuilder;
import casekit.nmr.dbservice.NMRShiftDB;
import casekit.nmr.model.DataSet;
import casekit.nmr.similarity.Similarity;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.BitSetFingerprint;
import org.openscience.webcase.dbservice.dataset.nmrshiftdb.model.BitSetRecord;
import org.openscience.webcase.dbservice.dataset.nmrshiftdb.model.DataSetRecord;
import org.openscience.webcase.dbservice.dataset.nmrshiftdb.model.MultiplicitySectionsSettingsRecord;
import org.openscience.webcase.dbservice.dataset.nmrshiftdb.service.BitSetServiceImplementation;
import org.openscience.webcase.dbservice.dataset.nmrshiftdb.service.DataSetServiceImplementation;
import org.openscience.webcase.dbservice.dataset.nmrshiftdb.service.MultiplicitySectionsSettingsServiceImplementation;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping(value = "/nmrshiftdb")
public class NMRShiftDBController {

    private final DataSetServiceImplementation dataSetServiceImplementation;
    private final BitSetServiceImplementation bitSetServiceImplementation;
    private final MultiplicitySectionsSettingsServiceImplementation multiplicitySectionsSettingsServiceImplementation;

    private final String pathToNMRShiftDB = "/data/nmrshiftdb/nmrshiftdb2withsignals.sd";
    private final MultiplicitySectionsBuilder multiplicitySectionsBuilder = new MultiplicitySectionsBuilder();
    private final Map<String, int[]> multiplicitySectionsSettings = new HashMap<>();

    public NMRShiftDBController(final DataSetServiceImplementation dataSetServiceImplementation,
                                final BitSetServiceImplementation bitSetServiceImplementation,
                                final MultiplicitySectionsSettingsServiceImplementation multiplicitySectionsSettingsServiceImplementation) {
        this.dataSetServiceImplementation = dataSetServiceImplementation;
        this.bitSetServiceImplementation = bitSetServiceImplementation;
        this.multiplicitySectionsSettingsServiceImplementation = multiplicitySectionsSettingsServiceImplementation;
    }

    @GetMapping(value = "/count")
    public Mono<Long> getCount() {
        return this.dataSetServiceImplementation.count();
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

    @GetMapping(value = "/getByNuclei", produces = "application/stream+json")
    public Flux<DataSetRecord> getByDataSetSpectrumNuclei(@RequestParam final String[] nuclei) {
        return this.dataSetServiceImplementation.findByDataSetSpectrumNuclei(nuclei);
    }

    @GetMapping(value = "/getByNucleiAndSignalCount", produces = "application/stream+json")
    public Flux<DataSetRecord> getByDataSetSpectrumNucleiAndDataSetSpectrumSignalCount(
            @RequestParam final String[] nuclei, @RequestParam final int signalCount) {
        return this.dataSetServiceImplementation.findByDataSetSpectrumNucleiAndDataSetSpectrumSignalCount(nuclei,
                                                                                                          signalCount);
    }

    @GetMapping(value = "/getByNucleiAndSignalCountAndMf", produces = "application/stream+json")
    public Flux<DataSetRecord> getByDataSetSpectrumNucleiAndDataSetSpectrumSignalCountAndMf(
            @RequestParam final String[] nuclei, @RequestParam final int signalCount, @RequestParam final String mf) {
        return this.dataSetServiceImplementation.findByDataSetSpectrumNucleiAndDataSetSpectrumSignalCountAndMf(nuclei,
                                                                                                               signalCount,
                                                                                                               mf);
    }

    @PostMapping(value = "/insert", consumes = "application/json")
    public Mono<DataSetRecord> insert(@RequestBody final DataSetRecord dataSetRecord) {
        return this.dataSetServiceImplementation.insert(dataSetRecord);
    }

    @DeleteMapping(value = "/deleteAll")
    public void deleteAll() {
        this.dataSetServiceImplementation.deleteAll()
                                         .block();
    }

    @PostMapping(value = "/replaceAll")
    public void replaceAll(@RequestParam final String[] nuclei) {
        this.deleteAll();
        this.bitSetServiceImplementation.deleteAll()
                                        .block();

        List<DataSet> dataSetList = new ArrayList<>();
        try {
            dataSetList = NMRShiftDB.getDataSetsFromNMRShiftDB(this.pathToNMRShiftDB, nuclei);
        } catch (final FileNotFoundException | CDKException e) {
            e.printStackTrace();
        }

        this.setMinLimitAndMaxLimitOfMultiplicitySectionsBuilder(dataSetList);

        final Map<String, Map<String, List<String>>> dataSetRecordIDsPerNucleusAndSetBits = new HashMap<>(); // per nucleus(outer key) a map of: set bits string as (inner) key and object array as value: dataset record id, nucleus, fingerprint
        DataSetRecord insertedDataSetRecord;
        String nucleus, setBitsString;
        for (final DataSet dataSet : dataSetList) {
            insertedDataSetRecord = this.insert(new DataSetRecord(null, dataSet))
                                        .block();
            nucleus = insertedDataSetRecord.getDataSet()
                                           .getSpectrum()
                                           .getNuclei()[0];
            this.multiplicitySectionsBuilder.setMinLimit(this.multiplicitySectionsSettings.get(nucleus)[0]);
            this.multiplicitySectionsBuilder.setMaxLimit(this.multiplicitySectionsSettings.get(nucleus)[1]);
            this.multiplicitySectionsBuilder.setStepSize(this.multiplicitySectionsSettings.get(nucleus)[2]);
            final BitSetFingerprint bitSetFingerprint = Similarity.getBitSetFingerprint(
                    insertedDataSetRecord.getDataSet()
                                         .getSpectrum(), 0, this.multiplicitySectionsBuilder);
            setBitsString = Arrays.toString(bitSetFingerprint.getSetbits());

            dataSetRecordIDsPerNucleusAndSetBits.putIfAbsent(nucleus, new HashMap<>());

            dataSetRecordIDsPerNucleusAndSetBits.get(nucleus)
                                                .putIfAbsent(setBitsString, new ArrayList<>());
            dataSetRecordIDsPerNucleusAndSetBits.get(nucleus)
                                                .get(setBitsString)
                                                .add(insertedDataSetRecord.getId());
        }
        String key;
        String[] split;
        int[] setBits;
        long fingerprintSize;
        for (final Map.Entry<String, Map<String, List<String>>> entryNucleus : dataSetRecordIDsPerNucleusAndSetBits.entrySet()) {
            nucleus = entryNucleus.getKey();
            for (final Map.Entry<String, List<String>> entrySetBits : entryNucleus.getValue()
                                                                                  .entrySet()) {
                key = entrySetBits.getKey();
                key = key.replaceFirst("\\[", "");
                key = key.replaceFirst("\\]", "");
                split = key.split(",");
                setBits = new int[split.length];
                for (int i = 0; i
                        < split.length; i++) {
                    setBits[i] = Integer.parseInt(split[i].trim());
                }
                fingerprintSize = this.multiplicitySectionsBuilder.calculateSteps(
                        this.multiplicitySectionsSettings.get(nucleus)[0],
                        this.multiplicitySectionsSettings.get(nucleus)[1],
                        this.multiplicitySectionsSettings.get(nucleus)[2]);

                this.bitSetServiceImplementation.insert(
                        new BitSetRecord(null, nucleus, fingerprintSize, setBits, setBits.length,
                                         entrySetBits.getValue()))
                                                .block();
            }
        }
    }

    @GetMapping(value = "/getBySetBits", produces = "application/json")
    public List<DataSetRecord> getBySetBits(@RequestParam final String nucleus,
                                            @RequestParam final long fingerprintSize,
                                            @RequestParam final int[] setBits) {
        final List<DataSetRecord> dataSetRecordList = new ArrayList<>();
        final List<BitSetRecord> bitSetRecordList = this.bitSetServiceImplementation.findBitSetRecordByNucleusAndFingerprintSizeAndSetBits(
                nucleus, fingerprintSize, setBits)
                                                                                    .collectList()
                                                                                    .block();
        List<DataSetRecord> dataSetRecordListTemp;
        if (bitSetRecordList
                != null) {
            for (final BitSetRecord bitSetRecord : bitSetRecordList) {
                dataSetRecordListTemp = this.dataSetServiceImplementation.findAllById(
                        bitSetRecord.getDataSetRecordIDs())
                                                                         .collectList()
                                                                         .block();
                if (dataSetRecordListTemp
                        != null) {
                    dataSetRecordList.addAll(dataSetRecordListTemp);
                }
            }
        }

        return dataSetRecordList;
    }

    @GetMapping(value = "/getByMfAndSetBits", produces = "application/json")
    public List<DataSetRecord> getByMfAndSetBits(@RequestParam final String mf, @RequestParam final String nucleus,
                                                 @RequestParam final long fingerprintSize,
                                                 @RequestParam final int[] setBits) {
        final List<DataSetRecord> dataSetRecordList = this.getByMf(mf)
                                                          .collectList()
                                                          .block();
        if (dataSetRecordList
                != null) {
            final List<DataSetRecord> dataSetRecordListSetBits = this.getBySetBits(nucleus, fingerprintSize, setBits);
            if (dataSetRecordListSetBits
                    != null) {
                dataSetRecordList.removeAll(dataSetRecordList.stream()
                                                             .filter(dataSetRecord -> dataSetRecordListSetBits.stream()
                                                                                                              .noneMatch(
                                                                                                                      dataSetRecordSetBits -> dataSetRecordSetBits.getId()
                                                                                                                                                                  .equals(dataSetRecord.getId())))
                                                             .collect(Collectors.toList()));
            }
        }

        return dataSetRecordList;
    }

    @GetMapping(value = "/getBySetBitsCount", produces = "application/stream+json")
    public List<String> getBySetBitsCount(@RequestParam final String nucleus, @RequestParam final long fingerprintSize,
                                          @RequestParam final int setBitsCount) {
        final List<String> dataSetRecordIDList = new ArrayList<>();
        final List<BitSetRecord> bitSetRecordList = this.bitSetServiceImplementation.findBitSetRecordByNucleusAndFingerprintSizeAndSetBitsCount(
                nucleus, fingerprintSize, setBitsCount)
                                                                                    .collectList()
                                                                                    .block();
        if (bitSetRecordList
                != null) {
            for (final BitSetRecord bitSetRecord : bitSetRecordList) {
                dataSetRecordIDList.addAll(bitSetRecord.getDataSetRecordIDs());
            }
        }

        return dataSetRecordIDList;
    }

    @GetMapping(value = "/getMultiplicitySectionsSettings", produces = "application/json")
    public Map<String, int[]> getMultiplicitySectionsSettings() {
        final List<MultiplicitySectionsSettingsRecord> multiplicitySectionsSettingsRecordList = this.multiplicitySectionsSettingsServiceImplementation.findAll()
                                                                                                                                                      .collectList()
                                                                                                                                                      .block();
        if (multiplicitySectionsSettingsRecordList
                != null) {
            for (final MultiplicitySectionsSettingsRecord multiplicitySectionsSettingsRecord : multiplicitySectionsSettingsRecordList) {
                this.multiplicitySectionsSettings.put(multiplicitySectionsSettingsRecord.getNucleus(),
                                                      multiplicitySectionsSettingsRecord.getMultiplicitySectionsSettings());
            }
        }

        return this.multiplicitySectionsSettings;
    }

    private void setMinLimitAndMaxLimitOfMultiplicitySectionsBuilder(final List<DataSet> dataSetList) {
        final Map<String, Integer> stepSizes = new HashMap<>();
        stepSizes.put("13C", 5);
        stepSizes.put("15N", 10);
        stepSizes.put("1H", 1);
        final Map<String, Integer[]> limits = new HashMap<>(); // min/max limit per nucleus
        String nucleus;
        Double tempMin, tempMax;
        for (final DataSet dataSet : dataSetList) {
            nucleus = dataSet.getSpectrum()
                             .getNuclei()[0];
            limits.putIfAbsent(nucleus, new Integer[]{null, null});

            tempMin = Collections.min(dataSet.getSpectrum()
                                             .getShifts(0));
            tempMax = Collections.max(dataSet.getSpectrum()
                                             .getShifts(0));
            if (limits.get(nucleus)[0]
                    == null
                    || tempMin
                    < limits.get(nucleus)[0]) {
                limits.get(nucleus)[0] = tempMin.intValue();
            }
            if (limits.get(nucleus)[1]
                    == null
                    || tempMax
                    > limits.get(nucleus)[1]) {
                limits.get(nucleus)[1] = tempMax.intValue();
            }
        }

        // delete previously stored multiplicity sections settings
        this.multiplicitySectionsSettingsServiceImplementation.deleteAll()
                                                              .block();
        int[] settings;
        for (final Map.Entry<String, Integer[]> entry : limits.entrySet()) {
            nucleus = entry.getKey();
            settings = new int[3];
            settings[0] = limits.get(nucleus)[0]
                    - stepSizes.get(nucleus); // extend by one more step
            settings[1] = limits.get(nucleus)[1]
                    // extend by one more step
                    + stepSizes.get(nucleus);
            settings[2] = stepSizes.get(nucleus);
            this.multiplicitySectionsSettings.put(nucleus, settings);
            this.multiplicitySectionsSettingsServiceImplementation.insert(
                    new MultiplicitySectionsSettingsRecord(null, nucleus, settings))
                                                                  .block();
        }
    }
}
