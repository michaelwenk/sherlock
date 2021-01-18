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
    private static final String PATH_TO_PYLSD_FILE_FOLDER = "/Users/mwenk/work/software/PyLSD-a4/Variant/";

    private final NMRShiftDBController nmrShiftDBController;
    private final HybridizationRepository hybridizationRepository;

    public CASEController(final NMRShiftDBController nmrShiftDBController, final HybridizationRepository hybridizationRepository) {
        this.nmrShiftDBController = nmrShiftDBController;
        this.hybridizationRepository = hybridizationRepository;
    }

    @GetMapping(value = "/elucidation", consumes = "application/json", produces = "application/json")
    public List<DataSet> elucidate(@RequestBody final String json, @RequestParam final boolean dereplicate, @RequestParam final boolean allowHeteroHeteroBonds) {

        final List<DataSet> solutions = new ArrayList<>();

        try {
            final Data data = OBJECT_MAPPER.readValue(json, Data.class);
            final Optional<org.openscience.webcase.nmrdisplayer.model.Spectrum> nmrDisplayerQuerySpectrum = data.getSpectra().stream().filter(spectrum -> spectrum.getInfo().containsKey("dimension") && (int) spectrum.getInfo().get("dimension") == 1 && spectrum.getInfo().containsKey("nucleus") && spectrum.getInfo().get("nucleus").equals("13C") && spectrum.getInfo().containsKey("experiment") && spectrum.getInfo().get("experiment").equals("1d")).findFirst();
            //            final Optional<org.openscience.webcase.nmrdisplayer.model.Spectrum> nmrDisplayerQuerySpectrum = data.getSpectra().stream().filter(spectrum -> spectrum.getInfo().containsKey("dimension") && (int) spectrum.getInfo().get("dimension") == 2 && spectrum.getInfo().containsKey("experiment") && spectrum.getInfo().get("experiment").equals("hsqc")).findFirst();
            final String mf = (String) data.getCorrelations().getOptions().get("mf");


            // @TODO get as parameter from somewhere
            final Map<String, Double> shiftTols = (HashMap<String, Double>) data.getCorrelations().getOptions().get("tolerance");
            final double thrsHybridizations = 0.1; // threshold to take a hybridization into account

            // DEREPLICATION
            if (dereplicate && nmrDisplayerQuerySpectrum.isPresent()) {
                solutions.addAll(this.dereplication(nmrDisplayerQuerySpectrum.get().toSpectrum(true), mf, shiftTols.get("C")));
                if (!solutions.isEmpty()) {
                    return solutions;
                }
            }

            // @TODO check possible structural input (incl. assignment) by nmr-displayer

            // @TODO SUBSTRUCTRUE SEARCH

            // PyLSD FILE CONTENT CREATION
            final String pyLSDFileContent = buildPyLSDFileContent(data, mf, thrsHybridizations, allowHeteroHeteroBonds);

            // write PyLSD file
            // write content into PyLSD file and store it
            final FileWriter fileWriter = new FileWriter(PATH_TO_PYLSD_FILE_FOLDER + "test.pyLSD");
            final BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(pyLSDFileContent);
            bufferedWriter.close();

            //                // execute PyLSD
            //                final ProcessBuilder builder = new ProcessBuilder();
            //                builder.directory(new File(PATH_TO_PYLSD_FILE_FOLDER));
            //                builder.redirectError(new File(PATH_TO_PYLSD_FILE_FOLDER + "error.txt"));
            //                builder.redirectOutput(new File(PATH_TO_PYLSD_FILE_FOLDER + "log.txt"));
            //
            //                builder.command("python2.7", PATH_TO_PYLSD_FILE_FOLDER + "lsd.py", PATH_TO_PYLSD_FILE_FOLDER + "test.pyLSD");
            //                final Process process = builder.start();
            //                int exitCode = process.waitFor();
            //                assert exitCode == 0;

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

    public String buildPyLSDFileContent(final Data data, final String mf, final double thrsHybridizations, final boolean allowHeteroHeteroBonds) {
        final HashMap<String, HashMap<String, Object>> state = data.getCorrelations().getState();
        boolean hasErrors = state.keySet().stream().anyMatch(s -> state.get(s).containsKey("error"));
        if (mf != null && !hasErrors) {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("; PyLSD input file created by webCASE\n");
            final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
            final Date date = new Date(System.currentTimeMillis());
            stringBuilder.append("; ").append(formatter.format(date)).append("\n\n");

            final Map<String, Integer> elementCounts = new LinkedHashMap<>(Utils.getMolecularFormulaElementCounts(mf));
            // index in correlation data -> [atom type, index in PyLSD file]
            final Map<Integer, Object[]> elementIndices = new HashMap<>();

            // FORM
            stringBuilder.append("; Molecular Formula: ").append(mf).append("\n");
            stringBuilder.append("FORM ");
            elementCounts.forEach((elem, count) -> stringBuilder.append(elem).append(" ").append(count).append(" "));
            stringBuilder.append("\n\n");

            // PIEC
            stringBuilder.append("PIEC 1").append("\n\n");

            Correlation correlation;
            StringBuilder hybridizationStringBuilder;

            // MULT
            final Map<String, String> nucleiMap = new HashMap<>();
            nucleiMap.put("C", "13C");
            nucleiMap.put("N", "15N");
            nucleiMap.put("H", "1H");

            // valid strings from LSD webpage: C N N5 O S S4 S6 F Cl Br I P P5 Si B X
            final Map<String, String> defaultHybridization = new HashMap<>();
            defaultHybridization.put("C", "(1 2 3)");
            defaultHybridization.put("N", "(1 2 3)");
            defaultHybridization.put("O", "(2 3)");
            defaultHybridization.put("S", "(1 2 3)");

            List<Integer> detectedHybridizations;
            final Map<String, String> defaultProtonsCountPerValency = new HashMap<>();
            defaultProtonsCountPerValency.put("C", "(0 1 2 3)");
            defaultProtonsCountPerValency.put("N", "(0 1 2)");
            defaultProtonsCountPerValency.put("N5", "(0 1 2 3)");
            defaultProtonsCountPerValency.put("N35", "(0 1 2 3)");
            defaultProtonsCountPerValency.put("S", "(0 1)");
            defaultProtonsCountPerValency.put("S4", "(0 1 2 3)");
            defaultProtonsCountPerValency.put("S6", "(0 1 2 3)");
            defaultProtonsCountPerValency.put("S246", "(0 1 2 3)");
            defaultProtonsCountPerValency.put("O", "(0 1)");

            final Map<String, String> defaultAtomLabel = new HashMap<>();
            defaultAtomLabel.put("C", "C");
            defaultAtomLabel.put("N", "N35");
            defaultAtomLabel.put("O", "O");
            defaultAtomLabel.put("S", "S246");

            StringBuilder attachedProtonsCountStringBuilder;

            // init element indices within correlations with same order as in correlation data input
            final int totalHeavyAtomsCount = elementCounts.entrySet().stream().filter(set -> !set.getKey().equals("H")).map(Map.Entry::getValue).reduce(0, Integer::sum);
            int heavyAtomIndexInPyLSDFile = 1;
            int protonIndexInPyLSDFile = totalHeavyAtomsCount + 1;
            int protonsToInsert;
            for (int i = 0; i < data.getCorrelations().getValues().size(); i++) {
                correlation = data.getCorrelations().getValues().get(i);
                // set entry for each correlation with consideration of equivalences
                if (correlation.getAtomType().equals("H")) {
                    protonsToInsert = 1;
                    for (final Link link : correlation.getLink()) {
                        if (link.getExperimentType().equals("hsqc")) {
                            protonsToInsert += data.getCorrelations().getValues().get(link.getMatch().get(0)).getEquivalence();
                        }
                    }
                    elementIndices.put(i, new Object[1 + protonsToInsert]);
                    elementIndices.get(i)[0] = correlation.getAtomType();
                    for (int j = 0; j < protonsToInsert; j++) {
                        elementIndices.get(i)[1 + j] = protonIndexInPyLSDFile;
                        protonIndexInPyLSDFile++;
                    }
                } else {
                    elementIndices.put(i, new Object[1 + correlation.getEquivalence() + 1]);
                    elementIndices.get(i)[0] = correlation.getAtomType();
                    for (int j = 1; j <= correlation.getEquivalence() + 1; j++) {
                        elementIndices.get(i)[j] = heavyAtomIndexInPyLSDFile;
                        heavyAtomIndexInPyLSDFile++;
                    }
                }

            }

            // build MULT section
            for (int i = 0; i < data.getCorrelations().getValues().size(); i++) {
                correlation = data.getCorrelations().getValues().get(i);
                if (correlation.getAtomType().equals("H")) {
                    continue;
                }
                detectedHybridizations = new ArrayList<>();
                if (correlation.getHybridization() != null && !correlation.getHybridization().isEmpty()) {
                    // if hybridization is already given
                    if (correlation.getHybridization().equals("SP")) {
                        detectedHybridizations.add(1);
                    } else if (correlation.getHybridization().equals("SP2")) {
                        detectedHybridizations.add(2);
                    } else {
                        detectedHybridizations.add(3);
                    }
                } else {
                    // if hybridization is not given then try to detect via MongoDB query
                    if (correlation.getAtomType().equals("C") && correlation.getProtonsCount().size() == 1) {
                        String multiplicity;
                        switch (correlation.getProtonsCount().get(0)) {
                            case 0:
                                multiplicity = "s";
                                break;
                            case 1:
                                multiplicity = "d";
                                break;
                            case 2:
                                multiplicity = "t";
                                break;
                            case 3:
                                multiplicity = "q";
                                break;
                            default:
                                multiplicity = "";
                                break;
                        }
                        detectedHybridizations = this.detectHybridization(nucleiMap.get(correlation.getAtomType()), (int) correlation.getSignal().getDelta(), multiplicity, thrsHybridizations);
                    }
                }
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
                // set attached protons count
                attachedProtonsCountStringBuilder = new StringBuilder();
                // if protons count is given
                if (correlation.getProtonsCount() != null && !correlation.getProtonsCount().isEmpty()) {
                    if (correlation.getProtonsCount().size() == 1) {
                        attachedProtonsCountStringBuilder.append(correlation.getProtonsCount().get(0));
                    } else {
                        attachedProtonsCountStringBuilder.append("(");
                        for (final int protonsCount : correlation.getProtonsCount()) {
                            attachedProtonsCountStringBuilder.append(protonsCount).append(" ");
                        }
                        attachedProtonsCountStringBuilder.deleteCharAt(attachedProtonsCountStringBuilder.length() - 1);
                        attachedProtonsCountStringBuilder.append(")");
                    }
                } else { // if protons count is not given then set it to default value
                    attachedProtonsCountStringBuilder.append(defaultProtonsCountPerValency.get(defaultAtomLabel.get(correlation.getAtomType())));
                }

                for (int j = 1; j < elementIndices.get(i).length; j++) {
                    stringBuilder.append("MULT ").append(elementIndices.get(i)[j]).append(" ").append(correlation.getAtomType()).append(" ").append(hybridizationStringBuilder.toString()).append(" ").append(attachedProtonsCountStringBuilder.toString()).append("\n");
                }
            }
            stringBuilder.append("\n");

            // HSQC
            for (int i = 0; i < data.getCorrelations().getValues().size(); i++) {
                correlation = data.getCorrelations().getValues().get(i);
                if (correlation.getAtomType().equals("H")) {
                    continue;
                }
                for (final Link link : correlation.getLink()) {
                    if (link.getExperimentType().equals("hsqc")) {
                        for (final int matchIndex : link.getMatch()) {
                            // for each equivalence of heavy atom and attached equivalent proton
                            for (int k = 1; k < elementIndices.get(i).length; k++) {
                                stringBuilder.append("HSQC ").append(elementIndices.get(i)[k]).append(" ").append(elementIndices.get(matchIndex)[k]).append("\n");
                            }
                        }
                    }
                }
            }
            stringBuilder.append("\n");

            // HMBC
            final String defaultBondDistance = "2 4";
            for (int i = 0; i < data.getCorrelations().getValues().size(); i++) {
                correlation = data.getCorrelations().getValues().get(i);
                if (correlation.getAtomType().equals("H")) {
                    continue;
                }
                for (final Link link : correlation.getLink()) {
                    if (link.getExperimentType().equals("hmbc")) {
                        for (final int matchIndex : link.getMatch()) {
                            for (int k = 1; k < elementIndices.get(i).length; k++) {
                                for (int l = 1; l < elementIndices.get(matchIndex).length; l++) {
                                    // only add an HMBC correlation if there is no direct link via HSQC and the equivalence index is not equal
                                    if (!(data.getCorrelations().getValues().get(matchIndex).getAttachment().containsKey(correlation.getAtomType()) && data.getCorrelations().getValues().get(matchIndex).getAttachment().get(correlation.getAtomType()).contains(i) && l == k)) {
                                        stringBuilder.append("HMBC ").append(elementIndices.get(i)[k]).append(" ").append(elementIndices.get(matchIndex)[l]).append(" ").append(defaultBondDistance).append("\n");
                                    }
                                }
                            }
                        }
                    }
                }
            }

            stringBuilder.append("\n");

            // COSY
            for (int i = 0; i < data.getCorrelations().getValues().size(); i++) {
                correlation = data.getCorrelations().getValues().get(i);
                if (!correlation.getAtomType().equals("H")) {
                    continue;
                }

                for (final Link link : correlation.getLink()) {
                    if (link.getExperimentType().equals("cosy")) {
                        for (final int matchIndex : link.getMatch()) {
                            // only add an COSY correlation if the two signals there is not equivalent
                            if (!data.getCorrelations().getValues().get(matchIndex).getId().equals(correlation.getId())) {
                                for (int k = 1; k < elementIndices.get(i).length; k++) {
                                    for (int l = 1; l < elementIndices.get(matchIndex).length; l++) {
                                        stringBuilder.append("COSY ").append(elementIndices.get(i)[k]).append(" ").append(elementIndices.get(matchIndex)[l]).append("\n");
                                    }
                                }
                            }
                        }
                    }
                }
            }
            stringBuilder.append("\n");

            // BOND (interpretation, INADEQUATE, previous assignments) -> input fragments

            // LIST PROP for hetero hetero bonds allowance
            if (!allowHeteroHeteroBonds) {
                // create hetero atom list automatically
                stringBuilder.append("HETE L1").append("; list of hetero atoms\n");
                stringBuilder.append("PROP L1 0 L1 -; no hetero-hetero bonds\n");

                stringBuilder.append("\n");
            }

            // SHIX / SHIH
            for (int i = 0; i < data.getCorrelations().getValues().size(); i++) {
                correlation = data.getCorrelations().getValues().get(i);
                if (correlation.getAtomType().equals("H") || correlation.isPseudo()) {
                    continue;
                }
                for (int k = 1; k < elementIndices.get(i).length; k++) {
                    stringBuilder.append("SHIX ").append(elementIndices.get(i)[k]).append(" ").append(correlation.getSignal().getDelta()).append("\n");
                }
            }
            stringBuilder.append("\n");
            for (int i = 0; i < data.getCorrelations().getValues().size(); i++) {
                correlation = data.getCorrelations().getValues().get(i);
                if (!correlation.getAtomType().equals("H") || correlation.isPseudo()) {
                    continue;
                }
                // only consider protons which are attached via HSQC/HMQC (pseudo and real links)
                for (final Link link : correlation.getLink()) {
                    if ((link.getExperimentType().equals("hsqc") || link.getExperimentType().equals("hmqc")) && !link.getMatch().isEmpty()) { // && !link.isPseudo()
                        for (int k = 1; k < elementIndices.get(i).length; k++) {
                            stringBuilder.append("SHIH ").append(elementIndices.get(i)[k]).append(" ").append(correlation.getSignal().getDelta()).append("\n");
                        }
                    }
                }
            }
            stringBuilder.append("\n");

            // DEFF + FEXP -> add filters
            stringBuilder.append("; externally defined filters\n");
            final Map<String, String> filters = new LinkedHashMap<>();
            int counter = 1;
            try {
                final FileReader fileReader = new FileReader(PATH_TO_LSD_FILTER_LIST);
                final BufferedReader bufferedReader = new BufferedReader(fileReader);
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    filters.put("F" + counter, line);
                    counter++;
                }
                fileReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

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

            //            System.out.println(stringBuilder.toString());

            return stringBuilder.toString();
        }

        return "";
    }
}
