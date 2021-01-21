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

import casekit.io.FileOperations;
import casekit.nmr.core.Dereplication;
import casekit.nmr.lsd.Constants;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.Signal;
import casekit.nmr.model.Spectrum;
import casekit.nmr.model.nmrdisplayer.Correlation;
import casekit.nmr.model.nmrdisplayer.Data;
import casekit.nmr.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openscience.webcase.nmrshiftdb.model.DataSetRecord;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static casekit.nmr.lsd.PyLSDInputFileBuilder.buildPyLSDFileContent;

@RestController
@RequestMapping(value = "/api/case")
public class CASEController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String PATH_TO_LSD_FILTER_LIST = "/Users/mwenk/work/software/PyLSD-a4/LSD/Filters/list.txt";
    private static final String PATH_TO_PYLSD_FILE_FOLDER = "/Users/mwenk/work/software/PyLSD-a4/Variant/";

    private final NMRShiftDBController nmrShiftDBController;
    private final AnalysisController analysisController;

    public CASEController(final NMRShiftDBController nmrShiftDBController, final AnalysisController analysisController) {
        this.nmrShiftDBController = nmrShiftDBController;
        this.analysisController = analysisController;
    }

    @GetMapping(value = "/elucidate", consumes = "application/json", produces = "application/json")
    public List<DataSet> elucidate(@RequestBody final String json, @RequestParam final boolean dereplicate, @RequestParam final boolean allowHeteroHeteroBonds) {
        final List<DataSet> solutions = new ArrayList<>();
        try {
            final Data data = OBJECT_MAPPER.readValue(json, Data.class);
            final String mf = (String) data.getCorrelations().getOptions().get("mf");

            // @TODO get as parameter from somewhere
            final Map<String, Double> shiftTols = (HashMap<String, Double>) data.getCorrelations().getOptions().get("tolerance");
            final double thrsHybridizations = 0.1; // threshold to take a hybridization into account

            // DEREPLICATION
            if (dereplicate) {
                final Spectrum querySpectrum = new Spectrum(new String[]{"13C"});
                data.getCorrelations().getValues().stream().filter(correlation -> correlation.getAtomType().equals("C")).forEach(correlation -> querySpectrum.addSignal(new Signal(querySpectrum.getNuclei(), new Double[]{correlation.getSignal().getDelta()}, Utils.getMultiplicityFromProtonsCount(correlation), correlation.getSignal().getKind(), null, correlation.getEquivalence(), correlation.getSignal().getSign())));
                if (querySpectrum.getSignals().stream().noneMatch(signal -> signal.getMultiplicity() == null)) {
                    solutions.addAll(this.dereplication(querySpectrum, mf, shiftTols.get("C")));
                    if (!solutions.isEmpty()) {
                        return solutions;
                    }
                }
            }

            // @TODO check possible structural input (incl. assignment) by nmr-displayer

            // @TODO SUBSTRUCTRUE SEARCH

            // PyLSD FILE CONTENT CREATION
            final String pyLSDFileContent = buildPyLSDFileContent(data, mf, getDetectedHybridizations(data, thrsHybridizations), allowHeteroHeteroBonds, PATH_TO_LSD_FILTER_LIST);

            // write PyLSD file
            // write content into PyLSD file and store it
            FileOperations.writeFile(PATH_TO_PYLSD_FILE_FOLDER + "test.pyLSD", pyLSDFileContent);

            //            // execute PyLSD
            //            final ProcessBuilder builder = new ProcessBuilder();
            //            builder.directory(new File(PATH_TO_PYLSD_FILE_FOLDER));
            //            builder.redirectError(new File(PATH_TO_PYLSD_FILE_FOLDER + "error.txt"));
            //            builder.redirectOutput(new File(PATH_TO_PYLSD_FILE_FOLDER + "log.txt"));
            //            builder.command("python2.7", PATH_TO_PYLSD_FILE_FOLDER + "lsd.py", PATH_TO_PYLSD_FILE_FOLDER + "test.pyLSD");
            //            final Process process = builder.start();
            //            int exitCode = process.waitFor();
            //            assert exitCode == 0;

        } catch (final Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
        }

        return solutions;
    }

    public List<DataSet> dereplication(final Spectrum querySpectrum, final String mf, final double shiftTol) {

        final List<DataSet> results;
        if (mf != null) {
            results = this.nmrShiftDBController.getByMf(mf).stream().map(DataSetRecord::getDataSet).collect(Collectors.toList());
        } else {
            // @TODO take the nuclei order into account when matching -> now it's just an exact array match
            results = this.nmrShiftDBController.getByDataSetSpectrumNucleiAndDataSetSpectrumSignalCount(querySpectrum.getNuclei(), querySpectrum.getSignalCount()).stream().map(DataSetRecord::getDataSet).collect(Collectors.toList());
        }
        return Dereplication.dereplicate1D(querySpectrum, results, shiftTol);
    }

    public Map<Integer, List<Integer>> getDetectedHybridizations(final Data data, final double thrs) {
        final HashMap<Integer, List<Integer>> detectedHybridizations = new HashMap<>();
        Correlation correlation;
        String multiplicity;
        for (int i = 0; i < data.getCorrelations().getValues().size(); i++) {
            correlation = data.getCorrelations().getValues().get(i);
            multiplicity = Utils.getMultiplicityFromProtonsCount(correlation);
            if (multiplicity != null) {
                detectedHybridizations.put(i, analysisController.detectHybridization(Constants.nucleiMap.get(correlation.getAtomType()), (int) correlation.getSignal().getDelta() - 2, (int) correlation.getSignal().getDelta() + 2, multiplicity, thrs));
            }
        }

        return detectedHybridizations;
    }
}
