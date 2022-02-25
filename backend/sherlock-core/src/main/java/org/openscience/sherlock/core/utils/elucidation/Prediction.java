package org.openscience.sherlock.core.utils.elucidation;

import casekit.nmr.analysis.MultiplicitySectionsBuilder;
import casekit.nmr.model.*;
import casekit.nmr.utils.Parser;
import casekit.nmr.utils.Statistics;
import casekit.nmr.utils.Utils;
import casekit.threading.MultiThreading;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.nmrshiftdb.util.AtomUtils;
import org.openscience.nmrshiftdb.util.ExtendedHOSECodeGenerator;
import org.openscience.sherlock.core.model.exchange.Transfer;
import org.openscience.sherlock.core.utils.FilterAndRank;
import org.openscience.sherlock.core.utils.Utilities;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class Prediction {
    private static final MultiplicitySectionsBuilder multiplicitySectionsBuilder = new MultiplicitySectionsBuilder();

    public static ResponseEntity<Transfer> parseAndPredictFromSmilesFile(final Transfer requestTransfer,
                                                                         final Map<String, Map<String, Double[]>> hoseCodeDBEntriesMap,
                                                                         final WebClient.Builder webClientBuilder,
                                                                         final ExchangeStrategies exchangeStrategies) {
        final Transfer responseTransfer = new Transfer();
        try {
            requestTransfer.setSmilesList(Parser.smilesFileToList(requestTransfer.getPathToSmilesFile()));
            try {
                final List<DataSet> dataSetList = Prediction.predictAndFilter(requestTransfer, hoseCodeDBEntriesMap,
                                                                              Objects.requireNonNull(
                                                                                      Utilities.getMultiplicitySectionsSettings(
                                                                                                       webClientBuilder,
                                                                                                       exchangeStrategies)
                                                                                               .block()));
                responseTransfer.setDataSetList(dataSetList);
            } catch (final Exception e) {
                responseTransfer.setErrorMessage(e.getMessage());
                return new ResponseEntity<>(responseTransfer, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (final FileNotFoundException e) {
            //            System.out.println("--> could not parse SMILES file: "
            //                                       + requestTransfer.getPathToSmilesFile());
            responseTransfer.setDataSetList(new ArrayList<>());
        }

        return new ResponseEntity<>(responseTransfer, HttpStatus.OK);
    }

    public static List<DataSet> predictAndFilter(final Transfer requestTransfer,
                                                 final Map<String, Map<String, Double[]>> hoseCodeDBEntriesMap,
                                                 final Map<String, int[]> multiplicitySectionsSettings) {
        // @TODO method modifications for different nuclei and solvent needed
        final String nucleus = "13C";
        final int maxSphere = 6;
        final List<String> smilesList = requestTransfer.getSmilesList();
        //        System.out.println("-----> requestSMILES: "
        //                                   + smilesList.size());
        final Spectrum querySpectrum = Utils.correlationListToSpectrum1D(requestTransfer.getCorrelations()
                                                                                        .getValues(), nucleus);
        multiplicitySectionsBuilder.setMinLimit(multiplicitySectionsSettings.get(nucleus)[0]);
        multiplicitySectionsBuilder.setMaxLimit(multiplicitySectionsSettings.get(nucleus)[1]);
        multiplicitySectionsBuilder.setStepSize(multiplicitySectionsSettings.get(nucleus)[2]);

        final SmilesParser smilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        List<DataSet> dataSetList = new ArrayList<>();
        try {
            final ConcurrentLinkedQueue<DataSet> dataSetConcurrentLinkedQueue = new ConcurrentLinkedQueue<>();
            final List<Callable<DataSet>> callables = new ArrayList<>();
            for (final String smiles : smilesList) {
                callables.add(() -> predictAndFilter(smilesParser.parseSmiles(smiles), querySpectrum, maxSphere,
                                                     requestTransfer.getElucidationOptions()
                                                                    .getShiftTolerance(),
                                                     requestTransfer.getElucidationOptions()
                                                                    .getMaxAverageDeviation(), true, true,
                                                     hoseCodeDBEntriesMap));
            }
            final Consumer<DataSet> consumer = (dataSet) -> {
                if (dataSet
                        != null) {
                    dataSetConcurrentLinkedQueue.add(dataSet);
                }
            };
            MultiThreading.processTasks(callables, consumer, 2, 5);
            dataSetList = new ArrayList<>(dataSetConcurrentLinkedQueue);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return dataSetList;
    }

    private static DataSet predictAndFilter(final IAtomContainer structure, final Spectrum querySpectrum,
                                            final int maxSphere, final double shiftTolerance,
                                            final double maxAverageDeviation, final boolean checkMultiplicity,
                                            final boolean checkEquivalencesCount,
                                            final Map<String, Map<String, Double[]>> hoseCodeDBEntriesMap) {
        final String nucleus = querySpectrum.getNuclei()[0];
        final String atomType = Utils.getAtomTypeFromNucleus(nucleus);
        final StructureDiagramGenerator structureDiagramGenerator = new StructureDiagramGenerator();
        final ExtendedHOSECodeGenerator extendedHOSECodeGenerator = new ExtendedHOSECodeGenerator();

        final Assignment assignment;
        Signal signal;
        Map<String, Double[]> hoseCodeObjectValues;
        double predictedShift;
        String hoseCode;
        Double[] statistics;
        int signalIndex, sphere;
        List<Double> medians;

        try {
            // set 2D coordinates
            structureDiagramGenerator.setMolecule(structure);
            structureDiagramGenerator.generateCoordinates(structure);
            /* !!! No explicit H in mol !!! */
            Utils.convertExplicitToImplicitHydrogens(structure);
            /* add explicit H atoms */
            AtomUtils.addAndPlaceHydrogens(structure);
            /* detect aromaticity */
            Utils.setAromaticityAndKekulize(structure);

            final DataSet dataSet = Utils.atomContainerToDataSet(structure, false);

            final Spectrum predictedSpectrum = new Spectrum();
            predictedSpectrum.setNuclei(querySpectrum.getNuclei());
            predictedSpectrum.setSignals(new ArrayList<>());

            final Map<Integer, List<Integer>> assignmentMap = new HashMap<>();
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
                    try {
                        hoseCode = extendedHOSECodeGenerator.getHOSECode(structure, structure.getAtom(i), sphere);
                        hoseCodeObjectValues = hoseCodeDBEntriesMap.get(hoseCode);
                        if (hoseCodeObjectValues
                                != null) {
                            for (final Map.Entry<String, Double[]> solventEntry : hoseCodeObjectValues.entrySet()) {
                                statistics = hoseCodeObjectValues.get(solventEntry.getKey());
                                medians.add(statistics[3]);
                            }
                            break;
                        }
                    } catch (final Exception ignored) {
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
                signal.setMultiplicity(Utils.getMultiplicityFromProtonsCount(
                        AtomUtils.getHcount(structure, structure.getAtom(i)))); // counts explicit H
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
                    return null;
                }
            } catch (final CDKException e) {
                e.printStackTrace();
                return null;
            }


            Utils.convertExplicitToImplicitHydrogens(structure);
            dataSet.setStructure(new StructureCompact(structure));
            dataSet.addMetaInfo("smiles", SmilesGenerator.generic()
                                                         .create(structure));

            dataSet.setSpectrum(new SpectrumCompact(predictedSpectrum));
            assignment = new Assignment();
            assignment.setNuclei(predictedSpectrum.getNuclei());
            assignment.initAssignments(predictedSpectrum.getSignalCount());

            for (final Map.Entry<Integer, List<Integer>> entry : assignmentMap.entrySet()) {
                for (final int atomIndex : assignmentMap.get(entry.getKey())) {
                    assignment.addAssignmentEquivalence(0, entry.getKey(), atomIndex);
                }
            }
            dataSet.setAssignment(assignment);

            return FilterAndRank.checkDataSet(dataSet, querySpectrum, shiftTolerance, maxAverageDeviation,
                                              checkMultiplicity, checkEquivalencesCount, multiplicitySectionsBuilder,
                                              true);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
