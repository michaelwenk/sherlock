package org.openscience.webcase.core.utils;

import casekit.nmr.model.DataSet;

import java.util.List;

public class Ranking {

    public static void rankDataSetList(final List<DataSet> dataSetList) {
        dataSetList.sort((dataSet1, dataSet2) -> {
            final int avgDevComparison = compareNumericDataSetMetaKey(dataSet1, dataSet2, "averageDeviation");
            if (avgDevComparison
                    != 0) {
                return avgDevComparison;
            }
            final boolean isCompleteSpectralMatchDataSet1 = Boolean.parseBoolean(dataSet1.getMeta()
                                                                                         .get("isCompleteSpectralMatch"));
            final boolean isCompleteSpectralMatchDataSet2 = Boolean.parseBoolean(dataSet2.getMeta()
                                                                                         .get("isCompleteSpectralMatch"));
            if (isCompleteSpectralMatchDataSet1
                    && !isCompleteSpectralMatchDataSet2) {
                return -1;
            } else if (!isCompleteSpectralMatchDataSet1
                    && isCompleteSpectralMatchDataSet2) {
                return 1;
            }
            final int setAssignmentsCountComparison = compareNumericDataSetMetaKey(dataSet1, dataSet2,
                                                                                   "setAssignmentsCount");
            if (setAssignmentsCountComparison
                    != 0) {
                return -1
                        * setAssignmentsCountComparison;
            }

            //            final int rmsdComparison = compareNumericDataSetMetaKey(dataSet1, dataSet2, "rmsd");
            //            if (rmsdComparison
            //                    != 0) {
            //                return rmsdComparison;
            //            }
            //
            //            final boolean isCompleteSpectralMatchWithEquivalencesDataSet1 = Boolean.parseBoolean(dataSet1.getMeta()
            //                                                                                                         .get("isCompleteSpectralMatchWithEquivalences"));
            //            final boolean isCompleteSpectralMatchWithEquivalencesDataSet2 = Boolean.parseBoolean(dataSet2.getMeta()
            //                                                                                                         .get("isCompleteSpectralMatchWithEquivalences"));
            //            if (isCompleteSpectralMatchWithEquivalencesDataSet1
            //                    && !isCompleteSpectralMatchWithEquivalencesDataSet2) {
            //                return -1;
            //            } else if (!isCompleteSpectralMatchWithEquivalencesDataSet1
            //                    && isCompleteSpectralMatchWithEquivalencesDataSet2) {
            //                return 1;
            //            }
            //
            //            final int setAssignmentsCountWithEquivalencesComparison = compareNumericDataSetMetaKey(dataSet1, dataSet2,
            //                                                                                                   "setAssignmentsCountWithEquivalences");
            //            if (setAssignmentsCountWithEquivalencesComparison
            //                    != 0) {
            //                return -1
            //                        * setAssignmentsCountWithEquivalencesComparison;
            //            }

            return 0;
        });
    }

    private static int compareNumericDataSetMetaKey(final DataSet dataSet1, final DataSet dataSet2,
                                                    final String metaKey) {
        Double valueDataSet1 = null;
        Double valueDataSet2 = null;
        try {
            valueDataSet1 = Double.parseDouble(dataSet1.getMeta()
                                                       .get(metaKey));
        } catch (final NullPointerException | NumberFormatException e) {
            //                e.printStackTrace();
        }
        try {
            valueDataSet2 = Double.parseDouble(dataSet2.getMeta()
                                                       .get(metaKey));
        } catch (final NullPointerException | NumberFormatException e) {
            //                e.printStackTrace();
        }

        if (valueDataSet1
                != null
                && valueDataSet2
                != null) {
            if (valueDataSet1
                    < valueDataSet2) {
                return -1;
            } else if (valueDataSet1
                    > valueDataSet2) {
                return 1;
            }
            return 0;
        }
        if (valueDataSet1
                != null) {
            return -1;
        } else if (valueDataSet2
                != null) {
            return 1;
        }

        return 0;
    }
}
