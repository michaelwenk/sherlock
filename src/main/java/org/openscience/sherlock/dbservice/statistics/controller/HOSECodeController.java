package org.openscience.sherlock.dbservice.statistics.controller;

import casekit.nmr.filterandrank.FilterAndRank;
import casekit.nmr.model.*;
import casekit.nmr.utils.Statistics;
import casekit.nmr.utils.Utils;

import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.nmrshiftdb.util.AtomUtils;
import org.openscience.nmrshiftdb.util.ExtendedHOSECodeGenerator;
import org.openscience.sherlock.model.exchange.Transfer;
import org.openscience.sherlock.dbservice.dataset.db.model.DataSetRecord;
import org.openscience.sherlock.dbservice.statistics.service.HOSECodeServiceImplementation;
import org.openscience.sherlock.dbservice.statistics.service.model.HOSECodeRecord;
import org.openscience.sherlock.dbservice.statistics.utils.Utilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping(value = "/statistics/hosecode")
public class HOSECodeController {

    @Autowired
    private HOSECodeServiceImplementation hoseCodeServiceImplementation;
    @Autowired
    private Utilities utilities;

    private final ExtendedHOSECodeGenerator extendedHOSECodeGenerator = new ExtendedHOSECodeGenerator();

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
        return this.hoseCodeServiceImplementation.findById(this.decode(id)).blockOptional();
    }

    @GetMapping(value = "/count")
    public long getCount() {
        return this.hoseCodeServiceImplementation.count().block();
    }

    @GetMapping(value = "/getAll")
    public List<HOSECodeRecord> getAll() {
        return this.hoseCodeServiceImplementation.findAll().collectList().block();
    }

    @DeleteMapping(value = "/deleteAll")
    public void deleteAll() {
        this.hoseCodeServiceImplementation.deleteAll().block();
    }

    @PostMapping(value = "/replaceAll")
    public void replaceAll(@RequestParam final String[] nuclei, @RequestParam final int maxSphere) {
        this.replaceAll(utilities.getByDataSetSpectrumNuclei(
                nuclei).map(DataSetRecord::getDataSet), maxSphere, false);
    }

    public void replaceAll(final Flux<DataSet> dataSetFlux, final int maxSphere, final boolean buildStatistics) {
        System.out.println(" -> replacing HOSE code collection ...");
        System.out.println(" -> deleting previous HOSE code collection ...");
        this.deleteAll();
        System.out.println(" -> previous HOSE code collection deleted");

        System.out.println(" --> building new HOSE code collection ...");
        final AtomicInteger counter = new AtomicInteger(0);
        final ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<Double, Long>>> hoseCodeShifts = new ConcurrentHashMap<>();
        dataSetFlux.doOnNext(
                dataSet -> {
                    final List<DataSet> dataSetList = new ArrayList<>();
                    dataSetList.add(dataSet);
                    try {
                        this.utilities.buildAndInsertHOSECodes(dataSetList, maxSphere,
                                hoseCodeShifts, this.hoseCodeServiceImplementation);
                        // System.out.println(" --> building HOSE codes successful");
                    } catch (final Exception e) {
                        e.printStackTrace();
                        // System.out.println(" --> building HOSE codes failed -> skipping dataset");
                    }

                    if (counter.incrementAndGet()
                            % 10000 == 0) {
                        System.out.println(" --> reached: "
                                + counter.get()
                                + " datasets");
                    }
                })
                .doAfterTerminate(() -> {
                    System.out.println(" --> building HOSE codes done for all datasets");
                    this.utilities.insertHOSECodeShiftsToDatabase(hoseCodeShifts,
                            this.hoseCodeServiceImplementation);
                    System.out.println(" --> new HOSE code collection built");
                    if (buildStatistics) {
                        this.buildStatistics();
                    }
                }).subscribe();

    }

    @PostMapping(value = "/buildStatistics")
    public void buildStatistics() {
        System.out.println(" -> building HOSE code statistics ...");
        final AtomicInteger count = new AtomicInteger(0);
        this.hoseCodeServiceImplementation.findAll()
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
                            if (shift == 1.0) {
                                continue;
                            }
                            for (long i = 0; i < entryPerShiftString.getValue(); i++) {
                                values.add(shift);
                            }
                        }
                        values = Statistics.removeOutliers(values, 1.5);
                        if (!values.isEmpty()) {
                            statistics.put(entryPerSolvent.getKey(),
                                    new Double[] { (double) values.size(),
                                            Collections.min(values),
                                            Statistics.getMean(values),
                                            Statistics.getMedian(values),
                                            Collections.max(values) });
                        }
                    }
                    hoseCodeRecord.setStatistics(statistics);
                    this.hoseCodeServiceImplementation.save(hoseCodeRecord)
                            .subscribe();

                    if (count.incrementAndGet()
                            % 100000 == 0) {
                        System.out.println(" -> reached: "
                                + count.get());
                    }
                })
                .doAfterTerminate(() -> {
                    System.out.println(" -> build HOSE code statistics done");
                })
                .subscribe();

    }

    @PostMapping(value = "/predictAndFilter")
    public Flux<DataSet> predictAndFilter(@RequestBody final Transfer transfer) {
        final String nucleus = transfer.getQuerySpectrum()
                .getNuclei()[0];
        final List<DataSet> dataSetList = new ArrayList<>();
        DataSet dataSet;
        for (final String smiles : transfer.getSmilesList()) {
            dataSet = this.predict(smiles, nucleus, transfer.getMaxSphere());
            if (dataSet != null) {
                dataSet = FilterAndRank.checkDataSet(dataSet, transfer.getQuerySpectrum(), transfer.getShiftTolerance(),
                        transfer.getMaximumAverageDeviation(),
                        transfer.isCheckMultiplicity(),
                        transfer.isCheckEquivalencesCount(),
                        transfer.isAllowLowerEquivalencesCount(),
                        transfer.getMultiplicitySectionsBuilder(), true,
                        transfer.getDetections());
                if (dataSet != null) {
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
            Utils.placeExplicitHydrogens(structure);
            Utils.setAromaticityAndKekulize(structure);

            final DataSet dataSet = Utils.atomContainerToDataSet(structure, false);

            final Spectrum predictedSpectrum = new Spectrum();
            predictedSpectrum.setNuclei(new String[] { nucleus });
            predictedSpectrum.setSignals(new ArrayList<>());

            final Map<Integer, List<Integer>> assignmentMap = new HashMap<>();
            final Map<Integer, Double[]> predictionMeta = new HashMap<>();
            final Map<Integer, Map<String, List<Integer>>> collection = new HashMap<>();

            for (int i = 0; i < structure.getAtomCount(); i++) {
                if (!structure.getAtom(i)
                        .getSymbol()
                        .equals(atomType)) {
                    continue;
                }
                sphere = maxSphere;
                while (sphere >= 1) {
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

            sphere = maxSphere;
            while (sphere >= 1
                    && predictedAtomIndices.size() < structure.getAtomCount()) {
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
                    hoseCodeRecordOptional = this.hoseCodeServiceImplementation.findById(hoseCode).blockOptional();
                    if (hoseCodeRecordOptional.isPresent()) {
                        hoseCodeRecord = hoseCodeRecordOptional.get();
                        for (final Map.Entry<String, Double[]> solventEntry : hoseCodeRecord.getStatistics()
                                .entrySet()) {
                            statistics = hoseCodeRecord.getStatistics()
                                    .get(solventEntry.getKey());
                            medians.add(statistics[3]);
                            count += statistics[0].intValue();
                            min = min == null
                                    ? statistics[1]
                                    : Double.min(min, statistics[1]);
                            max = max == null
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
                        signal.setNuclei(new String[] { nucleus });
                        signal.setShifts(new Double[] { predictedShift });
                        signal.setMultiplicity(Utils.getMultiplicityFromProtonsCount(
                                AtomUtils.getHcount(structure, structure.getAtom(atomIndex)))); // counts explicit H
                        signal.setEquivalencesCount(1);

                        signalIndex = predictedSpectrum.addSignal(signal);

                        assignmentMap.putIfAbsent(signalIndex, new ArrayList<>());
                        assignmentMap.get(signalIndex)
                                .add(atomIndex);

                        if (!predictionMeta.containsKey(signalIndex)) {
                            predictionMeta.put(signalIndex, new Double[] { (double) sphere, (double) count, min, max });
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

}
