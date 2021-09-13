package org.openscience.webcase.dbservice.hosecode.utils;

import casekit.nmr.analysis.MultiplicitySectionsBuilder;
import casekit.nmr.hose.HOSECodeBuilder;
import casekit.nmr.model.Assignment;
import casekit.nmr.model.DataSet;
import casekit.nmr.model.Signal;
import casekit.nmr.model.Spectrum;
import casekit.nmr.model.nmrium.Correlation;
import casekit.nmr.model.nmrium.Data;
import casekit.nmr.similarity.Similarity;
import casekit.nmr.utils.Statistics;
import casekit.nmr.utils.Utils;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.BitSetFingerprint;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
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
    private final Map<String, HOSECode> hoseCodeDBEntriesMap;

    @Autowired
    public ResultsParser(final HOSECodeController hoseCodeController,
                         final Map<String, HOSECode> hoseCodeDBEntriesMap) {
        this.hoseCodeController = hoseCodeController;
        this.hoseCodeDBEntriesMap = hoseCodeDBEntriesMap;

        this.fillHOSECodeDBEntriesMap();
    }

    public void clearHOSECodeDBEntriesMap() {
        System.out.println("\nclearHOSECodeDBEntriesMap...");
        this.hoseCodeDBEntriesMap.clear();
    }

    public void fillHOSECodeDBEntriesMap() {
        System.out.println("\nloading DB content...");
        final List<HOSECode> hoseCodeList = this.hoseCodeController.getAll()
                                                                   .collectList()
                                                                   .block();
        if (hoseCodeList
                != null) {
            for (final HOSECode hoseCodeObject : hoseCodeList) {
                this.hoseCodeDBEntriesMap.put(hoseCodeObject.getHOSECode(), hoseCodeObject);
            }
        }

        System.out.println(" -> "
                                   + this.hoseCodeDBEntriesMap.size());
    }

    public List<DataSet> parseAndPredict(final Transfer requestTransfer,
                                         final Map<String, int[]> multiplicitySectionsSettings) {
        // @TODO method modifications for different nuclei and solvent needed
        final String nucleus = "13C";
        final String atomType = Utils.getAtomTypeFromNucleus(nucleus);

        //        final String solvent = this.solvents[0];
        final int maxSphere = 6;

        final List<String> smilesList = requestTransfer.getSmilesList();
        final List<DataSet> dataSetList = new ArrayList<>();
        System.out.println(" ---> requestSMILES: "
                                   + smilesList.size());
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

        final SmilesParser smilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        DataSet dataSet;
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
        int counter = 1;
        try {
            for (final String smiles : smilesList) {
                structure = smilesParser.parseSmiles(smiles);
                dataSet = Utils.atomContainerToDataSet(structure);
                //                // convert implicit to explicit hydrogens for building HOSE codes and lookup in HOSE code DB
                //                Utils.convertImplicitToExplicitHydrogens(structure);
                //                Utils.setAromaticityAndKekulize(structure);

                predictedSpectrum = new Spectrum();
                predictedSpectrum.setNuclei(querySpectrum.getNuclei());
                predictedSpectrum.setSignals(new ArrayList<>());

                System.out.println("\nprediction for: "
                                           + counter
                                           + "/"
                                           + smilesList.size());
                assignmentMap = new HashMap<>();
                for (int i = 0; i
                        < structure.getAtomCount(); i++) {
                    if (!structure.getAtom(i)
                                  .getSymbol()
                                  .equals(atomType)) {
                        continue;
                    }
                    medians = new ArrayList<>();
                    sphere = maxSphere;
                    while (sphere
                            >= 1) {
                        hoseCode = HOSECodeBuilder.buildHOSECode(structure, i, sphere, false);
                        //                        hoseCodeObject = this.hoseCodeController.getByID(hoseCode)
                        //                                                                .block();
                        hoseCodeObject = this.hoseCodeDBEntriesMap.get(hoseCode);
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
                    if (medians.isEmpty()) {
                        continue;
                    }
                    predictedShift = Statistics.getMean(medians);
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

                // to save space and time when (re-)converting datasets avoid the currently non-used information
                // the SMILES was build by CDK and stored in meta information
                dataSet.setStructure(null);
                //                dataSet.setSpectrum(new SpectrumCompact(predictedSpectrum));
                //                assignment = new Assignment();
                //                assignment.setNuclei(predictedSpectrum.getNuclei());
                //                assignment.initAssignments(predictedSpectrum.getSignalCount());
                //
                //                for (final Map.Entry<Integer, List<Integer>> entry : assignmentMap.entrySet()) {
                //                    for (final int atomIndex : assignmentMap.get(entry.getKey())) {
                //                        assignment.addAssignmentEquivalence(0, entry.getKey(), atomIndex);
                //                    }
                //                }
                //                dataSet.setAssignment(assignment);

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
                        bitSetFingerprintDataSet = Similarity.getBitSetFingerprint(predictedSpectrum, 0,
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
                        bitSetFingerprintDataSet = Similarity.getBitSetFingerprint(predictedSpectrum, 0,
                                                                                   this.multiplicitySectionsBuilder);
                        tanimotoCoefficient = Similarity.calculateTanimotoCoefficient(bitSetFingerprintQuerySpectrum,
                                                                                      bitSetFingerprintDataSet);
                        dataSet.addMetaInfo("tanimoto", String.valueOf(tanimotoCoefficient));

                        dataSetList.add(dataSet);
                    }
                }
                counter++;
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return dataSetList;
    }
}
