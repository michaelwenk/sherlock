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

import casekit.nmr.Utils;
import casekit.nmr.model.DataSet;
import org.openscience.cdk.interfaces.IAtomType;
import org.openscience.webcase.nmrshiftdb.model.DataSetRecord;
import org.openscience.webcase.nmrshiftdb.model.HybridizationRecord;
import org.openscience.webcase.nmrshiftdb.service.HybridizationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/analysis")
public class AnalysisController {

    private final NMRShiftDBController nmrShiftDBController;
    private final HybridizationRepository hybridizationRepository;

    public AnalysisController(final NMRShiftDBController nmrShiftDBController, final HybridizationRepository hybridizationRepository) {
        this.nmrShiftDBController = nmrShiftDBController;
        this.hybridizationRepository = hybridizationRepository;
    }

    @GetMapping(value = "/getHybridizationCollection", produces = "application/json")
    public List<HybridizationRecord> getHybridizationCollection() {
        return this.hybridizationRepository.findAll();
    }

    @GetMapping(value = "/buildHybridizationCollection")
    public void buildHybridizationCollection() {

        this.hybridizationRepository.deleteAll();

        final String[] nuclei = new String[]{"13C", "15N"};
        DataSet dataSet;
        String atomType;
        IAtomType.Hybridization hybridization;
        String multiplicity;
        Integer shift;
        for (final String nucleus : nuclei) {
            final List<DataSetRecord> dataSetRecords = this.nmrShiftDBController.getByDataSetSpectrumNuclei(new String[]{nucleus});
            atomType = Utils.getAtomTypeFromNucleus(nucleus);
            for (DataSetRecord dataSetRecord : dataSetRecords) {
                dataSet = dataSetRecord.getDataSet();
                for (int i = 0; i < dataSet.getAssignment().getAssignmentsCount(); i++) {
                    hybridization = dataSet.getStructure().getHybridization(dataSet.getAssignment().getAssignment(0, i));
                    multiplicity = dataSet.getSpectrum().getMultiplicity(i);
                    shift = null;

                    if (dataSet.getSpectrum().getShift(i, 0) != null) {
                        shift = dataSet.getSpectrum().getShift(i, 0).intValue();
                    }
                    if (shift == null || dataSet.getStructure().getAtomType(dataSet.getAssignment().getAssignment(0, i)) == null || !dataSet.getStructure().getAtomType(dataSet.getAssignment().getAssignment(0, i)).equals(atomType)) {
                        continue;
                    }
                    this.hybridizationRepository.insert(new HybridizationRecord(null, nucleus, shift, multiplicity, hybridization.name()));
                }
            }
        }
    }
}
