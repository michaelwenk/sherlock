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

import org.openscience.webcase.dbservice.dataset.nmrshiftdb.model.DataSetRecord;
import org.openscience.webcase.dbservice.dataset.nmrshiftdb.service.DataSetServiceImplementation;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping(value = "/nmrshiftdb")
public class NMRShiftDBController {

    private final DataSetServiceImplementation dataSetServiceImplementation;

    public NMRShiftDBController(final DataSetServiceImplementation dataSetServiceImplementation) {
        this.dataSetServiceImplementation = dataSetServiceImplementation;
    }

    @GetMapping(value = "/count")
    public Mono<Long> getCount() {
        return this.dataSetServiceImplementation.count();
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
    public Flux<DataSetRecord> getByDataSetSpectrumNucleiAndDataSetSpectrumSignalCount(@RequestParam final String[] nuclei, @RequestParam final int signalCount) {
        return this.dataSetServiceImplementation.findByDataSetSpectrumNucleiAndDataSetSpectrumSignalCount(nuclei, signalCount);
    }

//    @PostMapping(value = "/insert", consumes = "application/json")
//    public void insert(@RequestBody final DataSetRecord dataSetRecord) {
//        this.nmrShiftDBRepository.insert(dataSetRecord);
//    }

//    @DeleteMapping(value = "/delete/all")
//    public void deleteAll() {
//        this.nmrShiftDBRepository.deleteAll();
//    }

//    @PostMapping(value = "/replace/all", consumes = "text/plain")
//    public void replaceAll(@RequestParam final String filePath) {
//        this.deleteAll();
//
//        //        try {
//        final String[] nuclei = new String[]{"13C", "1H", "15N"};
//        final ArrayList<DataSet> dataSets = new ArrayList<>(); //new ArrayList<>(NMRShiftDB.getDataSetsFromNMRShiftDB(filePath, nuclei));
//        dataSets.forEach(dataSet -> this.nmrShiftDBRepository.insert(new DataSetRecord(null, dataSet.getMeta().get("mf"), dataSet)));
//        //        } catch (FileNotFoundException e) {
//        //            e.printStackTrace();
//        //        }
//    }
}
