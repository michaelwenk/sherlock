package org.openscience.webcase.dbservice.hosecode.utils;

import casekit.nmr.hose.HOSECodeBuilder;
import casekit.nmr.model.Assignment;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.Signal;
import casekit.nmr.model.Spectrum;
import casekit.nmr.model.nmrdisplayer.Correlation;
import casekit.nmr.model.nmrdisplayer.Data;
import casekit.nmr.similarity.Similarity;
import casekit.nmr.utils.Statistics;
import casekit.nmr.utils.Utils;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.webcase.dbservice.hosecode.controller.HOSECodeController;
import org.openscience.webcase.dbservice.hosecode.model.exchange.Transfer;
import org.openscience.webcase.dbservice.hosecode.service.model.HOSECode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class PyLSDResultsRanker {

    private final String[] solvents = new String[]{"Chloroform-D1 (CDCl3)", "Methanol-D4 (CD3OD)",
                                                   "Dimethylsulphoxide-D6 (DMSO-D6, C2D6SO)", "Unreported", "Unknown"};

    private final HOSECodeController hoseCodeController;

    @Autowired
    public PyLSDResultsRanker(final HOSECodeController hoseCodeController) {
        this.hoseCodeController = hoseCodeController;
    }

    public List<DataSet> rankPyLSDResults(final Transfer requestTransfer) {
        // @TODO method modifications for different nuclei and solvent needed
        final String nucleus = "13C";
        final String atomType = Utils.getAtomTypeFromNucleus(nucleus);

        //        final String solvent = this.solvents[0];
        final int maxSphere = 6;

        final List<DataSet> requestDataSetList = requestTransfer.getDataSetList();
        final List<DataSet> dataSetList = new ArrayList<>();
        System.out.println(" ---> requestDataSets: "
                                   + requestDataSetList.size());
        final Data data = requestTransfer.getData();
        final double maxAverageDeviation = requestTransfer.getMaxAverageDeviation();
        System.out.println("correlations: "
                                   + data.getCorrelations()
                                         .getValues()
                                         .size());
        System.out.println("maxAverageDeviation: "
                                   + maxAverageDeviation);

        final List<Correlation> correlationsAtomType = data.getCorrelations()
                                                           .getValues()
                                                           .stream()
                                                           .filter(correlation -> correlation.getAtomType()
                                                                                             .equals(atomType)
                                                                   && !correlation.isPseudo())
                                                           .collect(Collectors.toList());


        final Spectrum experimentalSpectrum = new Spectrum();
        experimentalSpectrum.setNuclei(new String[]{nucleus});
        experimentalSpectrum.setSignals(new ArrayList<>());
        Signal signal;
        for (final Correlation correlation : correlationsAtomType) {
            signal = new Signal();
            signal.setNuclei(experimentalSpectrum.getNuclei());
            signal.setShifts(new Double[]{correlation.getSignal().getDelta()});
            signal.setMultiplicity(Utils.getMultiplicityFromProtonsCount(correlation));
            signal.setEquivalencesCount(correlation.getEquivalence());
            experimentalSpectrum.addSignalWithoutEquivalenceSearch(signal);
        }
        System.out.println(" --> experimentalSpectrum: "
                                   + experimentalSpectrum);

        IAtomContainer structure;
        Spectrum predictedSpectrum;
        Assignment assignment;
        HOSECode hoseCodeObject;
        double predictedShift;
        String hoseCode, solvent;
        Double[] statistics, deviations;
        Double rmsd, averageDeviation;
        int signalIndex, sphere;
        Map<Integer, List<Integer>> assignmentMap;
        List<Double> medians;
        try {
            for (final DataSet dataSet : requestDataSetList) {
                structure = dataSet.getStructure()
                                   .toAtomContainer();

                //                // convert implicit to explicit hydrogens for building HOSE codes and lookup in HOSE code DB
                //                Utils.convertImplicitToExplicitHydrogens(structure);
                //                Utils.setAromaticityAndKekulize(structure);


                predictedSpectrum = new Spectrum();
                predictedSpectrum.setNuclei(experimentalSpectrum.getNuclei());
                predictedSpectrum.setSignals(new ArrayList<>());

                assignmentMap = new HashMap<>();
                for (int i = 0; i
                        < structure.getAtomCount(); i++) {
                    if (!structure.getAtom(i)
                                  .getSymbol()
                                  .equals(atomType)) {
                        continue;
                    }

                    statistics = null;
                    medians = new ArrayList<>();
                    sphere = maxSphere;
                    final long start = System.currentTimeMillis();
                    while (sphere
                            >= 1) {
                        hoseCode = HOSECodeBuilder.buildHOSECode(structure, i, sphere, false);

                        System.out.println(" query HOSE code: "
                                                   + hoseCode);

                        final long start2 = System.currentTimeMillis();
                        hoseCodeObject = this.hoseCodeController.getByID(hoseCode) //getByHOSECode(hoseCode)
                                                                .block();

                        final long stop2 = System.currentTimeMillis();
                        System.out.println(" -----> time2: "
                                                   + (stop2
                                - start2));

                        //                        if (hoseCodeObject
                        //                                != null
                        //                                && hoseCodeObject.getValues()
                        //                                                 .containsKey(solvent)) {
                        //                            statistics = hoseCodeObject.getValues()
                        //                                                       .get(solvent);
                        //                            System.out.println(" --> statistics: "
                        //                                                       + Arrays.toString(statistics));
                        //
                        //                            break;
                        //                        }
                        if (hoseCodeObject
                                != null) {
                            for (final Map.Entry<String, Double[]> solventEntry : hoseCodeObject.getValues()
                                                                                                .entrySet()) {
                                statistics = hoseCodeObject.getValues()
                                                           .get(solventEntry.getKey());
                                System.out.println(" --> statistics: "
                                                           + Arrays.toString(statistics));
                                medians.add(statistics[3]);
                            }


                            break;
                        }
                        sphere--;
                    }

                    if (!medians.isEmpty()) {
                        System.out.println(" -> "
                                                   + Arrays.toString(statistics));
                        predictedShift = Statistics.getMean(medians);
                    } else {
                        predictedShift = 1000;
                    }
                    System.out.println(" -> "
                                               + predictedShift);
                    signal = new Signal();
                    signal.setNuclei(experimentalSpectrum.getNuclei());
                    signal.setShifts(new Double[]{predictedShift});
                    signal.setMultiplicity(Utils.getMultiplicityFromProtonsCount(structure.getAtom(i)
                                                                                          .getImplicitHydrogenCount()));
                    signal.setEquivalencesCount(1);

                    signalIndex = predictedSpectrum.addSignal(signal);

                    assignmentMap.putIfAbsent(signalIndex, new ArrayList<>());
                    assignmentMap.get(signalIndex)
                                 .add(i);

                    final long stop = System.currentTimeMillis();
                    System.out.println(" ---------> time total: "
                                               + (stop
                            - start));
                }

                // if no spectrum could be built or the number of signals in spectrum is different than the atom number in molecule
                try {
                    if (Utils.getDifferenceSpectrumSizeAndMolecularFormulaCount(predictedSpectrum,
                                                                                Utils.getMolecularFormulaFromString(
                                                                                        dataSet.getMeta()
                                                                                               .get("mf")), 0)
                            != 0) {
                        System.out.println("\n\n----> getDifferenceSpectrumSizeAndMolecularFormulaCount: false\n\n");
                        continue;
                    }
                } catch (final CDKException e) {
                    e.printStackTrace();
                    continue;
                }

                assignment = new Assignment();
                assignment.setNuclei(predictedSpectrum.getNuclei());
                assignment.initAssignments(predictedSpectrum.getSignalCount());

                for (final Map.Entry<Integer, List<Integer>> entry : assignmentMap.entrySet()) {
                    for (final int atomIndex : assignmentMap.get(entry.getKey())) {
                        assignment.addAssignmentEquivalence(0, entry.getKey(), atomIndex);
                    }
                }

                deviations = Similarity.getDeviations(experimentalSpectrum, predictedSpectrum, 0, 0, 50, true, true,
                                                      false);


                dataSet.setSpectrum(predictedSpectrum);
                dataSet.setAssignment(assignment);

                System.out.println(" --> experimentalSpectrum: "
                                           + experimentalSpectrum.getSignals());
                System.out.println(" --> predictedSpectrum: "
                                           + dataSet.getSpectrum()
                                                    .getSignals());
                System.out.println(" --> deviations: "
                                           + Arrays.deepToString(deviations));
                System.out.println(" --> assignments: "
                                           + Arrays.deepToString(dataSet.getAssignment()
                                                                        .getAssignments(0)));

                averageDeviation = Statistics.calculateAverageDeviation(deviations);
                System.out.println(" --> averageDeviation: "
                                           + averageDeviation);
                if (averageDeviation
                        != null
                        && averageDeviation
                        <= maxAverageDeviation) {
                    dataSet.addMetaInfo("averageDeviation", String.valueOf(averageDeviation));
                    rmsd = Statistics.calculateRMSD(deviations);
                    dataSet.addMetaInfo("rmsd", String.valueOf(rmsd));

                    System.out.println(" --> rmsd: "
                                               + rmsd);

                    dataSetList.add(dataSet);
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }


        // pre-sort by RMSD value
        dataSetList.sort((dataSet1, dataSet2) -> {
            if (Double.parseDouble(dataSet1.getMeta()
                                           .get("rmsd"))
                    < Double.parseDouble(dataSet2.getMeta()
                                                 .get("rmsd"))) {
                return -1;
            } else if (Double.parseDouble(dataSet1.getMeta()
                                                  .get("rmsd"))
                    > Double.parseDouble(dataSet2.getMeta()
                                                 .get("rmsd"))) {
                return 1;
            }
            return 0;
        });

        return dataSetList;
    }
}
