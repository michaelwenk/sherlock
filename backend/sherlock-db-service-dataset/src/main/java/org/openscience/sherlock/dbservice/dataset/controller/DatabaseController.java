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
import casekit.nmr.model.Spectrum;
import casekit.nmr.model.SpectrumCompact;
import casekit.nmr.similarity.Similarity;
import casekit.nmr.utils.Utils;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.BitSetFingerprint;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.sherlock.dbservice.dataset.db.model.DataSetRecord;
import org.openscience.sherlock.dbservice.dataset.db.model.MultiplicitySectionsSettingsRecord;
import org.openscience.sherlock.dbservice.dataset.db.service.DataSetServiceImplementation;
import org.openscience.sherlock.dbservice.dataset.db.service.MultiplicitySectionsSettingsServiceImplementation;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.FileNotFoundException;
import java.util.*;


@RestController
@RequestMapping(value = "/")
public class DatabaseController {

    private final DataSetServiceImplementation dataSetServiceImplementation;
    private final MultiplicitySectionsSettingsServiceImplementation multiplicitySectionsSettingsServiceImplementation;

    private final String pathToNMRShiftDB = "/data/nmrshiftdb/nmrshiftdb.sdf";
    private final String[] pathsToCOCONUT = new String[]{"/data/coconut/acd_coconut_1.sdf",
                                                         "/data/coconut/acd_coconut_2.sdf",
                                                         "/data/coconut/acd_coconut_3.sdf",
                                                         "/data/coconut/acd_coconut_4.sdf",
                                                         "/data/coconut/acd_coconut_5.sdf",
                                                         "/data/coconut/acd_coconut_6.sdf",
                                                         "/data/coconut/acd_coconut_7.sdf",
                                                         "/data/coconut/acd_coconut_8.sdf",
                                                         "/data/coconut/acd_coconut_9.sdf",
                                                         "/data/coconut/acd_coconut_10.sdf",
                                                         "/data/coconut/acd_coconut_11.sdf",
                                                         "/data/coconut/acd_coconut_12.sdf",
                                                         "/data/coconut/acd_coconut_13.sdf",
                                                         "/data/coconut/acd_coconut_14.sdf",
                                                         "/data/coconut/acd_coconut_15.sdf",
                                                         "/data/coconut/acd_coconut_16.sdf",
                                                         "/data/coconut/acd_coconut_17.sdf",
                                                         "/data/coconut/acd_coconut_18.sdf"};
    private final Map<String, int[]> multiplicitySectionsSettings = new HashMap<>();

    public DatabaseController(final DataSetServiceImplementation dataSetServiceImplementation,
                              final MultiplicitySectionsSettingsServiceImplementation multiplicitySectionsSettingsServiceImplementation) {
        this.dataSetServiceImplementation = dataSetServiceImplementation;
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
        return this.dataSetServiceImplementation.findByDataSetSpectrumNucleiAndAttachmentSetBits(nuclei, setBits);
    }

    @GetMapping(value = "/getByNucleiAndSetBitsAndMf", produces = "application/stream+json")
    public Flux<DataSetRecord> getByDataSetSpectrumNucleiAndAttachmentSetBitsAndMf(@RequestParam final String[] nuclei,
                                                                                   @RequestParam final int[] setBits,
                                                                                   final String mf) {
        return this.dataSetServiceImplementation.findByDataSetSpectrumNucleiAndAttachmentSetBitsAndMf(nuclei, setBits,
                                                                                                      mf);
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
    public Mono<Void> deleteAll() {
        return this.dataSetServiceImplementation.deleteAll();
    }

    @PostMapping(value = "/replaceAll")
    public void replaceAll(@RequestParam final String[] nuclei, @RequestParam final boolean setLimits) {
        System.out.println(" -> Deleting all DB entries...");
        this.deleteAll()
            .block();
        System.out.println(" -> Deleted all DB entries!");

        // detect bitset ranges and store in DB
        List<DataSet> dataSetList;
        if (setLimits) {
            System.out.println(" -> Setting new limits...");
            try {
                dataSetList = NMRShiftDB.getDataSetsFromNMRShiftDB(this.pathToNMRShiftDB, nuclei);
                this.setMultiplicityByProtonsCount(dataSetList, "13C");
                Map<String, Integer[]> limits = this.setMinLimitAndMaxLimitOfMultiplicitySectionsBuilder(dataSetList,
                                                                                                         new HashMap<>());
                System.out.println("dataset size NMRShiftDB -> "
                                           + dataSetList.size());
                System.out.println("limits NMRShiftDB: "
                                           + Arrays.toString(limits.get("13C")));
                for (int i = 0; i
                        < this.pathsToCOCONUT.length; i++) {
                    System.out.println(" -> COCONUT "
                                               + i
                                               + " -> "
                                               + this.pathsToCOCONUT[i]);
                    dataSetList = COCONUT.getDataSetsWithShiftPredictionFromCOCONUT(this.pathsToCOCONUT[i], nuclei);
                    this.setMultiplicityByProtonsCount(dataSetList, "13C");
                    System.out.println("dataset size COCONUT "
                                               + i
                                               + " -> "
                                               + dataSetList.size());
                    limits = this.setMinLimitAndMaxLimitOfMultiplicitySectionsBuilder(dataSetList, limits);
                    System.out.println("limits COCONUT "
                                               + i
                                               + ": "
                                               + Arrays.toString(limits.get("13C")));
                }
            } catch (final FileNotFoundException | CDKException e) {
                e.printStackTrace();
            }
            System.out.println(" -> Set new limits!");
        } else {
            MultiplicitySectionsSettingsRecord multiplicitySectionsSettingsRecord;
            for (final String nucleus : nuclei) {
                multiplicitySectionsSettingsRecord = this.multiplicitySectionsSettingsServiceImplementation.findByNucleus(
                                                                 nucleus)
                                                                                                           .block();
                this.multiplicitySectionsSettings.put(multiplicitySectionsSettingsRecord.getNucleus(),
                                                      multiplicitySectionsSettingsRecord.getMultiplicitySectionsSettings());
            }
        }

        // store datasets in DB
        // with checks whether a dataset with identical spectrum already exists
        try {
            System.out.println(" -> dataset creations...");
            dataSetList = NMRShiftDB.getDataSetsFromNMRShiftDB(this.pathToNMRShiftDB, nuclei);
            this.setMultiplicityByProtonsCount(dataSetList, "13C");
            System.out.println(" -> dataset size NMRShiftDB -> "
                                       + dataSetList.size());
            this.filterAndInsertDataSetRecords(dataSetList, new HashMap<>());
            System.out.println(" -> stored for NMRShiftDB done");
            final Map<String, Map<String, List<Spectrum>>> inserted = new HashMap<>(); // molecule id -> nucleus -> spectra list
            Flux.fromArray(this.pathsToCOCONUT)
                .doOnNext(pathToCOCONUT -> {
                    try {
                        System.out.println("storing -> "
                                                   + pathToCOCONUT);
                        final List<DataSet> dataSetListTemp = COCONUT.getDataSetsWithShiftPredictionFromCOCONUT(
                                pathToCOCONUT, nuclei);
                        this.setMultiplicityByProtonsCount(dataSetListTemp, "13C");
                        this.filterAndInsertDataSetRecords(dataSetListTemp, inserted);
                        System.out.println(pathToCOCONUT
                                                   + " -> done");
                    } catch (final CDKException | FileNotFoundException e) {
                        e.printStackTrace();
                    }
                })
                .subscribe();

        } catch (final FileNotFoundException | CDKException e) {
            e.printStackTrace();
        }
    }

    /**
     * Overwrite (non-)existing multiplicity value through protons count in signals from given nucleus type
     *
     * @param dataSetList
     * @param nucleus
     */
    private void setMultiplicityByProtonsCount(final List<DataSet> dataSetList, final String nucleus) {
        Spectrum spectrum;
        IAtomContainer structure;
        IAtom atom;
        for (final DataSet dataSet : dataSetList) {
            spectrum = dataSet.getSpectrum()
                              .toSpectrum();
            if (!spectrum.getNuclei()[0].equals(nucleus)) {
                continue;
            }
            structure = dataSet.getStructure()
                               .toAtomContainer();
            for (int i = 0; i
                    < spectrum.getSignals()
                              .size(); i++) {
                atom = structure.getAtom(dataSet.getAssignment()
                                                .getAssignment(0, i, 0));
                spectrum.getSignal(i)
                        .setMultiplicity(Utils.getMultiplicityFromProtonsCount(
                                AtomContainerManipulator.countHydrogens(structure, atom)));
            }
            dataSet.setSpectrum(new SpectrumCompact(spectrum));
        }
    }

    private void filterAndInsertDataSetRecords(final List<DataSet> dataSetList,
                                               final Map<String, Map<String, List<Spectrum>>> insertedMap) {
        String id, nucleus;
        Spectrum spectrum;
        Double averageDeviation;
        final Set<String> insertedKeys = new HashSet<>();
        for (final DataSet dataSet : new ArrayList<>(dataSetList)) {
            id = dataSet.getMeta()
                        .get("id");
            if (id
                    == null) {
                continue;
            }
            spectrum = dataSet.getSpectrum()
                              .toSpectrum();
            nucleus = spectrum.getNuclei()[0];
            insertedMap.putIfAbsent(id, new HashMap<>());
            insertedMap.get(id)
                       .putIfAbsent(nucleus, new ArrayList<>());
            if (insertedMap.get(id)
                           .get(nucleus)
                           .isEmpty()) {
                insertedMap.get(id)
                           .get(nucleus)
                           .add(spectrum);
                insertedKeys.add(id);
                continue;
            }
            // avoid storage of completely identical spectra
            for (final Spectrum insertedSpectrum : new ArrayList<>(insertedMap.get(id)
                                                                              .get(nucleus))) {
                averageDeviation = Similarity.calculateAverageDeviation(insertedSpectrum, spectrum, 0, 0, 0.0, true,
                                                                        true, false);
                if (averageDeviation
                        != null
                        && averageDeviation
                        == 0.0) {
                    dataSetList.remove(dataSet);
                } else {
                    insertedMap.get(id)
                               .get(nucleus)
                               .add(spectrum);
                    insertedKeys.add(id);
                    break;
                }
            }
        }
        // we here assume that each spectrum of the same compound should appear in one row
        // so we keep the keys from this insertion for next time to know the last inserted keys and check it
        for (final String insertedMapKey : new HashSet<>(insertedMap.keySet())) {
            if (!insertedKeys.contains(insertedMapKey)) {
                insertedMap.remove(insertedMapKey);
            }
        }

        this.dataSetServiceImplementation.insertMany(Flux.fromIterable(dataSetList)
                                                         .map(dataSet -> {
                                                             final String nucleusTemp = dataSet.getSpectrum()
                                                                                               .getNuclei()[0];
                                                             final MultiplicitySectionsBuilder multiplicitySectionsBuilder = new MultiplicitySectionsBuilder();
                                                             multiplicitySectionsBuilder.setMinLimit(
                                                                     this.multiplicitySectionsSettings.get(
                                                                             nucleusTemp)[0]);
                                                             multiplicitySectionsBuilder.setMaxLimit(
                                                                     this.multiplicitySectionsSettings.get(
                                                                             nucleusTemp)[1]);
                                                             multiplicitySectionsBuilder.setStepSize(
                                                                     this.multiplicitySectionsSettings.get(
                                                                             nucleusTemp)[2]);
                                                             final BitSetFingerprint bitSetFingerprint = Similarity.getBitSetFingerprint(
                                                                     dataSet.getSpectrum()
                                                                            .toSpectrum(), 0,
                                                                     multiplicitySectionsBuilder);

                                                             dataSet.addAttachment("fpSize", bitSetFingerprint.size());
                                                             dataSet.addAttachment("setBits",
                                                                                   bitSetFingerprint.getSetbits());

                                                             return new DataSetRecord(null, dataSet);
                                                         }))
                                         .subscribe();
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

    private Map<String, Integer[]> setMinLimitAndMaxLimitOfMultiplicitySectionsBuilder(final List<DataSet> dataSetList,
                                                                                       final Map<String, Integer[]> prevLimits) {
        final Map<String, Integer> stepSizes = new HashMap<>();
        stepSizes.put("13C", 2);
        stepSizes.put("15N", 2);
        stepSizes.put("1H", 1);
        final Map<String, Integer[]> limits = new HashMap<>(prevLimits); // min/max limit per nucleus
        String nucleus;
        Double tempMin, tempMax;
        Spectrum spectrum;
        for (final DataSet dataSet : dataSetList) {
            spectrum = dataSet.getSpectrum()
                              .toSpectrum();
            nucleus = spectrum.getNuclei()[0];
            limits.putIfAbsent(nucleus, new Integer[]{null, null});

            tempMin = Collections.min(spectrum.getShifts(0));
            tempMax = Collections.max(spectrum.getShifts(0));
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

        return limits;
    }
}
