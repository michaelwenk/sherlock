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

package org.openscience.webcase.controller;

import casekit.nmr.dbservice.NMRShiftDB;
import casekit.nmr.model.DataSet;
import org.openscience.cdk.exception.CDKException;
import org.openscience.webcase.nmrshiftdb.model.DataSetRecord;
import org.openscience.webcase.nmrshiftdb.service.DataSetServiceImplementation;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(value = "/api/nmrshiftdb")
public class NMRShiftDBController {

    private final DataSetServiceImplementation nmrShiftDBRepository;

    public NMRShiftDBController(final DataSetServiceImplementation nmrShiftDBRepository) {
        this.nmrShiftDBRepository = nmrShiftDBRepository;
    }

    @GetMapping(value = "/count")
    public String getCount() {
        return String.valueOf(this.nmrShiftDBRepository.count());
    }

    @GetMapping(value = "/get/all")
    public List<DataSetRecord> getAll() {
        return this.nmrShiftDBRepository.findAll();
    }

    @GetMapping(value = "/get/byMf", produces = "application/json")
    public List<DataSetRecord> getByMf(@RequestParam @Valid final String mf) {
        return this.nmrShiftDBRepository.findByMf(mf);
    }

    @GetMapping(value = "/get/byNuclei", produces = "application/json")
    public List<DataSetRecord> getByDataSetSpectrumNuclei(@RequestParam @Valid final String[] nuclei) {
        return this.nmrShiftDBRepository.findByDataSetSpectrumNuclei(nuclei);
    }

    @GetMapping(value = "/get/byNucleiAndSignalCount", produces = "application/json")
    public List<DataSetRecord> getByDataSetSpectrumNucleiAndDataSetSpectrumSignalCount(@RequestParam @Valid final String[] nuclei, @RequestParam @Valid final int signalCount) {
        return this.nmrShiftDBRepository.findByDataSetSpectrumNucleiAndDataSetSpectrumSignalCount(nuclei, signalCount);
    }

    @PostMapping(value = "/insert", consumes = "application/json")
    public void insert(@RequestBody @Valid final DataSetRecord dataSetRecord) {
        this.nmrShiftDBRepository.insert(dataSetRecord);
    }

    @DeleteMapping(value = "/delete/all")
    public void deleteAll() {
        this.nmrShiftDBRepository.deleteAll();
    }

    @PostMapping(value = "/replace/all", consumes = "text/plain")
    public void replaceAll(@RequestParam @Valid final String filePath) {
        this.deleteAll();

        try {
            final String[] nuclei = new String[]{"13C", "1H", "15N"};
            final ArrayList<DataSet> dataSets = new ArrayList<>(NMRShiftDB.getDataSetsFromNMRShiftDB(filePath, nuclei));
            dataSets.forEach(dataSet -> this.nmrShiftDBRepository.insert(new DataSetRecord(null, dataSet.getMeta().get("mf"), dataSet)));
        } catch (FileNotFoundException | CDKException e) {
            e.printStackTrace();
        }
    }
}
