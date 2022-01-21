package org.openscience.sherlock.pylsd.model;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class ElucidationOptions
        extends casekit.nmr.lsd.model.ElucidationOptions {
    // PyLSD options
    private boolean useFilterLsdRing3;
    private boolean useFilterLsdRing4;
    // elucidation process
    private int timeLimitTotal;
    // generated structures filter
    private double shiftTolerance;
    private double maxAverageDeviation;
    private boolean useCombinatorics;
}
