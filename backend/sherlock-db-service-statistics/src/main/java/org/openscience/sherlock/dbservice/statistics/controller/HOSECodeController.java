package org.openscience.sherlock.dbservice.statistics.controller;

import casekit.io.FileSystem;
import casekit.nmr.analysis.HOSECodeShiftStatistics;
import casekit.nmr.filterandrank.FilterAndRank;
import casekit.nmr.model.*;
import casekit.nmr.utils.Statistics;
import casekit.nmr.utils.Utils;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.nmrshiftdb.util.AtomUtils;
import org.openscience.nmrshiftdb.util.ExtendedHOSECodeGenerator;
import org.openscience.sherlock.dbservice.statistics.service.HOSECodeRepositoryReactive;
import org.openscience.sherlock.dbservice.statistics.service.HOSECodeServiceImplementation;
import org.openscience.sherlock.dbservice.statistics.service.model.DataSetRecord;
import org.openscience.sherlock.dbservice.statistics.service.model.HOSECodeRecord;
import org.openscience.sherlock.dbservice.statistics.service.model.exchange.Transfer;
import org.openscience.sherlock.dbservice.statistics.utils.Utilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/hosecode")
public class HOSECodeController {

    private final WebClient.Builder webClientBuilder;
    private final ExchangeStrategies exchangeStrategies;
    private final HOSECodeServiceImplementation hoseCodeServiceImplementation;
    private final HOSECodeRepositoryReactive hoseCodeRepositoryReactive;
    //    private final StructureDiagramGenerator structureDiagramGenerator = new StructureDiagramGenerator();
    private final ExtendedHOSECodeGenerator extendedHOSECodeGenerator = new ExtendedHOSECodeGenerator();

    @Autowired
    public HOSECodeController(final WebClient.Builder webClientBuilder, final ExchangeStrategies exchangeStrategies,
                              final HOSECodeServiceImplementation hoseCodeServiceImplementation,
                              final HOSECodeRepositoryReactive hoseCodeRepositoryReactive) {
        this.webClientBuilder = webClientBuilder;
        this.exchangeStrategies = exchangeStrategies;
        this.hoseCodeServiceImplementation = hoseCodeServiceImplementation;
        this.hoseCodeRepositoryReactive = hoseCodeRepositoryReactive;
    }

    private String decode(final String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return "";
    }

    @GetMapping(value = "/getByID")
    public Optional<HOSECodeRecord> getByID(@RequestParam final String id) {
        return this.hoseCodeServiceImplementation.findById(this.decode(id));
    }

    @GetMapping(value = "/count")
    public long getCount() {
        return this.hoseCodeServiceImplementation.count();
    }

    @GetMapping(value = "/getAll")
    public List<HOSECodeRecord> getAll() {
        return this.hoseCodeServiceImplementation.findAll();
    }


    @DeleteMapping(value = "/deleteAll")
    public void deleteAll() {
        this.hoseCodeServiceImplementation.deleteAll();
    }

    @PostMapping(value = "/replaceAll")
    public void replaceAll(@RequestParam final String[] nuclei, @RequestParam final int maxSphere) {
        System.out.println(" --> delete all DB entries...");
        this.deleteAll();
        System.out.println(" --> deleted all DB entries!");

        System.out.println(" --> fetching all datasets, build HOSE code collection and store...");

        final List<DataSet> dataSetList = Objects.requireNonNull(Utilities.getByDataSetSpectrumNuclei(
                                                                                  this.webClientBuilder, this.exchangeStrategies, nuclei)
                                                                          .collectList()
                                                                          .block())
                                                 .stream()
                                                 .map(DataSetRecord::getDataSet)
                                                 .collect(Collectors.toList());
        System.out.println(" --> fetched all datasets: "
                                   + dataSetList.size());
        List<DataSet> dataSetListTemp;
        boolean printOutput;
        int i = 0;
        final int steps = 10;
        while (i
                + steps
                < dataSetList.size()) {
            dataSetListTemp = new ArrayList<>();
            for (int j = i; j
                    < i
                    + steps; j++) {
                dataSetListTemp.add(dataSetList.get(j));
            }
            printOutput = i
                    % steps
                    == 0;
            if (printOutput) {
                System.out.println(" --> dataset: "
                                           + i);
                System.out.println(" --> building HOSE code statistics...");
            }

            try {
                final Map<String, Map<String, Double[]>> hoseCodeShiftStatisticsTemp3D = HOSECodeShiftStatistics.buildHOSECodeShiftStatistics(
                        dataSetListTemp, maxSphere, true, false);
                if (printOutput) {
                    System.out.println(" --> building HOSE code statistics done -> "
                                               + hoseCodeShiftStatisticsTemp3D.size());
                    System.out.println(" --> updating HOSE codes in database...");
                }
                this.insertOrUpdateHOSECodeRecord(hoseCodeShiftStatisticsTemp3D);
                if (printOutput) {
                    System.out.println(" --> updating HOSE codes in database done");
                }


            } catch (final Exception e) {
                e.printStackTrace();
                System.out.println(" --> building HOSE code statistics failed");
            }

            i = i
                    + steps;
        }
        if (i
                < dataSetList.size()) {
            System.out.println(" --> walking through the rest -> "
                                       + (dataSetList.size()
                    - i)
                                       + " datasets...");

            dataSetListTemp = new ArrayList<>();
            for (int j = i; j
                    < dataSetList.size(); j++) {
                dataSetListTemp.add(dataSetList.get(j));
            }
            System.out.println(" --> building HOSE code statistics...");
            final Map<String, Map<String, Double[]>> hoseCodeShiftStatisticsTemp3D = HOSECodeShiftStatistics.buildHOSECodeShiftStatistics(
                    dataSetListTemp, maxSphere, true, false);
            System.out.println(" --> building HOSE code statistics done -> "
                                       + hoseCodeShiftStatisticsTemp3D.size());
            System.out.println(" --> updating HOSE codes in database...");
            this.insertOrUpdateHOSECodeRecord(hoseCodeShiftStatisticsTemp3D);
            System.out.println(" --> updating HOSE codes in database done");

            System.out.println(" --> rest is done");
        }
    }

    private void insertOrUpdateHOSECodeRecord(final Map<String, Map<String, Double[]>> hoseCodeShiftStatisticsTemp) {
        for (final Map.Entry<String, Map<String, Double[]>> entryPerHOSECode : hoseCodeShiftStatisticsTemp.entrySet()) {
            final String hoseCode = entryPerHOSECode.getKey();
            if (!this.hoseCodeServiceImplementation.existsById(hoseCode)) {
                final HOSECodeRecord hoseCodeRecord = new HOSECodeRecord(hoseCode, new HashMap<>(), new HashMap<>());
                this.insertIntoHoseCodeRecord(entryPerHOSECode, hoseCodeRecord);
                this.hoseCodeServiceImplementation.insert(hoseCodeRecord);
            } else {
                final HOSECodeRecord hoseCodeRecord = this.hoseCodeServiceImplementation.findById(hoseCode)
                                                                                        .get();
                this.insertIntoHoseCodeRecord(entryPerHOSECode, hoseCodeRecord);
                this.hoseCodeServiceImplementation.save(hoseCodeRecord);
            }
        }
    }

    private void insertIntoHoseCodeRecord(final Map.Entry<String, Map<String, Double[]>> entryPerHOSECode,
                                          final HOSECodeRecord hoseCodeRecord) {
        String solvent, shiftString;
        for (final Map.Entry<String, Double[]> entryPerSolvent : entryPerHOSECode.getValue()
                                                                                 .entrySet()) {
            solvent = entryPerSolvent.getKey();
            hoseCodeRecord.getValues()
                          .putIfAbsent(solvent, new HashMap<>());
            for (final Double shift : entryPerSolvent.getValue()) {
                shiftString = String.valueOf(Statistics.roundDouble(shift, 1))
                                    .replaceAll("\\.", "_");
                hoseCodeRecord.getValues()
                              .get(solvent)
                              .putIfAbsent(shiftString, 0L);
                hoseCodeRecord.getValues()
                              .get(solvent)
                              .put(shiftString, hoseCodeRecord.getValues()
                                                              .get(solvent)
                                                              .get(shiftString)
                                      + 1);
            }
        }
    }

    @PostMapping(value = "/buildStatistics")
    public void buildStatistics() {
        final AtomicInteger count = new AtomicInteger(0);
        this.hoseCodeRepositoryReactive.findAll()
                                       .doOnNext(hoseCodeRecord -> {
                                           final Map<String, Double[]> statistics = new HashMap<>();
                                           List<Double> values;
                                           double shift;

                                           for (final Map.Entry<String, Map<String, Long>> entryPerSolvent : hoseCodeRecord.getValues()
                                                                                                                           .entrySet()) {
                                               values = new ArrayList<>();
                                               for (final Map.Entry<String, Long> entryPerShiftString : entryPerSolvent.getValue()
                                                                                                                       .entrySet()) {
                                                   shift = Double.parseDouble(entryPerShiftString.getKey()
                                                                                                 .replaceAll("_",
                                                                                                             "\\."));
                                                   if (shift
                                                           == 1.0) {
                                                       continue;
                                                   }
                                                   for (long i = 0; i
                                                           < entryPerShiftString.getValue(); i++) {
                                                       values.add(shift);
                                                   }
                                               }
                                               if (!values.isEmpty()) {
                                                   statistics.put(entryPerSolvent.getKey(),
                                                                  new Double[]{(double) values.size(),
                                                                               Collections.min(values),
                                                                               Statistics.getMean(values),
                                                                               Statistics.getMedian(values),
                                                                               Collections.max(values)});
                                               }
                                           }
                                           hoseCodeRecord.setStatistics(statistics);
                                           this.hoseCodeRepositoryReactive.save(hoseCodeRecord)
                                                                          .subscribe();

                                           if (count.incrementAndGet()
                                                   % 100000
                                                   == 0) {
                                               System.out.println(" -> reached: "
                                                                          + count.get());
                                           }
                                       })
                                       .doAfterTerminate(() -> {
                                           System.out.println(" -> build statistics done");
                                       })
                                       .subscribe();

    }

    @PostMapping(value = "/predictAndFilter")
    public Flux<DataSet> predictAndFilter(@RequestBody final Transfer transfer) {
        final String nucleus = transfer.getQuerySpectrum()
                                       .getNuclei()[0];
        final List<DataSet> dataSetList = new ArrayList<>();
        List<DataSet> dataSetListTemp;
        for (final String smiles : transfer.getPredictionOptions()
                                           .getSmilesList()) {
            dataSetListTemp = new ArrayList<>();
            dataSetListTemp.add(this.predict(this.decode(smiles), nucleus, transfer.getPredictionOptions()
                                                                                   .getMaxSphere(), false));
            final List<DataSet> resultDataSetList = this.filter(transfer, dataSetListTemp);
            //            if (!resultDataSetList.isEmpty()) {
            //                if (transfer.getPredictionOptions()
            //                            .isPredictWithStereo()) {
            //                    final List<DataSet> dataSetListToIterate = new ArrayList<>(resultDataSetList);
            //                    for (final DataSet dataSet : dataSetListToIterate) {
            //                        dataSetListTemp = this.predictWithStereo(dataSet.getMeta()
            //                                                                        .get("smiles"), nucleus,
            //                                                                 transfer.getPredictionOptions()
            //                                                                         .getMaxSphere());
            //                        dataSetList.addAll(this.filter(transfer, dataSetListTemp));
            //                    }
            //                } else {
            //                    dataSetList.addAll(resultDataSetList);
            //                }
            //            }
            dataSetList.addAll(resultDataSetList);
        }

        return Flux.fromIterable(dataSetList);
    }

    private List<DataSet> filter(final Transfer transfer, final List<DataSet> dataSetList) {
        final List<DataSet> dataSetListFiltered = new ArrayList<>();
        DataSet dataSetTemp;
        for (final DataSet dataSet : dataSetList) {
            if (dataSet
                    != null) {
                dataSetTemp = FilterAndRank.checkDataSet(dataSet, transfer.getQuerySpectrum(),
                                                         transfer.getPredictionOptions()
                                                                 .getShiftTolerance(), transfer.getPredictionOptions()
                                                                                               .getMaximumAverageDeviation(),
                                                         transfer.getPredictionOptions()
                                                                 .isCheckMultiplicity(), transfer.getPredictionOptions()
                                                                                                 .isCheckEquivalencesCount(),
                                                         transfer.getPredictionOptions()
                                                                 .isAllowLowerEquivalencesCount(),
                                                         transfer.getPredictionOptions()
                                                                 .getMultiplicitySectionsBuilder(), true,
                                                         transfer.getDetections());
                if (dataSetTemp
                        != null) {
                    dataSetListFiltered.add(dataSetTemp);
                }
            }
        }

        return dataSetListFiltered;
    }


    @PostMapping(value = "/predictAndFilterWithStereo")
    public Flux<DataSet> predictAndFilterWithStereo(@RequestBody final Transfer transfer) {
        final String nucleus = transfer.getQuerySpectrum()
                                       .getNuclei()[0];
        final List<DataSet> dataSetList = this.predictWithStereo(transfer.getSmiles(), nucleus,
                                                                 transfer.getPredictionOptions()
                                                                         .getMaxSphere());

        return Flux.fromIterable(this.filter(transfer, dataSetList));
    }

    @GetMapping(value = "/predictWithStereo")
    public List<DataSet> predictWithStereo(@RequestParam final String smiles, @RequestParam final String nucleus,
                                           @RequestParam final int maxSphere) {

        final List<IAtomContainer> structureList = new ArrayList<>();
        try {
            final String requestID = UUID.randomUUID()
                                         .toString();
            final String tempDir = "/data/temp/";
            //            final String tempFileName = tempDir
            //                    + requestID
            //                    + ".sdf";
            //
            //            String line = "python3 /scripts/createStereoisomers.py \""
            //                    + requestID
            //                    + "\" \""
            //                    + this.decode(smiles)
            //                    + "\" \""
            //                    + tempFileName
            //                    + "\"";
            //            CommandLine cmdLine = CommandLine.parse(line);
            //
            //            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            //            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
            //            DefaultExecutor executor = new DefaultExecutor();
            //            executor.setStreamHandler(streamHandler);
            //
            //            executor.execute(cmdLine);
            //
            //            final IteratingSDFReader iteratingSDFReader = new IteratingSDFReader(new FileReader(tempFileName),
            //                                                                                 SilentChemObjectBuilder.getInstance());

            final IAtomContainer structure;
            final String tempFileName2;
            final IteratingSDFReader iteratingSDFReader2;
            //            while (iteratingSDFReader.hasNext()) {
            //                structure = iteratingSDFReader.next();

            tempFileName2 = tempDir
                    + requestID
                    + "_2.sdf";
            final String line = "node /scripts/buildDiastereotopics.js \""
                    + this.decode(smiles)
                    //+ structure.getProperty("smiles")
                    + "\" \""
                    + tempFileName2
                    + "\"";
            final CommandLine cmdLine = CommandLine.parse(line);

            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            final PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
            final DefaultExecutor executor = new DefaultExecutor();
            executor.setStreamHandler(streamHandler);

            executor.execute(cmdLine);

            iteratingSDFReader2 = new IteratingSDFReader(new FileReader(tempFileName2),
                                                         SilentChemObjectBuilder.getInstance());

            structure = iteratingSDFReader2.next();
            if (Utilities.setUpAndDownBondsOnTwoMethylGroups(structure, true)) { //false);
                structureList.add(structure);
            }

            //                        structure.setProperty("smiles", SmilesGenerator.isomeric()
            //                                                                       .create(structure));
            //
            //            structureList.add(structure);
            //            }

            FileSystem.cleanup(new String[]{tempDir}, requestID);
        } catch (final IOException e) {
            e.printStackTrace();
        }

        final List<DataSet> dataSetList = new ArrayList<>();
        DataSet dataSet;
        for (final IAtomContainer structure : structureList) {
            dataSet = this.predict(structure, nucleus, maxSphere, true);
            if (dataSet
                    != null) {
                dataSetList.add(dataSet);
            }
        }

        return dataSetList;
    }

    @GetMapping(value = "/predict")
    private DataSet predict(@RequestParam final String smiles, @RequestParam final String nucleus,
                            @RequestParam final int maxSphere, @RequestParam final boolean withStereo) {
        final String decodedSmiles = this.decode(smiles);
        try {
            final IAtomContainer structure = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(
                    decodedSmiles);
            structure.setProperty("smiles", decodedSmiles);

            return this.predict(structure, nucleus, maxSphere, withStereo);
        } catch (final InvalidSmilesException e) {
            e.printStackTrace();
        }

        return null;
    }

    private DataSet predict(final IAtomContainer structure, final String nucleus, final int maxSphere,
                            final boolean withStereo) {
        final String atomType = Utils.getAtomTypeFromNucleus(nucleus);
        final Assignment assignment;
        String hoseCode;
        int sphere;

        try {
            final String molfile = Utilities.createMolFileContent(structure);

            Utils.placeExplicitHydrogens(structure);
            Utils.setAromaticityAndKekulize(structure);

            final Map<String, String> meta = new HashMap<>();
            final String mf = Utils.molecularFormularToString(Utils.getMolecularFormulaFromAtomContainer(structure));
            meta.put("mfOriginal", mf);
            meta.put("mf", Utils.buildAlphabeticMF(mf));
            meta.put("smiles", structure.getProperty("smiles"));
            meta.put("molfile", molfile);

            final DataSet dataSet = new DataSet();
            dataSet.setMeta(meta);
            dataSet.setAttachment(new HashMap<>());

            final Spectrum predictedSpectrum = new Spectrum();
            predictedSpectrum.setNuclei(new String[]{nucleus});
            predictedSpectrum.setSignals(new ArrayList<>());

            final Map<Integer, List<Integer>> assignmentMap = new HashMap<>();
            final Map<Integer, Double[]> predictionMeta = new HashMap<>();
            final Map<Integer, Map<String, List<Integer>>> collection = new HashMap<>();

            for (int i = 0; i
                    < structure.getAtomCount(); i++) {
                if (!structure.getAtom(i)
                              .getSymbol()
                              .equals(atomType)) {
                    continue;
                }
                sphere = maxSphere;
                while (sphere
                        >= 1) {
                    hoseCode = this.extendedHOSECodeGenerator.getHOSECode(structure, structure.getAtom(i), sphere);
                    collection.putIfAbsent(sphere, new HashMap<>());
                    collection.get(sphere)
                              .putIfAbsent(hoseCode, new ArrayList<>());
                    collection.get(sphere)
                              .get(hoseCode)
                              .add(i);

                    sphere--;
                }
            }
            final List<Integer> predictedAtomIndices = new ArrayList<>();
            this.assignToAtoms(predictedSpectrum, assignmentMap, predictionMeta, collection, predictedAtomIndices,
                               structure, nucleus, maxSphere, withStereo);

            Utils.convertExplicitToImplicitHydrogens(structure);

            dataSet.setStructure(new StructureCompact(structure));

            dataSet.setSpectrum(new SpectrumCompact(predictedSpectrum));
            assignment = new Assignment();
            assignment.setNuclei(predictedSpectrum.getNuclei());
            assignment.initAssignments(predictedSpectrum.getSignalCount());

            for (final Map.Entry<Integer, List<Integer>> entry : assignmentMap.entrySet()) {
                for (final int atomIndex : entry.getValue()) {
                    assignment.addAssignmentEquivalence(0, entry.getKey(), atomIndex);
                }
            }
            dataSet.setAssignment(assignment);

            dataSet.addAttachment("predictionMeta", predictionMeta);

            return dataSet;
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void assignToAtoms(final Spectrum predictedSpectrum, final Map<Integer, List<Integer>> assignmentMap,
                               final Map<Integer, Double[]> predictionMeta,
                               final Map<Integer, Map<String, List<Integer>>> collection,
                               final List<Integer> predictedAtomIndices, final IAtomContainer structure,
                               final String nucleus, final Integer maxSphere, final boolean withStereo) {
        Signal signal;
        Optional<HOSECodeRecord> hoseCodeRecordOptional;
        HOSECodeRecord hoseCodeRecord;
        double predictedShift;
        String hoseCode;
        Double[] statistics;
        int signalIndex, sphere, count;
        Double min, max;
        List<Double> medians;

        sphere = maxSphere;
        while (sphere
                >= 1
                && predictedAtomIndices.size()
                < structure.getAtomCount()) {
            for (final Map.Entry<String, List<Integer>> entryPerHOSECode : collection.get(sphere)
                                                                                     .entrySet()) {
                if (predictedAtomIndices.containsAll(entryPerHOSECode.getValue())) {
                    continue;
                }
                medians = new ArrayList<>();
                count = 0;
                min = null;
                max = null;
                hoseCode = entryPerHOSECode.getKey();
                hoseCodeRecordOptional = this.hoseCodeServiceImplementation.findById(hoseCode);
                if (hoseCodeRecordOptional.isPresent()) {
                    hoseCodeRecord = hoseCodeRecordOptional.get();
                    for (final Map.Entry<String, Double[]> solventEntry : hoseCodeRecord.getStatistics()
                                                                                        .entrySet()) {
                        statistics = hoseCodeRecord.getStatistics()
                                                   .get(solventEntry.getKey());
                        medians.add(statistics[3]);
                        count += statistics[0].intValue();
                        min = min
                                      == null
                              ? statistics[1]
                              : Double.min(min, statistics[1]);
                        max = max
                                      == null
                              ? statistics[4]
                              : Double.max(max, statistics[4]);
                    }
                }
                if (medians.isEmpty()) {
                    continue;
                }
                predictedShift = Statistics.getMean(medians);

                // insert signals
                for (final int atomIndex : entryPerHOSECode.getValue()) {
                    if (predictedAtomIndices.contains(atomIndex)) {
                        continue;
                    }
                    signal = new Signal();
                    signal.setNuclei(new String[]{nucleus});
                    signal.setShifts(new Double[]{predictedShift});
                    signal.setMultiplicity(Utils.getMultiplicityFromProtonsCount(
                            AtomUtils.getHcount(structure, structure.getAtom(atomIndex)))); // counts explicit H
                    signal.setEquivalencesCount(1);

                    signalIndex = predictedSpectrum.addSignal(signal);

                    assignmentMap.putIfAbsent(signalIndex, new ArrayList<>());
                    assignmentMap.get(signalIndex)
                                 .add(atomIndex);

                    if (!predictionMeta.containsKey(signalIndex)) {
                        predictionMeta.put(signalIndex, new Double[]{(double) sphere, (double) count, min, max,
                                                                     withStereo
                                                                     ? 1.0
                                                                     : 0.0});
                    }
                    predictedAtomIndices.add(atomIndex);
                }
            }
            sphere--;
        }
    }

    //    private void addUpAndDownBondsOnTwoMethylGroups(final IAtomContainer structure) {
    //        for (final IAtom atom : structure.atoms()) {
    //            if (AtomContainerManipulator.countHydrogens(structure, atom)
    //                    == 3
    //                    || atom.getSymbol()
    //                           .equals("H")) {
    //                continue;
    //            }
    //            final List<Integer> methylGroups = new ArrayList<>();
    //            final List<IAtom> connectedAtomsList = structure.getConnectedAtomsList(atom);
    //            for (final IAtom connectedAtom : connectedAtomsList) {
    //                if (AtomContainerManipulator.countHydrogens(structure, connectedAtom)
    //                        == 3) {
    //                    methylGroups.add(connectedAtom.getIndex());
    //                }
    //            }
    //            if (methylGroups.size()
    //                    == 2) {
    //                structure.getBond(atom, structure.getAtom(methylGroups.get(0)))
    //                         .setStereo(IBond.Stereo.UP);
    //                structure.getBond(atom, structure.getAtom(methylGroups.get(1)))
    //                         .setStereo(IBond.Stereo.DOWN);
    //            }
    //        }
    //    }

    //    @GetMapping(value = "/saveAllAsMap")
    //    public void saveAllAsMap() {
    //
    //        final Gson gson = new GsonBuilder().create();
    //        final String pathToHOSECodesFile = "/data/hosecode/hosecodes.json";
    //        System.out.println("-> store json file in shared volume under \""
    //                                   + pathToHOSECodesFile
    //                                   + "\"");
    //
    //        final StringBuilder stringBuilder = new StringBuilder();
    //        stringBuilder.append("[");
    //
    //        this.getAll()
    //            .doOnNext(hoseCodeObject -> stringBuilder.append(gson.toJson(hoseCodeObject))
    //                                                     .append(","))
    //            .doAfterTerminate(() -> {
    //                stringBuilder.deleteCharAt(stringBuilder.length()
    //                                                   - 1);
    //                stringBuilder.append("]");
    //                FileSystem.writeFile(pathToHOSECodesFile, stringBuilder.toString());
    //                System.out.println("-> done");
    //            })
    //            .subscribe();
    //    }
}
