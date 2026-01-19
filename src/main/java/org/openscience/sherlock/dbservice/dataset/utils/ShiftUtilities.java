package org.openscience.sherlock.dbservice.dataset.utils;

import casekit.nmr.model.DataSet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ShiftUtilities {

    public static boolean checkShifts(final DataSet dataSet, final int minShift, final int maxShift) {
        return Arrays.stream(dataSet.getSpectrum()
                                    .getSignals())
                     .allMatch(signalCompact -> signalCompact.getDoubles()[0]
                             >= minShift
                             && signalCompact.getDoubles()[0]
                             <= maxShift);
    }

    public static List<DataSet> filterByShift(final List<DataSet> dataSetList, final int minShift, final int maxShift) {
        return dataSetList.stream()
                          .filter(dataSet -> checkShifts(dataSet, minShift, maxShift))
                          .collect(Collectors.toList());
    }
}
