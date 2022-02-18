package org.openscience.sherlock.pylsd.utils;

import casekit.nmr.analysis.MultiplicitySectionsBuilder;
import casekit.nmr.hose.HOSECodeBuilder;
import casekit.nmr.model.*;
import casekit.nmr.similarity.Similarity;
import casekit.nmr.utils.Statistics;
import casekit.nmr.utils.Utils;
import casekit.threading.MultiThreading;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.BitSetFingerprint;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.MDLV3000Writer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.sherlock.pylsd.model.db.HOSECode;
import org.openscience.sherlock.pylsd.model.exchange.Transfer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class Prediction {

    //    private final String[] solvents = new String[]{"Chloroform-D1 (CDCl3)", "Methanol-D4 (CD3OD)",
    //                                                   "Dimethylsulphoxide-D6 (DMSO-D6, C2D6SO)", "Unreported", "Unknown"};
    private static final MultiplicitySectionsBuilder multiplicitySectionsBuilder = new MultiplicitySectionsBuilder();

    private static Mono<HOSECode> getHOSECodeByID(final WebClient.Builder webClientBuilder,
                                                  final ExchangeStrategies exchangeStrategies, final String hoseCode) {
        final WebClient webClient = webClientBuilder.baseUrl(
                                                            "http://sherlock-gateway:8080/sherlock-db-service-hosecode/")
                                                    .defaultHeader(HttpHeaders.CONTENT_TYPE,
                                                                   MediaType.APPLICATION_JSON_VALUE)
                                                    .exchangeStrategies(exchangeStrategies)
                                                    .build();
        final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
        uriComponentsBuilder.path("/getByID")
                            .queryParam("id", hoseCode);
        return webClient.get()
                        .uri(uriComponentsBuilder.toUriString())
                        .retrieve()
                        .bodyToMono(HOSECode.class);
    }

    public static List<DataSet> predict(final Transfer requestTransfer,
                                        final Map<String, Map<String, Double[]>> hoseCodeDBEntriesMap,
                                        final Map<String, int[]> multiplicitySectionsSettings) {
        // @TODO method modifications for different nuclei and solvent needed
        final String nucleus = "13C";
        final int maxSphere = 6;
        final boolean keepDataSetMetaOnly = false;
        final List<String> smilesList = requestTransfer.getSmilesList();
        //        System.out.println("-----> requestSMILES: "
        //                                   + smilesList.size());
        final Spectrum querySpectrum = Utils.correlationListToSpectrum1D(requestTransfer.getCorrelations()
                                                                                        .getValues(), nucleus);
        multiplicitySectionsBuilder.setMinLimit(multiplicitySectionsSettings.get(nucleus)[0]);
        multiplicitySectionsBuilder.setMaxLimit(multiplicitySectionsSettings.get(nucleus)[1]);
        multiplicitySectionsBuilder.setStepSize(multiplicitySectionsSettings.get(nucleus)[2]);

        final BitSetFingerprint bitSetFingerprintQuerySpectrum = Similarity.getBitSetFingerprint(querySpectrum, 0,
                                                                                                 multiplicitySectionsBuilder);

        final SmilesParser smilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        List<DataSet> dataSetList = new ArrayList<>();

        try {
            final ConcurrentLinkedQueue<DataSet> dataSetConcurrentLinkedQueue = new ConcurrentLinkedQueue<>();
            final List<Callable<DataSet>> callables = new ArrayList<>();
            for (final String smiles : smilesList) {
                callables.add(() -> predict(smilesParser.parseSmiles(smiles), querySpectrum, maxSphere,
                                            requestTransfer.getElucidationOptions()
                                                           .getShiftTolerance(), requestTransfer.getElucidationOptions()
                                                                                                .getMaxAverageDeviation(),
                                            bitSetFingerprintQuerySpectrum, keepDataSetMetaOnly, hoseCodeDBEntriesMap));
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

    private static DataSet predict(final IAtomContainer structure, final Spectrum querySpectrum, final int maxSphere,
                                   final double shiftTolerance, final double maxAverageDeviation,
                                   final BitSetFingerprint bitSetFingerprintQuerySpectrum,
                                   final boolean keepDataSetMetaOnly,
                                   final Map<String, Map<String, Double[]>> hoseCodeDBEntriesMap) {
        final String nucleus = querySpectrum.getNuclei()[0];
        final String atomType = Utils.getAtomTypeFromNucleus(nucleus);
        //        final String solvent = this.solvents[0];


        final Assignment assignment;
        final Assignment spectralMatchAssignment;
        Signal signal;
        Map<String, Double[]> hoseCodeObjectValues;
        double predictedShift;
        String hoseCode;
        Double[] statistics, deviations;
        final Double rmsd;
        Double averageDeviation;
        final Double tanimotoCoefficient;
        int signalIndex, sphere;
        List<Double> medians;
        final BitSetFingerprint bitSetFingerprintDataSet;
        final MDLV3000Writer mdlv3000Writer;
        final ByteArrayOutputStream byteArrayOutputStream;
        try {
            final DataSet dataSet = Utils.atomContainerToDataSet(structure);
            //                // convert implicit to explicit hydrogens for building HOSE codes and lookup in HOSE code DB
            //                Utils.convertImplicitToExplicitHydrogens(structure);
            //                Utils.setAromaticityAndKekulize(structure);

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
                    hoseCode = HOSECodeBuilder.buildHOSECode(structure, i, sphere, false);
                    hoseCodeObjectValues = hoseCodeDBEntriesMap.get(hoseCode);
                    if (hoseCodeObjectValues
                            != null) {
                        for (final Map.Entry<String, Double[]> solventEntry : hoseCodeObjectValues.entrySet()) {
                            statistics = hoseCodeObjectValues.get(solventEntry.getKey());
                            medians.add(statistics[3]);
                            //                            System.out.println(" -> "
                            //                                                       + sphere
                            //                                                       + " -> "
                            //                                                       + solventEntry.getKey()
                            //                                                       + " -> "
                            //                                                       + Arrays.toString(statistics));
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
                    return null;
                }
            } catch (final CDKException e) {
                e.printStackTrace();
                return null;
            }

            // to save space and time when (re-)converting datasets avoid the currently non-used information
            // the SMILES was build by CDK and stored in meta information
            if (keepDataSetMetaOnly) {
                dataSet.setStructure(null);
            } else {
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

                // store as MOL file
                mdlv3000Writer = new MDLV3000Writer();
                byteArrayOutputStream = new ByteArrayOutputStream();
                mdlv3000Writer.setWriter(byteArrayOutputStream);
                mdlv3000Writer.write(dataSet.getStructure()
                                            .toAtomContainer());
                dataSet.addMetaInfo("molfile", byteArrayOutputStream.toString());
            }

            dataSet.addMetaInfo("querySpectrumSignalCount", String.valueOf(querySpectrum.getSignalCount()));
            dataSet.addMetaInfo("querySpectrumSignalCountWithEquivalences",
                                String.valueOf(querySpectrum.getSignalCountWithEquivalences()));
            spectralMatchAssignment = Similarity.matchSpectra(predictedSpectrum, querySpectrum, 0, 0, shiftTolerance,
                                                              true, true, false);
            dataSet.addMetaInfo("setAssignmentsCount",
                                String.valueOf(spectralMatchAssignment.getSetAssignmentsCount(0)));
            dataSet.addMetaInfo("setAssignmentsCountWithEquivalences",
                                String.valueOf(spectralMatchAssignment.getSetAssignmentsCountWithEquivalences(0)));
            dataSet.addMetaInfo("isCompleteSpectralMatch", String.valueOf(querySpectrum.getSignalCount()
                                                                                  == spectralMatchAssignment.getSetAssignmentsCount(
                    0)));
            dataSet.addMetaInfo("isCompleteSpectralMatchWithEquivalences", String.valueOf(
                    querySpectrum.getSignalCountWithEquivalences()
                            == spectralMatchAssignment.getSetAssignmentsCountWithEquivalences(0)));
            dataSet.addAttachment("spectralMatchAssignment", spectralMatchAssignment);
            deviations = Similarity.getDeviations(predictedSpectrum, querySpectrum, 0, 0, spectralMatchAssignment);
            averageDeviation = Statistics.calculateAverageDeviation(deviations);
            if (averageDeviation
                    != null) {
                if (averageDeviation
                        <= maxAverageDeviation) {
                    dataSet.addMetaInfo("averageDeviation", String.valueOf(averageDeviation));
                    rmsd = Statistics.calculateRMSD(deviations);
                    dataSet.addMetaInfo("rmsd", String.valueOf(rmsd));
                    bitSetFingerprintDataSet = Similarity.getBitSetFingerprint(predictedSpectrum, 0,
                                                                               multiplicitySectionsBuilder);
                    tanimotoCoefficient = Similarity.calculateTanimotoCoefficient(bitSetFingerprintDataSet,
                                                                                  bitSetFingerprintQuerySpectrum);
                    dataSet.addMetaInfo("tanimoto", String.valueOf(tanimotoCoefficient));

                    return dataSet;
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
                                                                               multiplicitySectionsBuilder);
                    tanimotoCoefficient = Similarity.calculateTanimotoCoefficient(bitSetFingerprintQuerySpectrum,
                                                                                  bitSetFingerprintDataSet);
                    dataSet.addMetaInfo("tanimoto", String.valueOf(tanimotoCoefficient));

                    return dataSet;
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
