package org.openscience.sherlock.dbservice.statistics.utils;

import casekit.nmr.analysis.HOSECodeShiftStatistics;
import casekit.nmr.model.DataSet;
import casekit.nmr.utils.Statistics;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.sherlock.dbservice.dataset.controller.DataSetController;
import org.openscience.sherlock.dbservice.dataset.db.model.DataSetRecord;
import org.openscience.sherlock.dbservice.statistics.service.HOSECodeServiceImplementation;
import org.openscience.sherlock.dbservice.statistics.service.model.HOSECodeRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class Utilities {

        @Autowired
        private DataSetController dataSetController;

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

        public void insertIntoHoseCodeRecord(final Map.Entry<String, Map<String, Double[]>> entryPerHOSECode,
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

        public void buildAndInsertHOSECodes(final List<DataSet> dataSetList, final int maxSphere,
                        final HOSECodeServiceImplementation hoseCodeServiceImplementation) {
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
                System.out.println(" --> building 3D HOSE codes without stereo configuration in "
                                + dataSetList.size()
                                + " cases...");
                final Map<String, Map<String, Double[]>> hoseCodeShiftStatistics = HOSECodeShiftStatistics
                                .buildHOSECodeShiftStatistics(
                                                dataSetList, maxSphere, true, false);
                System.out.println(" --> building 3D HOSE codes without stereo configuration done -> "
                                + hoseCodeShiftStatistics.size());
                System.out.println(" --> updating 3D HOSE codes in database...");

                insertOrUpdateHOSECodeRecord(hoseCodeShiftStatistics, hoseCodeServiceImplementation);
                System.out.println(" --> updating 3D HOSE codes in database done");
        }

        public void insertOrUpdateHOSECodeRecord(
                        final Map<String, Map<String, Double[]>> hoseCodeShiftStatisticsTemp,
                        final HOSECodeServiceImplementation hoseCodeServiceImplementation) {
                for (final Map.Entry<String, Map<String, Double[]>> entryPerHOSECode : hoseCodeShiftStatisticsTemp
                                .entrySet()) {
                        final String hoseCode = entryPerHOSECode.getKey();
                        if (!hoseCodeServiceImplementation.existsById(hoseCode).block()) {
                                final HOSECodeRecord hoseCodeRecord = new HOSECodeRecord(hoseCode, new HashMap<>(),
                                                new HashMap<>());
                                insertIntoHoseCodeRecord(entryPerHOSECode, hoseCodeRecord);
                                hoseCodeServiceImplementation.insert(hoseCodeRecord).block();
                        } else {
                                final HOSECodeRecord hoseCodeRecord = hoseCodeServiceImplementation.findById(hoseCode)
                                                .block();
                                insertIntoHoseCodeRecord(entryPerHOSECode, hoseCodeRecord);
                                hoseCodeServiceImplementation.save(hoseCodeRecord).block();
                        }
                }
        }
}
