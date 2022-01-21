package org.openscience.sherlock.dbservice.result.model;

import lombok.Data;

@Data
public class ElucidationOptions
        extends casekit.nmr.lsd.model.ElucidationOptions {
    // PyLSD input file creation
    private boolean useFilterLsdRing3;
    private boolean useFilterLsdRing4;
    // elucidation process
    private int timeLimitTotal;
    // generated structures filter
    private double shiftTolerance;
    private double maxAverageDeviation;
    private boolean useCombinatorics;
}
