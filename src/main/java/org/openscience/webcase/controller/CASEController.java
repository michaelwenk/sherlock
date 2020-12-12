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
import org.openscience.webcase.nmrshiftdb.model.DataSetRecord;
import org.openscience.webcase.nmrshiftdb.service.HybridizationRepository;
import org.springframework.web.bind.annotation.*;

import java.io.*;
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
    private final HybridizationRepository hybridizationRepository;

    public CASEController(final NMRShiftDBController nmrShiftDBController, final HybridizationRepository hybridizationRepository) {
        this.nmrShiftDBController = nmrShiftDBController;
        this.hybridizationRepository = hybridizationRepository;
    }

    @GetMapping(value = "/elucidation", consumes = "application/json", produces = "application/json")
    public List<DataSet> elucidate(@RequestBody final String json, @RequestParam final boolean dereplicate, @RequestParam final boolean heteroHeteroBonds) {

        final List<DataSet> solutions = new ArrayList<>();

        try {
            final Data data = OBJECT_MAPPER.readValue(json, Data.class);
            final Optional<org.openscience.webcase.nmrdisplayer.model.Spectrum> nmrDisplayerQuerySpectrum = data.getSpectra().stream().filter(spectrum -> spectrum.getInfo().containsKey("dimension") && (int) spectrum.getInfo().get("dimension") == 1 && spectrum.getInfo().containsKey("nucleus") && spectrum.getInfo().get("nucleus").equals("13C") && spectrum.getInfo().containsKey("experiment") && spectrum.getInfo().get("experiment").equals("1d")).findFirst();
            //            final Optional<org.openscience.webcase.nmrdisplayer.model.Spectrum> nmrDisplayerQuerySpectrum = data.getSpectra().stream().filter(spectrum -> spectrum.getInfo().containsKey("dimension") && (int) spectrum.getInfo().get("dimension") == 2 && spectrum.getInfo().containsKey("experiment") && spectrum.getInfo().get("experiment").equals("hsqc")).findFirst();
            final String mf = (String) data.getCorrelations().getOptions().get("mf");

            final Map<String, String> nucleiMap = new HashMap<>();
            nucleiMap.put("C", "13C");
            nucleiMap.put("N", "15N");
            nucleiMap.put("H", "1H");
            // @TODO get as parameter from somewhere
            final Map<String, Double> shiftTols = (HashMap<String, Double>) data.getCorrelations().getOptions().get("tolerance");
            final double thrs = 0.1; // threshold to take a hybridization into account


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
                stringBuilder.append("; PyLSD input file created by webCASE\n");
                final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
                final Date date = new Date(System.currentTimeMillis());
                stringBuilder.append("; ").append(formatter.format(date)).append("\n\n");

                final Map<String, Integer> elementCounts = new LinkedHashMap<>(Utils.getMolecularFormulaElementCounts(mf));
                final Map<Integer, String> elementIndicesWithCorrelations = new HashMap<>();
                // index in correlation data -> [atom type, index in PyLSD file]
                final Map<Integer, Object[]> elementIndicesWithoutCorrelations = new HashMap<>();

                // FORM
                stringBuilder.append("; Molecular Formula: ").append(mf).append("\n");
                stringBuilder.append("FORM ");
                elementCounts.forEach((elem, count) -> stringBuilder.append(elem).append(" ").append(count).append(" "));
                stringBuilder.append("\n\n");

                // PIEC
                stringBuilder.append("PIEC 1").append("\n\n");

                Correlation correlation;
                StringBuilder hybridizationStringBuilder;
                int attachedHydrogenCount;

                // MULT

                // valid strings from LSD webpage: C N N5 O S S4 S6 F Cl Br I P P5 Si B X
                final Map<String, String> defaultHybridization = new HashMap<>();
                defaultHybridization.put("C", "(1 2 3)");
                defaultHybridization.put("N", "(1 2 3)");
                defaultHybridization.put("O", "(2 3)");
                defaultHybridization.put("S", "(1 2 3)");

                List<Integer> detectedHybridizations;
                final Map<String, String> attachedHydrogensPerValency = new HashMap<>();
                attachedHydrogensPerValency.put("N", "(0 1 2)");
                attachedHydrogensPerValency.put("N5", "(0 1 2 3)");
                attachedHydrogensPerValency.put("N35", "(0 1 2 3)");
                attachedHydrogensPerValency.put("S", "(0 1)");
                attachedHydrogensPerValency.put("S4", "(0 1 2 3)");
                attachedHydrogensPerValency.put("S6", "(0 1 2 3)");
                attachedHydrogensPerValency.put("S246", "(0 1 2 3)");
                attachedHydrogensPerValency.put("O", "(0 1)");

                final Map<String, String> defaultAtomLabel = new HashMap<>();
                defaultAtomLabel.put("N", "N35");
                defaultAtomLabel.put("O", "O");
                defaultAtomLabel.put("S", "S246");


                // init element indices within correlations with same order as in correlation data input
                int protonsCount = 0;
                for (int i = 0; i < data.getCorrelations().getValues().size(); i++) {
                    correlation = data.getCorrelations().getValues().get(i);
                    elementIndicesWithCorrelations.put(i, correlation.getAtomType());

                    if (correlation.getAtomType().equals("H")) {
                        protonsCount++;
                    }
                }
                // init element indices which do not have correlation (subtract protons count)
                int indexInCorrelations = elementIndicesWithCorrelations.size();
                int indexInPyLSDFile = elementIndicesWithCorrelations.size() - protonsCount + 1;
                for (final Map.Entry<String, Integer> set : elementCounts.entrySet()) {
                    if (elementIndicesWithCorrelations.containsValue(set.getKey())) {
                        continue;
                    }
                    for (int i = 0; i < elementCounts.get(set.getKey()); i++) {
                        elementIndicesWithoutCorrelations.put(indexInCorrelations, new Object[]{set.getKey(), indexInPyLSDFile});
                        indexInCorrelations++;
                        indexInPyLSDFile++;
                    }
                }
                // build MULT section
                for (int i = 0; i < data.getCorrelations().getValues().size(); i++) {
                    correlation = data.getCorrelations().getValues().get(i);
                    if (correlation.getAtomType().equals("H")) {
                        continue;
                    }
                    attachedHydrogenCount = 0;
                    for (final Link link : correlation.getLink()) {
                        if (link.getExperimentType().equals("hsqc")) {
                            attachedHydrogenCount += link.getMatch().stream().reduce(0, (sum, index) -> sum + data.getCorrelations().getValues().get(index).getCount());
                        }
                    }
                    for (int j = 0; j < correlation.getCount(); j++) {
                        detectedHybridizations = this.detectHybridization(nucleiMap.get(correlation.getAtomType()), (int) correlation.getSignal().getDelta(), correlation.getSignal().getMultiplicity(), thrs);
                        if (detectedHybridizations.isEmpty()) {
                            hybridizationStringBuilder = new StringBuilder(defaultHybridization.get(correlation.getAtomType()));
                        } else {
                            hybridizationStringBuilder = new StringBuilder();
                            if (detectedHybridizations.size() > 1) {
                                hybridizationStringBuilder.append("(");
                            }
                            for (int k = 0; k < detectedHybridizations.size(); k++) {
                                hybridizationStringBuilder.append(detectedHybridizations.get(k));
                                if (k < detectedHybridizations.size() - 1) {
                                    hybridizationStringBuilder.append(" ");
                                }
                            }
                            if (detectedHybridizations.size() > 1) {
                                hybridizationStringBuilder.append(")");
                            }
                        }
                        stringBuilder.append("MULT ").append(i + 1).append(" ").append(correlation.getAtomType()).append(" ").append(hybridizationStringBuilder).append(" ").append(attachedHydrogenCount).append("\n");
                    }
                }

                for (final Map.Entry<Integer, Object[]> set : elementIndicesWithoutCorrelations.entrySet()) {
                    stringBuilder.append("MULT ").append((int) set.getValue()[1]).append(" ").append(defaultAtomLabel.get(set.getValue()[0])).append(" ").append(defaultHybridization.get(set.getValue()[0])).append(" ").append(attachedHydrogensPerValency.get(defaultAtomLabel.get(set.getValue()[0]))).append("\n");
                }
                stringBuilder.append("\n");

                final Map<Integer, Object[]> hydrogenIndicesMap = new HashMap<>();
                int hydrogenIndexInPyLSDFile = indexInPyLSDFile;
                for (final Map.Entry<Integer, String> set : elementIndicesWithCorrelations.entrySet()) {
                    if (!set.getValue().equals("H")) {
                        continue;
                    }
                    hydrogenIndicesMap.put(set.getKey(), new Object[]{set.getValue(), hydrogenIndexInPyLSDFile});
                    System.out.println(set.getKey() + " -> " + set.getValue() + ", " + hydrogenIndexInPyLSDFile);
                    hydrogenIndexInPyLSDFile++;
                }

                // HSQC
                for (int i = 0; i < data.getCorrelations().getValues().size(); i++) {
                    correlation = data.getCorrelations().getValues().get(i);
                    if (correlation.getAtomType().equals("H"))
                        continue;

                    for (final Link link : correlation.getLink()) {
                        if (link.getExperimentType().equals("hsqc")) {
                            for (final Integer matchIndex : link.getMatch()) {
                                stringBuilder.append("HSQC ").append(i + 1).append(" ").append(hydrogenIndicesMap.get(matchIndex)[1]).append("\n");
                            }
                        }
                    }
                }
                stringBuilder.append("\n");

                // HMBC
                final String defaultBondDistance = "2 4";
                for (int i = 0; i < data.getCorrelations().getValues().size(); i++) {
                    correlation = data.getCorrelations().getValues().get(i);
                    if (correlation.getAtomType().equals("H"))
                        continue;

                    for (final Link link : correlation.getLink()) {
                        if (link.getExperimentType().equals("hmbc")) {
                            for (final Integer matchIndex : link.getMatch()) {
                                stringBuilder.append("HMBC ").append(i + 1).append(" ").append(hydrogenIndicesMap.get(matchIndex)[1]).append(" ").append(defaultBondDistance).append("\n");
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
                                stringBuilder.append("COSY ").append(i + 1).append(" ").append(hydrogenIndicesMap.get(matchIndex)[1]).append("\n"); //.append(" ").append(defaultBondDistance).append("\n");
                            }
                        }
                    }
                }
                stringBuilder.append("\n");

                // BOND (interpretation, INADEQUATE, previous assignments) -> input fragments

                // LIST PROP for hetero hetero bonds allowance
                if (!heteroHeteroBonds) {
                    // create hetero atom list manually
                    final ArrayList<Integer> heteroAtomList = new ArrayList<>();
                    elementIndicesWithCorrelations.entrySet().stream().filter(set -> !set.getValue().equals("H") && !set.getValue().equals("C")).forEach(set -> heteroAtomList.add(set.getKey() + 1));
                    elementIndicesWithoutCorrelations.entrySet().stream().filter(set -> !set.getValue().equals("H") && !set.getValue().equals("C")).forEach(set -> heteroAtomList.add((int) elementIndicesWithoutCorrelations.get(set.getKey())[1]));
                    if (!heteroAtomList.isEmpty()) {
                        stringBuilder.append("LIST L1");
                        heteroAtomList.forEach(index -> stringBuilder.append(" ").append(index));
                        stringBuilder.append("; list of hetero atoms\n");
                        stringBuilder.append("PROP L1 0 L1 -; no hetero-hetero bonds\n");
                    }

                    //                    // create hetero atom list automatically
                    //                    stringBuilder.append("HETE L1").append("; list of hetero atoms\n");
                    //                    stringBuilder.append("PROP L1 0 L1 -; no hetero-hetero bonds\n");

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
                    if (!correlation.getAtomType().equals("H"))
                        continue;
                    // only consider protons which are attached via HSQC/HMQC
                    for (final Link link : correlation.getLink()) {
                        if (link.getExperimentType().equals("hsqc") && !link.getMatch().isEmpty()) {
                            stringBuilder.append("SHIH ").append(hydrogenIndicesMap.get(i)[1]).append(" ").append(correlation.getSignal().getDelta()).append("\n");
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

                final FileWriter fileWriter = new FileWriter(new File(PATH_TO_PYLSD_FILE));
                final BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.write(stringBuilder.toString());
                bufferedWriter.close();
            }


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

    public List<Integer> detectHybridization(final String nucleus, final int shift, final String multiplicity, final double thrs) {
        final List<String> hybridizations = this.hybridizationRepository.aggregateHybridizationsByNucleusAndShiftAndMultiplicity(nucleus, shift - 1, shift + 1, multiplicity);
        final Set<String> unique = new HashSet<>(hybridizations);

        // @TODO access this information from MongoDB and store it instead of hard coding it
        // possible command in MongoDB: db.hybridizations.aggregate([{$match: {nucleus: "15N"}}, {$group: {_id: null, set: {$addToSet: "$hybridization"}}}])
        // nucleus -> hybridization string -> number
        final Map<String, Map<String, Integer>> conversionMap = new HashMap<>();
        conversionMap.put("13C", new HashMap<>());
        conversionMap.get("13C").put("PLANAR3", 3);
        conversionMap.get("13C").put("SP3", 3);
        conversionMap.get("13C").put("SP2", 2);
        conversionMap.get("13C").put("SP1", 1);
        conversionMap.put("15N", new HashMap<>());
        conversionMap.get("15N").put("PLANAR3", 3);
        conversionMap.get("15N").put("SP3", 3);
        conversionMap.get("15N").put("SP2", 2);
        conversionMap.get("15N").put("SP1", 1);

        final List<Integer> values = new ArrayList<>();

        unique.forEach(label -> {
            if (conversionMap.containsKey(nucleus) && conversionMap.get(nucleus).containsKey(label) && hybridizations.stream().filter(value -> value.equals(label)).count() / (double) hybridizations.size() > thrs) {
                values.add(conversionMap.get(nucleus).get(label));
            }
        });

        return values;
    }
}
