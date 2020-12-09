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
import casekit.nmr.core.Dereplication;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.Spectrum;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openscience.webcase.nmrdisplayer.model.Correlation;
import org.openscience.webcase.nmrdisplayer.model.Data;
import org.openscience.webcase.nmrdisplayer.model.Link;
import org.openscience.webcase.nmrshiftdb.model.NMRShiftDBRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/case")
public class CASEController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String PATH_TO_LSD_FILTER_LIST = "/Users/mwenk/work/software/PyLSD-a4/LSD/Filters/list.txt";
    private static final String PATH_TO_PYLSD_FILE = "/Users/mwenk/work/software/PyLSD-a4/Variant/test.pyLSD";
    private final NMRShiftDBController nmrShiftDBController;

    @Autowired
    public CASEController(NMRShiftDBController nmrShiftDBController) {
        this.nmrShiftDBController = nmrShiftDBController;
    }

    @GetMapping(value = "/elucidation", consumes = "application/json")
    public List<DataSet> elucidate(@RequestBody final String json, @RequestParam final boolean dereplicate, @RequestParam final boolean heteroHeteroBonds) {

        final List<DataSet> solutions = new ArrayList<>();

        try {
            final Data data = OBJECT_MAPPER.readValue(json, Data.class);
            final Optional<org.openscience.webcase.nmrdisplayer.model.Spectrum> nmrDisplayerQuerySpectrum = data.getSpectra().stream().filter(spectrum -> spectrum.getInfo().containsKey("dimension") && (int) spectrum.getInfo().get("dimension") == 1 && spectrum.getInfo().containsKey("nucleus") && spectrum.getInfo().get("nucleus").equals("13C") && spectrum.getInfo().containsKey("experiment") && spectrum.getInfo().get("experiment").equals("1d")).findFirst();
            //            final Optional<org.openscience.webcase.nmrdisplayer.model.Spectrum> nmrDisplayerQuerySpectrum = data.getSpectra().stream().filter(spectrum -> spectrum.getInfo().containsKey("dimension") && (int) spectrum.getInfo().get("dimension") == 2 && spectrum.getInfo().containsKey("experiment") && spectrum.getInfo().get("experiment").equals("hsqc")).findFirst();
            final String mf = (String) data.getCorrelations().getOptions().get("mf");
            final Map<String, Double> shiftTols = (HashMap<String, Double>) data.getCorrelations().getOptions().get("tolerance");

            // DEREPLICATION
            if (dereplicate && nmrDisplayerQuerySpectrum.isPresent()) {
                solutions.addAll(this.dereplication(nmrDisplayerQuerySpectrum.get().toSpectrum(true), mf, shiftTols.get("C")));
                if (!solutions.isEmpty()) {
                    return solutions;
                }
            }

            // @TODO check possible structural input (incl. assignment) by nmr-displayer

            // @TODO SUBSTRUCTRUE SEARCH

            // PyLSD FILE CREATION
            final HashMap<String, HashMap<String, Object>> state = data.getCorrelations().getState();
            boolean hasErrors = state.keySet().stream().anyMatch(s -> state.get(s).containsKey("error"));
            if (!hasErrors && mf != null) {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("; PyLSD file created by webCASE\n");
                final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
                final Date date = new Date(System.currentTimeMillis());
                stringBuilder.append("; ").append(formatter.format(date)).append("\n\n");

                final Map<String, Integer> elementCounts = new LinkedHashMap<>(Utils.getMolecularFormulaElementCounts(mf));
                final Map<String, ArrayList<Integer>> elementsWithoutCorrelations = new LinkedHashMap<>();
                elementCounts.keySet().forEach(elem -> elementsWithoutCorrelations.put(elem, new ArrayList<>()));

                // FORM
                stringBuilder.append("; Molecular Formula: ").append(mf).append("\n");
                stringBuilder.append("FORM ");
                elementCounts.forEach((elem, count) -> stringBuilder.append(elem).append(" ").append(count).append(" "));
                stringBuilder.append("\n\n");

                // PIEC
                stringBuilder.append("PIEC 1").append("\n\n");

                Correlation correlation;
                final String hybridization = "(1 2 3)";
                int hydrogenCount;
                int heavyAtomCorrelationCount = 0;

                // MULT
                for (int i = 0; i < data.getCorrelations().getValues().size(); i++) {
                    correlation = data.getCorrelations().getValues().get(i);
                    if (correlation.getAtomType().equals("H")) {
                        elementsWithoutCorrelations.remove("H");
                        continue;
                    }

                    heavyAtomCorrelationCount++;
                    elementsWithoutCorrelations.remove(correlation.getAtomType());
                    hydrogenCount = 0;
                    for (final Link link : correlation.getLink()) {
                        if (link.getExperimentType().equals("hsqc")) {
                            hydrogenCount += link.getMatch().stream().reduce(0, (sum, index) -> sum + data.getCorrelations().getValues().get(index).getCount());
                        }
                    }
                    for (int j = 0; j < correlation.getCount(); j++) {
                        stringBuilder.append("MULT ").append(i + 1).append(" ").append(correlation.getAtomType()).append(" ").append(hybridization).append(" ").append(hydrogenCount).append("\n");
                    }
                }

                for (final Map.Entry<String, ArrayList<Integer>> set : elementsWithoutCorrelations.entrySet()) {
                    for (int i = 0; i < elementCounts.get(set.getKey()); i++) {
                        set.getValue().add(heavyAtomCorrelationCount);
                        heavyAtomCorrelationCount++;
                        stringBuilder.append("MULT ").append(heavyAtomCorrelationCount).append(" ").append(set.getKey()).append(" ").append(hybridization).append(" ").append(0).append("\n");
                    }
                }
                stringBuilder.append("\n");

                // HSQC
                for (int i = 0; i < data.getCorrelations().getValues().size(); i++) {
                    correlation = data.getCorrelations().getValues().get(i);
                    if (correlation.getAtomType().equals("H"))
                        continue;

                    for (final Link link : correlation.getLink()) {
                        if (link.getExperimentType().equals("hsqc")) {
                            stringBuilder.append("HSQC ").append(i + 1).append(" ").append(i + 1).append("\n");
                        }
                    }
                }
                stringBuilder.append("\n");

                // HMBC
                final String bondDistance = "2 3";
                for (int i = 0; i < data.getCorrelations().getValues().size(); i++) {
                    correlation = data.getCorrelations().getValues().get(i);
                    if (correlation.getAtomType().equals("H"))
                        continue;

                    for (final Link link : correlation.getLink()) {
                        if (link.getExperimentType().equals("hmbc")) {
                            for (final Integer matchIndex : link.getMatch()) {
                                stringBuilder.append("HMBC ").append(i + 1).append(" ").append(matchIndex + 1).append(" ").append(bondDistance).append("\n");
                            }

                        }
                    }
                }
                stringBuilder.append("\n");

                // COSY
                for (int i = 0; i < data.getCorrelations().getValues().size(); i++) {
                    correlation = data.getCorrelations().getValues().get(i);
                    if (!correlation.getAtomType().equals("H"))
                        continue;

                    for (final Link link : correlation.getLink()) {
                        if (link.getExperimentType().equals("cosy")) {
                            for (final Integer matchIndex : link.getMatch()) {
                                stringBuilder.append("COSY ").append(i + 1).append(" ").append(matchIndex + 1).append("\n"); //.append(" ").append(bondDistance).append("\n");
                            }
                        }
                    }
                }
                stringBuilder.append("\n");

                // BOND (interpretation, INADEQUATE, previous assignments) -> input fragments

                // LIST PROP for hetero hetero bonds allowance
                if (!heteroHeteroBonds) {
                    final ArrayList<Integer> heteroAtomList = new ArrayList<>();

                    for (int i = 0; i < data.getCorrelations().getValues().size(); i++) {
                        correlation = data.getCorrelations().getValues().get(i);
                        if (correlation.getAtomType().equals("H") || correlation.getAtomType().equals("C"))
                            continue;

                        heteroAtomList.add(i + 1);
                    }
                    elementsWithoutCorrelations.entrySet().stream().filter(set -> !set.getKey().equals("H") && !set.getKey().equals("C")).map(Map.Entry::getValue).forEach(heteroAtomList::addAll);

                    if (!heteroAtomList.isEmpty()) {
                        stringBuilder.append("LIST HETERO");
                        heteroAtomList.forEach(index -> stringBuilder.append(" ").append(index));
                        stringBuilder.append("; list of hetero atoms\n");
                        stringBuilder.append("PROP HETERO 0 HETERO -; no hetero-hetero bonds\n");
                    }

                    stringBuilder.append("\n");
                }

                // SHIX / SHIH
                for (int i = 0; i < data.getCorrelations().getValues().size(); i++) {
                    correlation = data.getCorrelations().getValues().get(i);
                    if (correlation.getAtomType().equals("H"))
                        continue;
                    stringBuilder.append("SHIX ").append(i + 1).append(" ").append(correlation.getSignal().getDelta()).append("\n");
                }
                stringBuilder.append("\n");

                for (int i = 0; i < data.getCorrelations().getValues().size(); i++) {
                    correlation = data.getCorrelations().getValues().get(i);
                    if (correlation.getAtomType().equals("H"))
                        continue;

                    for (final Link link : correlation.getLink()) {
                        if (link.getExperimentType().equals("hsqc")) {
                            for (final Integer matchIndex : link.getMatch()) {
                                stringBuilder.append("SHIH ").append(i + 1).append(" ").append(data.getCorrelations().getValues().get(matchIndex).getSignal().getDelta()).append("\n");
                            }

                        }
                    }
                }
                stringBuilder.append("\n");

                // DEFF + FEXP -> add filters
                stringBuilder.append("; externally defined filters\n");

                final FileReader fileReader = new FileReader(new File(PATH_TO_LSD_FILTER_LIST));
                final BufferedReader bufferedReader = new BufferedReader(fileReader);
                String line;
                int counter = 1;
                final Map<String, String> filters = new LinkedHashMap<>();
                while ((line = bufferedReader.readLine()) != null) {
                    filters.put("F" + counter, line);
                    counter++;
                }
                fileReader.close();

                if (!filters.isEmpty()) {
                    filters.forEach((label, filePath) -> stringBuilder.append("DEFF ").append(label).append(" \"").append(filePath).append("\"\n"));
                    stringBuilder.append("\n");

                    stringBuilder.append("FEXP \"");
                    counter = 0;
                    for (final String label : filters.keySet()) {
                        stringBuilder.append("NOT ").append(label);
                        if (counter < filters.size() - 1) {
                            stringBuilder.append(" and ");
                        }
                        counter++;
                    }
                    stringBuilder.append("\"\n");
                }

                System.out.println(stringBuilder.toString());

                //                final FileWriter fileWriter = new FileWriter(new File(PATH_TO_PYLSD_FILE));
                //                final BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                //                bufferedWriter.write(stringBuilder.toString());
                //                bufferedWriter.close();

            }


        } catch (final Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
        }

        return solutions;
    }

    public List<DataSet> dereplication(final Spectrum querySpectrum, final String mf, final double shiftTol) {

        final List<DataSet> results;
        if (mf != null) {
            results = this.nmrShiftDBController.getByMf(mf).stream().map(NMRShiftDBRecord::getDataSet).collect(Collectors.toList());
        } else {
            // @TODO take the nuclei order into account when matching -> now it's just an exact array match
            results = this.nmrShiftDBController.getByDataSetSpectrumNucleiAndDataSetSpectrumSignalCount(querySpectrum.getNuclei(), querySpectrum.getSignalCount()).stream().map(NMRShiftDBRecord::getDataSet).collect(Collectors.toList());
        }

        return Dereplication.dereplicate1D(querySpectrum, results, shiftTol);
    }
}
