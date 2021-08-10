package org.openscience.webcase.dbservice.hosecode.utils;

import casekit.nmr.analysis.MultiplicitySectionsBuilder;
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
import org.openscience.cdk.fingerprint.BitSetFingerprint;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.webcase.dbservice.hosecode.controller.HOSECodeController;
import org.openscience.webcase.dbservice.hosecode.model.exchange.Transfer;
import org.openscience.webcase.dbservice.hosecode.service.model.HOSECode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ResultsParser {

    //    private final String[] solvents = new String[]{"Chloroform-D1 (CDCl3)", "Methanol-D4 (CD3OD)",
    //                                                   "Dimethylsulphoxide-D6 (DMSO-D6, C2D6SO)", "Unreported", "Unknown"};
    private final MultiplicitySectionsBuilder multiplicitySectionsBuilder = new MultiplicitySectionsBuilder();

    private final HOSECodeController hoseCodeController;

    @Autowired
    public ResultsParser(final HOSECodeController hoseCodeController) {
        this.hoseCodeController = hoseCodeController;
    }

    public List<DataSet> parseAndPredict(final Transfer requestTransfer,
                                         final Map<String, int[]> multiplicitySectionsSettings) {
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
        final double maxAverageDeviation = requestTransfer.getElucidationOptions()
                                                          .getMaxAverageDeviation();
        final List<Correlation> correlationsAtomType = data.getCorrelations()
                                                           .getValues()
                                                           .stream()
                                                           .filter(correlation -> correlation.getAtomType()
                                                                                             .equals(atomType)
                                                                   && !correlation.isPseudo())
                                                           .collect(Collectors.toList());


        final Spectrum querySpectrum = new Spectrum();
        querySpectrum.setNuclei(new String[]{nucleus});
        querySpectrum.setSignals(new ArrayList<>());
        Signal signal;
        for (final Correlation correlation : correlationsAtomType) {
            signal = new Signal();
            signal.setNuclei(querySpectrum.getNuclei());
            signal.setShifts(new Double[]{correlation.getSignal().getDelta()});
            signal.setMultiplicity(Utils.getMultiplicityFromProtonsCount(correlation));
            signal.setEquivalencesCount(correlation.getEquivalence());
            querySpectrum.addSignalWithoutEquivalenceSearch(signal);
        }

        this.multiplicitySectionsBuilder.setMinLimit(multiplicitySectionsSettings.get(nucleus)[0]);
        this.multiplicitySectionsBuilder.setMaxLimit(multiplicitySectionsSettings.get(nucleus)[1]);
        this.multiplicitySectionsBuilder.setStepSize(multiplicitySectionsSettings.get(nucleus)[2]);

        final BitSetFingerprint bitSetFingerprintQuerySpectrum = Similarity.getBitSetFingerprint(querySpectrum, 0,
                                                                                                 this.multiplicitySectionsBuilder);

        IAtomContainer structure;
        Spectrum predictedSpectrum;
        Assignment assignment, matchAssignment;
        HOSECode hoseCodeObject;
        double predictedShift;
        String hoseCode;
        Double[] statistics, deviations;
        Double rmsd, averageDeviation, tanimotoCoefficient;
        int signalIndex, sphere;
        Map<Integer, List<Integer>> assignmentMap;
        List<Double> medians;
        BitSetFingerprint bitSetFingerprintDataSet;
        try {
            for (final DataSet dataSet : requestDataSetList) {
                structure = dataSet.getStructure()
                                   .toAtomContainer();
                //                // convert implicit to explicit hydrogens for building HOSE codes and lookup in HOSE code DB
                //                Utils.convertImplicitToExplicitHydrogens(structure);
                //                Utils.setAromaticityAndKekulize(structure);

                predictedSpectrum = new Spectrum();
                predictedSpectrum.setNuclei(querySpectrum.getNuclei());
                predictedSpectrum.setSignals(new ArrayList<>());

                assignmentMap = new HashMap<>();
                for (int i = 0; i
                        < structure.getAtomCount(); i++) {
                    if (!structure.getAtom(i)
                                  .getSymbol()
                                  .equals(atomType)) {
                        continue;
                    }

                    //                    statistics = null;
                    medians = new ArrayList<>();
                    sphere = maxSphere;
                    while (sphere
                            >= 1) {
                        hoseCode = HOSECodeBuilder.buildHOSECode(structure, i, sphere, false);
                        hoseCodeObject = this.hoseCodeController.getByID(hoseCode) //getByHOSECode(hoseCode)
                                                                .block();
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
                                medians.add(statistics[3]);
                            }
                            break;
                        }
                        sphere--;
                    }

                    if (!medians.isEmpty()) {
                        predictedShift = Statistics.getMean(medians);
                    } else {
                        predictedShift = 1000;
                    }
                    signal = new Signal();
                    signal.setNuclei(querySpectrum.getNuclei());
                    signal.setShifts(new Double[]{predictedShift});
                    signal.setMultiplicity(Utils.getMultiplicityFromProtonsCount(structure.getAtom(i)
                                                                                          .getImplicitHydrogenCount()));
                    signal.setEquivalencesCount(1);

                    signalIndex = predictedSpectrum.addSignal(signal);

                    assignmentMap.putIfAbsent(signalIndex, new ArrayList<>());
                    assignmentMap.get(signalIndex)
                                 .add(i);
                }

                // if no spectrum could be built or the number of signals in spectrum is different than the atom number in molecule
                try {
                    if (Utils.getDifferenceSpectrumSizeAndMolecularFormulaCount(predictedSpectrum,
                                                                                Utils.getMolecularFormulaFromString(
                                                                                        dataSet.getMeta()
                                                                                               .get("mf")), 0)
                            != 0) {
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

                dataSet.setSpectrum(predictedSpectrum);
                dataSet.setAssignment(assignment);

                dataSet.addMetaInfo("querySpectrumSignalCount", String.valueOf(querySpectrum.getSignalCount()));
                dataSet.addMetaInfo("querySpectrumSignalCountWithEquivalences",
                                    String.valueOf(querySpectrum.getSignalCountWithEquivalences()));
                matchAssignment = Similarity.matchSpectra(querySpectrum, predictedSpectrum, 0, 0, 50, true, true,
                                                          false);
                dataSet.addMetaInfo("setAssignmentsCount", String.valueOf(matchAssignment.getSetAssignmentsCount(0)));
                dataSet.addMetaInfo("setAssignmentsCountWithEquivalences",
                                    String.valueOf(matchAssignment.getSetAssignmentsCountWithEquivalences(0)));
                dataSet.addMetaInfo("isCompleteSpectralMatch", String.valueOf(querySpectrum.getSignalCount()
                                                                                      == matchAssignment.getSetAssignmentsCount(
                        0)));
                dataSet.addMetaInfo("isCompleteSpectralMatchWithEquivalences", String.valueOf(
                        querySpectrum.getSignalCountWithEquivalences()
                                == matchAssignment.getSetAssignmentsCountWithEquivalences(0)));
                deviations = Similarity.getDeviations(querySpectrum, predictedSpectrum, 0, 0, matchAssignment);
                averageDeviation = Statistics.calculateAverageDeviation(deviations);
                if (averageDeviation
                        != null) {
                    if (averageDeviation
                            <= maxAverageDeviation) {
                        dataSet.addMetaInfo("averageDeviation", String.valueOf(averageDeviation));
                        rmsd = Statistics.calculateRMSD(deviations);
                        dataSet.addMetaInfo("rmsd", String.valueOf(rmsd));
                        bitSetFingerprintDataSet = Similarity.getBitSetFingerprint(dataSet.getSpectrum(), 0,
                                                                                   this.multiplicitySectionsBuilder);
                        tanimotoCoefficient = Similarity.calculateTanimotoCoefficient(bitSetFingerprintQuerySpectrum,
                                                                                      bitSetFingerprintDataSet);
                        dataSet.addMetaInfo("tanimoto", String.valueOf(tanimotoCoefficient));

                        dataSetList.add(dataSet);
                    }
                } else {
                    deviations = Arrays.stream(deviations)
                                       .filter(Objects::nonNull)
                                       .toArray(Double[]::new);
                    averageDeviation = Statistics.calculateAverageDeviation(deviations);
                    if (averageDeviation
                            <= maxAverageDeviation) {
                        dataSet.addMetaInfo("averageDeviation", String.valueOf(averageDeviation));
                        rmsd = Statistics.calculateRMSD(deviations);
                        dataSet.addMetaInfo("rmsd", String.valueOf(rmsd));
                        bitSetFingerprintDataSet = Similarity.getBitSetFingerprint(dataSet.getSpectrum(), 0,
                                                                                   this.multiplicitySectionsBuilder);
                        tanimotoCoefficient = Similarity.calculateTanimotoCoefficient(bitSetFingerprintQuerySpectrum,
                                                                                      bitSetFingerprintDataSet);
                        dataSet.addMetaInfo("tanimoto", String.valueOf(tanimotoCoefficient));

                        dataSetList.add(dataSet);
                    }
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return dataSetList;
    }
}
