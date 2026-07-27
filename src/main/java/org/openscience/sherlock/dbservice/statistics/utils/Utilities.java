package org.openscience.sherlock.dbservice.statistics.utils;

import casekit.nmr.analysis.HOSECodeShiftStatistics;
import casekit.nmr.model.DataSet;
import casekit.nmr.utils.Statistics;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.sherlock.dbservice.dataset.controller.DataSetController;
import org.openscience.sherlock.dbservice.dataset.db.model.DataSetRecord;
import org.openscience.sherlock.dbservice.statistics.service.model.HOSECodeRecord;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class Utilities {

        private final DataSetController dataSetController;

        public Utilities(final DataSetController dataSetController) {
                this.dataSetController = dataSetController;
        }

        public long getDataSetCount() {
                return dataSetController.getCount().block();
        }

        public Flux<DataSetRecord> getDataSetsByIds(final Iterable<String> ids) {
                return dataSetController.getByIds(ids);
        }

        public Flux<DataSetRecord> getAllDataSets() {
                return dataSetController.getAll();
        }

        public Flux<DataSetRecord> getByDataSetSpectrumNuclei(
                        final String[] nuclei) {
                // @TODO take the nuclei order into account when matching -> now it's just an
                // exact array match

                return dataSetController.getByDataSetSpectrumNuclei(nuclei);
        }

        public boolean containsStereoConfiguration(final DataSet dataSet) {
                for (final int[][] bondProperties : dataSet.getStructure()
                                .getBondProperties()) {
                        for (final int[] bondProperty : bondProperties) {
                                if (bondProperty[4] == IBond.Stereo.UP.ordinal()
                                                || bondProperty[4] == IBond.Stereo.UP_INVERTED.ordinal()
                                                || bondProperty[4] == IBond.Stereo.DOWN.ordinal()
                                                || bondProperty[4] == IBond.Stereo.DOWN_INVERTED.ordinal()) {
                                        return true;
                                }
                        }
                }

                return false;
        }

        public void removeStereoConfiguration(final DataSet dataSet) {
                for (final int[][] bondProperties : dataSet.getStructure()
                                .getBondProperties()) {
                        for (final int[] bondProperty : bondProperties) {
                                bondProperty[4] = 0;
                        }
                }
        }

        public void buildHOSECodes(final List<DataSet> dataSetList, final int maxSphere,
                        final ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<Double, Long>>> hoseCodeShifts) {
                // final List<Boolean> containsStereo = new ArrayList<>();
                // for (final DataSet dataSet : dataSetList) {
                // containsStereo.add(this.containsStereoConfiguration(dataSet));
                // }
                //
                // System.out.println(" --> building 3D HOSE codes with/without stereo
                // configuration...");
                // Map<String, Map<String, Double[]>> hoseCodeShiftStatistics =
                // HOSECodeShiftStatistics.buildHOSECodeShiftStatistics(
                // dataSetList, maxSphere, true, false);
                // System.out.println(" --> building 3D HOSE codes with stereo configuration
                // done -> "
                // + hoseCodeShiftStatistics.size());
                // System.out.println(" --> updating 3D HOSE codes in database...");
                // insertOrUpdateHOSECodeRecord(hoseCodeShiftStatistics,
                // hoseCodeServiceImplementation);
                // System.out.println(" --> updating 3D HOSE codes in database done");

                // // walk through every dataset with stereo information again but without
                // considering stereo
                // final List<DataSet> dataSetList2 = new ArrayList<>();
                // DataSet dataSet;
                // for (int i = 0; i
                // < dataSetList.size(); i++) {
                // if (containsStereo.get(i)) {
                // dataSet = dataSetList.get(i)
                // .buildClone();
                // Utilities.removeStereoConfiguration(dataSet);
                //
                // dataSetList2.add(dataSet);
                // }
                // }
                //
                // System.out.println(" --> building 3D HOSE codes without stereo configuration
                // in "
                // + dataSetList2.size()
                // + " cases...");
                // hoseCodeShiftStatistics =
                // HOSECodeShiftStatistics.buildHOSECodeShiftStatistics(dataSetList2, maxSphere,
                // true,
                // false);
                // System.out.println(" --> building 3D HOSE codes without stereo configuration
                // done -> "
                // + hoseCodeShiftStatistics.size());
                // System.out.println(" --> updating 3D HOSE codes in database...");
                // insertOrUpdateHOSECodeRecord(hoseCodeShiftStatistics,
                // hoseCodeServiceImplementation);
                // System.out.println(" --> updating 3D HOSE codes in database done");

                // for now:
                // walk through every dataset without considering stereo bond information
                // but benefit from the distinctions at double bonds
                for (final DataSet dataSet : dataSetList) {
                        this.removeStereoConfiguration(dataSet);
                }
                // System.out.println(" --> building 3D HOSE codes without stereo configuration
                // in " + dataSetList.size()
                // + " cases...");
                final Map<String, Map<String, Double[]>> hoseCodeShiftStatistics = HOSECodeShiftStatistics
                                .buildHOSECodeShiftStatistics(
                                                dataSetList, maxSphere, true, false);
                // System.out.println(" --> building 3D HOSE codes without stereo configuration
                // done -> "
                // + hoseCodeShiftStatistics.size());
                // System.out.println(" --> updating 3D HOSE codes in database...");

                this.updateHoseCodeEntry(hoseCodeShifts, hoseCodeShiftStatistics);
                // System.out.println(" --> updating 3D HOSE codes in database done");
        }

        public void updateHoseCodeEntry(
                        final ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<Double, Long>>> hoseCodeShifts,
                        final Map<String, Map<String, Double[]>> hoseCodeShiftStatisticsTemp) {
                for (final Map.Entry<String, Map<String, Double[]>> entryPerHOSECode : hoseCodeShiftStatisticsTemp
                                .entrySet()) {
                        final String hoseCode = entryPerHOSECode.getKey();

                        hoseCodeShifts.putIfAbsent(hoseCode,
                                        new ConcurrentHashMap<>());
                        String solvent;
                        Double roundedShift;
                        for (final Map.Entry<String, Double[]> entryPerSolvent : entryPerHOSECode
                                        .getValue().entrySet()) {
                                solvent = entryPerSolvent.getKey();
                                hoseCodeShifts.get(hoseCode)
                                                .putIfAbsent(solvent,
                                                                new ConcurrentHashMap<>());
                                for (final Double shift : entryPerSolvent.getValue()) {
                                        roundedShift = Statistics.roundDouble(shift, 1);
                                        hoseCodeShifts.get(hoseCode)
                                                        .get(solvent)
                                                        .putIfAbsent(roundedShift, 0L);
                                        hoseCodeShifts.get(hoseCode)
                                                        .get(solvent)
                                                        .put(roundedShift, hoseCodeShifts.get(hoseCode)
                                                                        .get(solvent)
                                                                        .get(roundedShift)
                                                                        + 1);
                                }
                        }
                }
        }

        public List<HOSECodeRecord> buildHOSECodeRecords(
                        final List<DataSet> dataSetList,
                        final int maxSphere) {
                final ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<Double, Long>>> hoseCodeShifts = new ConcurrentHashMap<>();
                for (final DataSet dataSet : dataSetList) {
                        final List<DataSet> singleDataSetList = new ArrayList<>();
                        singleDataSetList.add(dataSet);
                        try {
                                this.buildHOSECodes(singleDataSetList, maxSphere, hoseCodeShifts);
                        } catch (final Exception e) {
                                e.printStackTrace();
                        }
                }

                return this.buildHOSECodeRecords(hoseCodeShifts);
        }

        public List<HOSECodeRecord> buildHOSECodeRecords(
                        final ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<Double, Long>>> hoseCodeShifts) {
                final List<HOSECodeRecord> hoseCodeRecords = new ArrayList<>();
                for (final Map.Entry<String, ConcurrentHashMap<String, ConcurrentHashMap<Double, Long>>> entryPerHOSECode : hoseCodeShifts
                                .entrySet()) {
                        hoseCodeRecords.add(this.buildHoseCodeRecord(entryPerHOSECode));
                }

                return hoseCodeRecords;
        }

        public HOSECodeRecord buildHoseCodeRecord(
                        final Map.Entry<String, ConcurrentHashMap<String, ConcurrentHashMap<Double, Long>>> entryPerHOSECode) {
                String solvent;
                String shiftString;
                final String hoseCode = entryPerHOSECode.getKey();
                final HOSECodeRecord hoseCodeRecord = new HOSECodeRecord(hoseCode, new HashMap<>(),
                                new HashMap<>());
                for (final Map.Entry<String, ConcurrentHashMap<Double, Long>> entryPerSolvent : entryPerHOSECode
                                .getValue().entrySet()) {
                        solvent = entryPerSolvent.getKey();
                        hoseCodeRecord.getValues()
                                        .putIfAbsent(solvent, new HashMap<>());
                        for (final Map.Entry<Double, Long> entryPerShift : entryPerSolvent
                                        .getValue().entrySet()) {
                                shiftString = String.valueOf(Statistics.roundDouble(entryPerShift.getKey(), 1))
                                                .replaceAll("\\.", "_");
                                hoseCodeRecord.getValues()
                                                .get(solvent)
                                                .putIfAbsent(shiftString, 0L);
                                hoseCodeRecord.getValues()
                                                .get(solvent)
                                                .put(shiftString, entryPerShift.getValue());
                        }
                }

                return hoseCodeRecord;
        }
}
