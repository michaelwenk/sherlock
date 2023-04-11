package org.openscience.sherlock.dbservice.statistics.controller;

import casekit.nmr.analysis.HOSECodeShiftStatistics;
import casekit.nmr.filterandrank.FilterAndRank;
import casekit.nmr.model.*;
import casekit.nmr.utils.Statistics;
import casekit.nmr.utils.Utils;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.nmrshiftdb.util.AtomUtils;
import org.openscience.nmrshiftdb.util.ExtendedHOSECodeGenerator;
import org.openscience.sherlock.dbservice.statistics.service.HOSECodeRepositoryReactive;
import org.openscience.sherlock.dbservice.statistics.service.HOSECodeServiceImplementation;
import org.openscience.sherlock.dbservice.statistics.service.model.HOSECodeRecord;
import org.openscience.sherlock.dbservice.statistics.service.model.exchange.Transfer;
import org.openscience.sherlock.dbservice.statistics.utils.Utilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping(value = "/hosecode")
public class HOSECodeController {

    private final WebClient.Builder webClientBuilder;
    private final ExchangeStrategies exchangeStrategies;
    private final HOSECodeServiceImplementation hoseCodeServiceImplementation;
    private final HOSECodeRepositoryReactive hoseCodeRepositoryReactive;

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
        Utilities.getByDataSetSpectrumNuclei(this.webClientBuilder, this.exchangeStrategies, nuclei)
                 .doOnNext(dataSetRecord -> {
                     final List<DataSet> dataSetList = new ArrayList<>();
                     dataSetList.add(dataSetRecord.getDataSet());
                     final Map<String, Map<String, Double[]>> hoseCodeShiftStatisticsTemp = HOSECodeShiftStatistics.buildHOSECodeShiftStatistics(
                             dataSetList, maxSphere, true, false);
                     for (final Map.Entry<String, Map<String, Double[]>> entryPerHOSECode : hoseCodeShiftStatisticsTemp.entrySet()) {
                         final String hoseCode = entryPerHOSECode.getKey();
                         if (!this.hoseCodeServiceImplementation.existsById(hoseCode)) {
                             final HOSECodeRecord hoseCodeRecord = new HOSECodeRecord(hoseCode, new HashMap<>(),
                                                                                      new HashMap<>());
                             this.insertIntoHoseCodeRecord(entryPerHOSECode, hoseCodeRecord);
                             this.hoseCodeServiceImplementation.insert(hoseCodeRecord);
                         } else {
                             final HOSECodeRecord hoseCodeRecord = this.hoseCodeServiceImplementation.findById(hoseCode)
                                                                                                     .get();
                             this.insertIntoHoseCodeRecord(entryPerHOSECode, hoseCodeRecord);
                             this.hoseCodeServiceImplementation.save(hoseCodeRecord);
                         }
                     }
                 })
                 .doAfterTerminate(() -> {
                     System.out.println(" -> build and store HOSE code collection done: "
                                                + this.getCount());
                 })
                 .doOnError(Throwable::printStackTrace)
                 .subscribe();
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
        for (final String smiles : transfer.getSmilesList()) {


            DataSet dataSet = this.predict(smiles, nucleus, transfer.getMaxSphere());
            if (dataSet
                    != null) {
                dataSet = FilterAndRank.checkDataSet(dataSet, transfer.getQuerySpectrum(), transfer.getShiftTolerance(),
                                                     transfer.getMaximumAverageDeviation(),
                                                     transfer.isCheckMultiplicity(),
                                                     transfer.isCheckEquivalencesCount(),
                                                     transfer.isAllowLowerEquivalencesCount(),
                                                     transfer.getMultiplicitySectionsBuilder(), true,
                                                     transfer.getDetections());
                if (dataSet
                        != null) {
                    dataSetList.add(dataSet);
                }
            }
        }

        return Flux.fromIterable(dataSetList);
    }

    @GetMapping(value = "/predict")
    public DataSet predict(@RequestParam final String smiles, @RequestParam final String nucleus,
                           @RequestParam final int maxSphere) {

        final IAtomContainer structure;
        try {
            structure = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(this.decode(smiles));
        } catch (final InvalidSmilesException e) {
            e.printStackTrace();
            return null;
        }
        final String atomType = Utils.getAtomTypeFromNucleus(nucleus);
        final StructureDiagramGenerator structureDiagramGenerator = new StructureDiagramGenerator();
        final ExtendedHOSECodeGenerator extendedHOSECodeGenerator = new ExtendedHOSECodeGenerator();

        final Assignment assignment;
        Signal signal;
        Optional<HOSECodeRecord> hoseCodeRecordOptional;
        HOSECodeRecord hoseCodeRecord;
        double predictedShift;
        String hoseCode;
        Double[] statistics;
        int signalIndex, sphere, count;
        Double min, max;
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
                    hoseCode = extendedHOSECodeGenerator.getHOSECode(structure, structure.getAtom(i), sphere);
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
                            predictionMeta.put(signalIndex, new Double[]{(double) sphere, (double) count, min, max});
                        }
                        predictedAtomIndices.add(atomIndex);
                    }
                }
                sphere--;
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
